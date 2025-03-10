from fastapi import APIRouter, HTTPException, Depends, Query
from pydantic import BaseModel
from typing import Optional, List
import uuid
from app.gemini_api import get_gemini_response
from app.chat_history import ConversationHistory
from datetime import datetime
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1")
conversation_history = ConversationHistory()

class ChatMessage(BaseModel):
    message: str
    session_id: Optional[str] = None

class ChatResponse(BaseModel):
    response: str
    session_id: str

class MessageHistory(BaseModel):
    role: str
    content: str
    timestamp: str

# Yeni mesaj gönderme endpoint'i
@router.post("/chat", response_model=ChatResponse)
async def chat_with_user(chat_message: ChatMessage):
    if not chat_message.message.strip():
        raise HTTPException(status_code=400, detail="Boş mesaj gönderilemez.")

    session_id = chat_message.session_id or str(uuid.uuid4())
    
    try:
        # Önce mevcut sohbet geçmişini al
        history = conversation_history.get_conversation(session_id)
        
        # Kullanıcı mesajını ekle ve kaydet
        conversation_history.add_message(session_id, "user", chat_message.message)
        
        # Güncellenmiş geçmişi al
        updated_history = conversation_history.get_conversation(session_id)
        
        # Gemini API'ye gönder
        response = get_gemini_response(chat_message.message, updated_history)
        
        # Asistan yanıtını ekle
        conversation_history.add_message(session_id, "assistant", response)
        
        return ChatResponse(response=response, session_id=session_id)
    except Exception as e:
        logger.error(f"Sohbet işlemi sırasında hata: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

# Sohbet geçmişini getirme endpoint'i
@router.get("/chat/{session_id}/history", response_model=List[MessageHistory])
async def get_chat_history(session_id: str):
    try:
        history = conversation_history.get_conversation(session_id)
        if not history:
            raise HTTPException(status_code=404, detail="Sohbet geçmişi bulunamadı")
        
        # MessageHistory modeline dönüştür
        formatted_history = []
        for msg in history:
            formatted_history.append(
                MessageHistory(
                    role=msg["role"],
                    content=msg["content"],
                    timestamp=msg["timestamp"]
                )
            )
        
        return formatted_history
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Sohbet geçmişi alınırken hata: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

# Sohbet geçmişini silme endpoint'i
@router.delete("/chat/{session_id}")
async def clear_chat_history(session_id: str):
    try:
        # Önce geçmişin var olup olmadığını kontrol et
        history = conversation_history.get_conversation(session_id)
        if not history:
            raise HTTPException(status_code=404, detail="Sohbet geçmişi bulunamadı")
        
        # Geçmişi sil
        conversation_history.clear_conversation(session_id)
        return {"message": f"Sohbet geçmişi silindi: {session_id}"}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Sohbet geçmişi silinirken hata: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

# Aktif oturumları listeleme endpoint'i
@router.get("/sessions")
async def list_active_sessions():
    try:
        sessions = conversation_history.get_all_sessions()
        return {"sessions": sessions, "count": len(sessions)}
    except Exception as e:
        logger.error(f"Oturumlar listelenirken hata: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

# Sağlık kontrolü endpoint'i
@router.get("/health")
async def health_check():
    return {"status": "ok", "timestamp": datetime.now().isoformat()}
