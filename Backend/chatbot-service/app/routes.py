from fastapi import APIRouter, HTTPException, Depends, Query
from pydantic import BaseModel
from typing import Optional, List
import uuid
from app.gemini_api import get_gemini_response
from app.chat_history import ConversationHistory
from datetime import datetime

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
        history = conversation_history.get_conversation(session_id)
        conversation_history.add_message(session_id, "user", chat_message.message)
        response = get_gemini_response(chat_message.message, history)
        conversation_history.add_message(session_id, "assistant", response)
        conversation_history.cleanup_expired()
        
        return ChatResponse(response=response, session_id=session_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Sohbet geçmişini getirme endpoint'i
@router.get("/chat/{session_id}/history", response_model=List[MessageHistory])
async def get_chat_history(session_id: str):
    try:
        history = conversation_history.get_conversation(session_id)
        if not history:
            raise HTTPException(status_code=404, detail="Sohbet geçmişi bulunamadı")
        
        return [
            MessageHistory(
                role=msg["role"],
                content=msg["content"],
                timestamp=msg["timestamp"].isoformat()
            ) for msg in history
        ]
    except Exception as e:
        if isinstance(e, HTTPException):
            raise e
        raise HTTPException(status_code=500, detail=str(e))

# Sohbet geçmişini silme endpoint'i
@router.delete("/chat/{session_id}")
async def clear_chat_history(session_id: str):
    try:
        if session_id not in conversation_history.conversations:
            raise HTTPException(status_code=404, detail="Sohbet geçmişi bulunamadı")
            
        conversation_history.clear_conversation(session_id)
        return {"message": "Sohbet geçmişi başarıyla silindi"}
    except Exception as e:
        if isinstance(e, HTTPException):
            raise e
        raise HTTPException(status_code=500, detail=str(e))

# Aktif oturumları listeleme endpoint'i
@router.get("/sessions")
async def list_active_sessions():
    try:
        active_sessions = [
            {
                "session_id": session_id,
                "message_count": len(messages),
                "last_activity": messages[-1]["timestamp"].isoformat() if messages else None
            }
            for session_id, messages in conversation_history.conversations.items()
        ]
        return {"active_sessions": active_sessions}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Sağlık kontrolü endpoint'i
@router.get("/health")
async def health_check():
    return {"status": "healthy", "service": "chatbot"}
