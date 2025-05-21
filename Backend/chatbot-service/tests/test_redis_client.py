import sys
import os
import pytest
from unittest.mock import patch, MagicMock, call

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.redis_client import RedisClient

class TestRedisClient:
    
    @patch('app.redis_client.redis.Redis')
    def test_init_with_default_values(self, mock_redis):
        """Varsayılan değerlerle RedisClient başlatma testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_redis.return_value = mock_instance
        
        # Act
        client = RedisClient()
        
        # Assert
        assert client.redis_host == "redis_service"
        assert client.redis_port == 6379
        assert client.redis_db == 0
        assert client.redis_password == ""
        assert client.expiry_seconds == 1800
        mock_redis.assert_called_once()
        mock_instance.ping.assert_called_once()
    
    @patch('app.redis_client.redis.Redis')
    @patch('app.redis_client.os.getenv')
    def test_init_with_env_values(self, mock_getenv, mock_redis):
        """Çevresel değişkenlerle RedisClient başlatma testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_redis.return_value = mock_instance
        
        # Çevresel değişken değerlerini ayarla
        mock_getenv.side_effect = lambda key, default: {
            "REDIS_HOST": "test-host",
            "REDIS_PORT": "1234",
            "REDIS_DB": "2",
            "REDIS_PASSWORD": "test-password",
            "REDIS_EXPIRY": "3600"
        }.get(key, default)
        
        # Act
        client = RedisClient()
        
        # Assert
        assert client.redis_host == "test-host"
        assert client.redis_port == 1234
        assert client.redis_db == 2
        assert client.redis_password == "test-password"
        assert client.expiry_seconds == 3600
        mock_redis.assert_called_once_with(
            host="test-host", 
            port=1234, 
            db=2, 
            password="test-password", 
            decode_responses=True
        )
    
    @patch('app.redis_client.redis.Redis')
    def test_connect_to_redis_connection_error(self, mock_redis):
        """Redis bağlantı hatası testi"""
        # Arrange
        import redis
        mock_redis.side_effect = redis.ConnectionError("Connection refused")
        
        # Act & Assert
        with pytest.raises(redis.ConnectionError):
            client = RedisClient()
    
    @patch('app.redis_client.redis.Redis')
    def test_save_chat_history_success(self, mock_redis):
        """Sohbet geçmişi kaydetme başarılı durumu testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_redis.return_value = mock_instance
        client = RedisClient()
        
        session_id = "test-session"
        messages = [{"role": "user", "content": "Test mesaj"}]
        
        # Act
        result = client.save_chat_history(session_id, messages)
        
        # Assert
        assert result is True
        mock_instance.set.assert_called_once()
        mock_instance.expire.assert_called_once_with(f"chat:{session_id}", client.expiry_seconds)
    
    @patch('app.redis_client.redis.Redis')
    def test_save_chat_history_exception(self, mock_redis):
        """Sohbet geçmişi kaydetme hata durumu testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_instance.set.side_effect = Exception("Test error")
        mock_redis.return_value = mock_instance
        client = RedisClient()
        
        # Act
        result = client.save_chat_history("test-session", [])
        
        # Assert
        assert result is False
    
    @patch('app.redis_client.redis.Redis')
    def test_get_chat_history_existing(self, mock_redis):
        """Var olan sohbet geçmişini alma testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_instance.get.return_value = '[{"role":"user","content":"Test"}]'
        mock_redis.return_value = mock_instance
        client = RedisClient()
        
        # Act
        result = client.get_chat_history("test-session")
        
        # Assert
        assert result == [{"role": "user", "content": "Test"}]
        mock_instance.get.assert_called_once_with("chat:test-session")
        mock_instance.expire.assert_called_once()
    
    @patch('app.redis_client.redis.Redis')
    def test_get_chat_history_not_existing(self, mock_redis):
        """Var olmayan sohbet geçmişini alma testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_instance.get.return_value = None
        mock_redis.return_value = mock_instance
        client = RedisClient()
        
        # Act
        result = client.get_chat_history("test-session")
        
        # Assert
        assert result == []
        mock_instance.get.assert_called_once()
        mock_instance.expire.assert_not_called()
    
    @patch('app.redis_client.redis.Redis')
    def test_get_chat_history_exception(self, mock_redis):
        """Sohbet geçmişi alma hata durumu testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_instance.get.side_effect = Exception("Test error")
        mock_redis.return_value = mock_instance
        client = RedisClient()
        
        # Act
        result = client.get_chat_history("test-session")
        
        # Assert
        assert result == []
    
    @patch('app.redis_client.redis.Redis')
    def test_delete_chat_history_success(self, mock_redis):
        """Sohbet geçmişi silme başarılı durumu testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_redis.return_value = mock_instance
        client = RedisClient()
        
        # Act
        result = client.delete_chat_history("test-session")
        
        # Assert
        assert result is True
        mock_instance.delete.assert_called_once_with("chat:test-session")
    
    @patch('app.redis_client.redis.Redis')
    def test_delete_chat_history_exception(self, mock_redis):
        """Sohbet geçmişi silme hata durumu testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_instance.delete.side_effect = Exception("Test error")
        mock_redis.return_value = mock_instance
        client = RedisClient()
        
        # Act
        result = client.delete_chat_history("test-session")
        
        # Assert
        assert result is False
    
    @patch('app.redis_client.redis.Redis')
    def test_get_all_sessions(self, mock_redis):
        """Tüm oturumları listeleme testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_instance.keys.return_value = ["chat:session1", "chat:session2"]
        mock_redis.return_value = mock_instance
        client = RedisClient()
        
        # Act
        result = client.get_all_sessions()
        
        # Assert
        assert result == ["session1", "session2"]
        mock_instance.keys.assert_called_once_with("chat:*")
    
    @patch('app.redis_client.redis.Redis')
    def test_get_all_sessions_empty(self, mock_redis):
        """Hiç oturum olmadığında listeleme testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_instance.keys.return_value = []
        mock_redis.return_value = mock_instance
        client = RedisClient()
        
        # Act
        result = client.get_all_sessions()
        
        # Assert
        assert result == []
    
    @patch('app.redis_client.redis.Redis')
    def test_get_all_sessions_exception(self, mock_redis):
        """Tüm oturumları listeleme hata durumu testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_instance.keys.side_effect = Exception("Test error")
        mock_redis.return_value = mock_instance
        client = RedisClient()
        
        # Act
        result = client.get_all_sessions()
        
        # Assert
        assert result == [] 