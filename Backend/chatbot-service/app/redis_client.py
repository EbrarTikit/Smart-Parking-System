import redis
import os
import json
from typing import List, Dict, Optional
from dotenv import load_dotenv
import logging

logger = logging.getLogger(__name__)

load_dotenv()

class RedisClient:
    def __init__(self):
        self.redis_host = os.getenv("REDIS_HOST", "localhost")
        self.redis_port = int(os.getenv("REDIS_PORT", 6379))
        self.redis_db = int(os.getenv("REDIS_DB", 0))
        self.redis_password = os.getenv("REDIS_PASSWORD", None)
        self.expiry_seconds = int(os.getenv("REDIS_EXPIRY", 1800))  # 30 dakika varsayılan
        
        self.redis = self._connect_to_redis()
        
    def _connect_to_redis(self) -> redis.Redis:
        try:
            client = redis.Redis(
                host=self.redis_host,
                port=self.redis_port,
                db=self.redis_db,
                password=self.redis_password,
                decode_responses=True  # Yanıtları string olarak almak için
            )
            # Bağlantıyı test et
            client.ping()
            logger.info("Redis bağlantısı başarılı")
            return client
        except redis.ConnectionError as e:
            logger.error(f"Redis bağlantı hatası: {str(e)}")
            raise

    def save_chat_history(self, session_id: str, messages: List[Dict]) -> bool:
        """Kullanıcı sohbet geçmişini Redis'e kaydeder"""
        try:
            # Mesajları JSON formatına dönüştür
            messages_json = json.dumps(messages)
            # Redis'e kaydet
            self.redis.set(f"chat:{session_id}", messages_json)
            # Süre sınırı belirle
            self.redis.expire(f"chat:{session_id}", self.expiry_seconds)
            return True
        except Exception as e:
            logger.error(f"Sohbet geçmişi kaydedilirken hata: {str(e)}")
            return False

    def get_chat_history(self, session_id: str) -> List[Dict]:
        """Kullanıcı sohbet geçmişini Redis'ten alır"""
        try:
            # Redis'ten veriyi al
            chat_history = self.redis.get(f"chat:{session_id}")
            if chat_history:
                # TTL'i yenile
                self.redis.expire(f"chat:{session_id}", self.expiry_seconds)
                # JSON'dan Python nesnesine dönüştür
                return json.loads(chat_history)
            return []
        except Exception as e:
            logger.error(f"Sohbet geçmişi alınırken hata: {str(e)}")
            return []

    def delete_chat_history(self, session_id: str) -> bool:
        """Kullanıcı sohbet geçmişini Redis'ten siler"""
        try:
            self.redis.delete(f"chat:{session_id}")
            return True
        except Exception as e:
            logger.error(f"Sohbet geçmişi silinirken hata: {str(e)}")
            return False
            
    def get_all_sessions(self) -> List[str]:
        """Tüm aktif sohbet oturumlarını listeler"""
        try:
            # chat: ile başlayan tüm anahtarları bul
            keys = self.redis.keys("chat:*")
            # Ön eki kaldır
            return [key.replace("chat:", "") for key in keys]
        except Exception as e:
            logger.error(f"Oturumlar listelenirken hata: {str(e)}")
            return [] 