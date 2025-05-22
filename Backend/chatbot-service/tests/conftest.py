import pytest
from unittest.mock import patch, MagicMock
import uuid
import sys
import os
import json
from datetime import datetime

# Projenin ana dizinini Python yoluna ekle
# Eğer tests/ klasöründen çalıştırılıyorsa bir üst dizine çıkmamız gerekir
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

class MockRedis:
    def __init__(self, **kwargs):
        self.data = {}
        self.expiry = {}
    
    def ping(self):
        return True
    
    def set(self, key, value):
        self.data[key] = value
        return True
    
    def get(self, key):
        return self.data.get(key)
    
    def delete(self, key):
        if key in self.data:
            del self.data[key]
        return True
    
    def keys(self, pattern):
        if pattern.endswith('*'):
            prefix = pattern[:-1]
            return [key for key in self.data.keys() if key.startswith(prefix)]
        return []
    
    def expire(self, key, seconds):
        self.expiry[key] = seconds
        return True


sys.modules['redis'] = MagicMock()
sys.modules['redis'].Redis = MockRedis
sys.modules['redis'].ConnectionError = Exception

# routes modülünü içe aktar
from app.routes import conversation_history

@pytest.fixture(autouse=True)
def mock_gemini_api():
    with patch('app.gemini_api.get_gemini_response') as mock:
        mock.return_value = "Bu bir test yanıtıdır."
        yield mock

@pytest.fixture(autouse=True)
def mock_redis_client():
    with patch('app.redis_client.RedisClient') as mock_redis_class:
        mock_redis = MagicMock()
        
        mock_redis.save_chat_history.return_value = True
        
        chat_history = {}
        
        def mock_get_chat_history(session_id):
            return chat_history.get(session_id, [])
        
        def mock_save_chat_history(session_id, messages):
            chat_history[session_id] = messages
            return True
        
        def mock_delete_chat_history(session_id):
            if session_id in chat_history:
                del chat_history[session_id]
            return True
            
        def mock_get_all_sessions():
            return list(chat_history.keys())
        
        mock_redis.get_chat_history.side_effect = mock_get_chat_history
        mock_redis.save_chat_history.side_effect = mock_save_chat_history
        mock_redis.delete_chat_history.side_effect = mock_delete_chat_history
        mock_redis.get_all_sessions.side_effect = mock_get_all_sessions
        
        mock_redis_class.return_value = mock_redis
        yield mock_redis

@pytest.fixture(autouse=True)
def clear_conversation_history():
    conversation_history.redis_client.get_all_sessions = MagicMock(return_value=[])
    
    yield
    
    conversation_history.redis_client.get_all_sessions = MagicMock(return_value=[])

@pytest.fixture
def test_session_id():
    return str(uuid.uuid4())

@pytest.fixture
def mock_conversation_data():
    return [
        {
            "role": "user",
            "content": "Merhaba",
            "timestamp": datetime.now().isoformat()
        },
        {
            "role": "assistant", 
            "content": "Size nasıl yardımcı olabilirim?",
            "timestamp": datetime.now().isoformat()
        }
    ]

@pytest.fixture
def setup_test_conversation(test_session_id, mock_conversation_data, mock_redis_client):
    mock_redis_client.get_chat_history.return_value = mock_conversation_data
    
    return {
        "session_id": test_session_id,
        "conversation": mock_conversation_data
    } 