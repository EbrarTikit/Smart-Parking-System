from typing import List, Dict
from datetime import datetime, timedelta
import json
import logging
from app.redis_client import RedisClient

logger = logging.getLogger(__name__)

class ConversationHistory:
    def __init__(self, max_history: int = 5):
        self.max_history = max_history
        self.redis_client = RedisClient()

    def add_message(self, session_id: str, role: str, content: str) -> None:
        """Yeni bir mesajı sohbet geçmişine ekler ve Redis'e kaydeder"""
        try:
            # Mevcut geçmişi al
            conversation = self.get_conversation(session_id)
            
            # Yeni mesajı ekle
            conversation.append({
                'role': role,
                'content': content,
                'timestamp': datetime.now().isoformat()  # ISO formatında tarih
            })
            
            # Geçmiş çok uzunsa kısalt
            if len(conversation) > self.max_history * 2:
                conversation = conversation[-self.max_history * 2:]
            
            # Redis'e kaydet
            self.redis_client.save_chat_history(session_id, conversation)
            
        except Exception as e:
            logger.error(f"Mesaj eklenirken hata: {str(e)}")

    def get_conversation(self, session_id: str) -> List[Dict]:
        """Redis'ten sohbet geçmişini alır"""
        try:
            # Redis'ten geçmişi al
            conversation = self.redis_client.get_chat_history(session_id)
            
            # Tarih formatını düzelt (eğer gerekirse)
            for message in conversation:
                if isinstance(message.get('timestamp'), str):
                    try:
                        # ISO formatındaki tarihi datetime nesnesine çevirme işlemi burada yapılabilir
                        # Şimdilik string olarak bırakıyoruz
                        pass
                    except:
                        pass
                        
            return conversation
        except Exception as e:
            logger.error(f"Sohbet geçmişi alınırken hata: {str(e)}")
            return []

    def clear_conversation(self, session_id: str) -> None:
        """Redis'ten sohbet geçmişini siler"""
        try:
            self.redis_client.delete_chat_history(session_id)
        except Exception as e:
            logger.error(f"Sohbet geçmişi silinirken hata: {str(e)}")

    def get_all_sessions(self) -> List[str]:
        """Tüm aktif sohbet oturumlarını listeler"""
        try:
            return self.redis_client.get_all_sessions()
        except Exception as e:
            logger.error(f"Oturumlar listelenirken hata: {str(e)}")
            return []
            
    def cleanup_expired(self) -> None:
        """
        Redis TTL mekanizması otomatik olarak süresi dolan kayıtları temizlediği için
        bu metot artık gerekli değil, ancak geriye dönük uyumluluk için boş olarak bırakıyoruz
        """
        pass 