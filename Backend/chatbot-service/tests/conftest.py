import pytest
from unittest.mock import patch, MagicMock
import uuid
import sys


class MockRedis:
    def __init__(self, **kwargs):
        pass
    
    def ping(self):
        return True
    
    def set(self, key, value):
        return True
    
    def get(self, key):
        return "{}"
    
    def delete(self, key):
        return True
    
    def keys(self, pattern):
        return []
    
    def expire(self, key, seconds):
        return True


sys.modules['redis'] = MagicMock()
sys.modules['redis'].Redis = MockRedis


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
        mock_redis.get_chat_history.return_value = []
        mock_redis.delete_chat_history.return_value = True
        mock_redis.get_all_sessions.return_value = []
        mock_redis_class.return_value = mock_redis
        yield mock_redis

@pytest.fixture(autouse=True)
def clear_conversation_history():

    conversation_history.conversations = {}
    yield

    conversation_history.conversations = {}

@pytest.fixture
def test_session_id():
    return str(uuid.uuid4()) 