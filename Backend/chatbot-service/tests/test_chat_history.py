import sys
import os
import pytest
from unittest.mock import patch, MagicMock
from datetime import datetime, timedelta

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.chat_history import ConversationHistory

class TestConversationHistory:
    
    def test_init_with_default_values(self):
        """Varsayılan değerlerle ConversationHistory başlatma testi"""
        # Act
        history = ConversationHistory()
        
        # Assert
        assert history.max_history == 5
    
    def test_init_with_custom_max_history(self):
        """Özel max_history değeriyle ConversationHistory başlatma testi"""
        # Act
        history = ConversationHistory(max_history=10)
        
        # Assert
        assert history.max_history == 10
    
    @patch('app.chat_history.RedisClient')
    def test_add_message(self, mock_redis_client):
        """Mesaj ekleme testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_redis_client.return_value = mock_instance
        mock_instance.get_chat_history.return_value = []
        mock_instance.save_chat_history.return_value = True
        
        history = ConversationHistory()
        session_id = "test-session"
        role = "user"
        content = "Test message"
        
        # Act
        history.add_message(session_id, role, content)
        
        # Assert
        # RedisClient metodlarının çağrıldığını doğrula
        mock_instance.get_chat_history.assert_called_once_with(session_id)
        mock_instance.save_chat_history.assert_called_once()
        
        # save_chat_history'ye gönderilen parametreleri kontrol et
        args, _ = mock_instance.save_chat_history.call_args
        saved_session_id, saved_messages = args
        
        assert saved_session_id == session_id
        assert len(saved_messages) == 1
        assert saved_messages[0]["role"] == role
        assert saved_messages[0]["content"] == content
        assert "timestamp" in saved_messages[0]
    
    @patch('app.chat_history.RedisClient')
    def test_add_message_to_existing_conversation(self, mock_redis_client):
        """Var olan sohbete mesaj ekleme testi"""
        # Arrange
        existing_messages = [
            {"role": "user", "content": "Hello", "timestamp": "2023-01-01T12:00:00"}
        ]
        
        mock_instance = MagicMock()
        mock_redis_client.return_value = mock_instance
        mock_instance.get_chat_history.return_value = existing_messages
        mock_instance.save_chat_history.return_value = True
        
        history = ConversationHistory()
        session_id = "test-session"
        
        # Act
        history.add_message(session_id, "assistant", "Hi there!")
        
        # Assert
        args, _ = mock_instance.save_chat_history.call_args
        saved_session_id, saved_messages = args
        
        assert saved_session_id == session_id
        assert len(saved_messages) == 2
        assert saved_messages[0]["role"] == "user"
        assert saved_messages[0]["content"] == "Hello"
        assert saved_messages[1]["role"] == "assistant"
        assert saved_messages[1]["content"] == "Hi there!"
    
    @patch('app.chat_history.RedisClient')
    def test_add_message_with_truncation(self, mock_redis_client):
        """Uzun sohbet geçmişine mesaj ekleme testi (kırpma)"""
        # Arrange
        # Max history = 5 olduğunda, 10 mesajdan sonra kırpılmalı (5*2 = 10)
        existing_messages = []
        for i in range(10):
            existing_messages.append({
                "role": "user" if i % 2 == 0 else "assistant",
                "content": f"Message {i}",
                "timestamp": "2023-01-01T12:00:00"
            })
        
        mock_instance = MagicMock()
        mock_redis_client.return_value = mock_instance
        mock_instance.get_chat_history.return_value = existing_messages
        mock_instance.save_chat_history.return_value = True
        
        history = ConversationHistory(max_history=5)
        session_id = "test-session"
        
        # Act
        history.add_message(session_id, "user", "New message")
        
        # Assert
        args, _ = mock_instance.save_chat_history.call_args
        saved_session_id, saved_messages = args
        
        assert saved_session_id == session_id
        assert len(saved_messages) == 10  # Max 10 mesaj (5 user, 5 assistant)
        assert saved_messages[0]["content"] == "Message 1"  # İlk mesaj kesildi
        assert saved_messages[-1]["content"] == "New message"  # Yeni mesaj eklendi
    
    @patch('app.chat_history.RedisClient')
    def test_add_message_exception(self, mock_redis_client):
        """Mesaj ekleme hatası testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_redis_client.return_value = mock_instance
        mock_instance.get_chat_history.side_effect = Exception("Test error")
        
        history = ConversationHistory()
        session_id = "test-session"
        
        # Act - Hata durumunda sessizce devam etmeli
        history.add_message(session_id, "user", "Test message")
        
        # Assert - Exception log'lanmalı ancak program çökmemeli
        mock_instance.get_chat_history.assert_called_once_with(session_id)
        # Uygulama kodu boş liste kullanarak devam edecek, bu yüzden save_chat_history çağrılabilir
        # Bu durumda save_chat_history, boş bir liste ve yeni eklenen mesajla çağrılır
    
    @patch('app.chat_history.RedisClient')
    def test_get_conversation(self, mock_redis_client):
        """Sohbet geçmişi alma testi"""
        # Arrange
        mock_messages = [
            {"role": "user", "content": "Hello", "timestamp": "2023-01-01T12:00:00"}
        ]
        
        mock_instance = MagicMock()
        mock_redis_client.return_value = mock_instance
        mock_instance.get_chat_history.return_value = mock_messages
        
        history = ConversationHistory()
        session_id = "test-session"
        
        # Act
        result = history.get_conversation(session_id)
        
        # Assert
        assert result == mock_messages
        mock_instance.get_chat_history.assert_called_once_with(session_id)
    
    @patch('app.chat_history.RedisClient')
    def test_get_conversation_exception(self, mock_redis_client):
        """Sohbet geçmişi alma hatası testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_redis_client.return_value = mock_instance
        mock_instance.get_chat_history.side_effect = Exception("Test error")
        
        history = ConversationHistory()
        session_id = "test-session"
        
        # Act
        result = history.get_conversation(session_id)
        
        # Assert - Hata durumunda boş liste döndürmeli
        assert result == []
        mock_instance.get_chat_history.assert_called_once_with(session_id)
    
    @patch('app.chat_history.RedisClient')
    def test_clear_conversation(self, mock_redis_client):
        """Sohbet geçmişi silme testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_redis_client.return_value = mock_instance
        mock_instance.delete_chat_history.return_value = True
        
        history = ConversationHistory()
        session_id = "test-session"
        
        # Act
        history.clear_conversation(session_id)
        
        # Assert
        mock_instance.delete_chat_history.assert_called_once_with(session_id)
    
    @patch('app.chat_history.RedisClient')
    def test_clear_conversation_exception(self, mock_redis_client):
        """Sohbet geçmişi silme hatası testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_redis_client.return_value = mock_instance
        mock_instance.delete_chat_history.side_effect = Exception("Test error")
        
        history = ConversationHistory()
        session_id = "test-session"
        
        # Act - Hata durumunda sessizce devam etmeli
        history.clear_conversation(session_id)
        
        # Assert - Exception log'lanmalı ancak program çökmemeli
        mock_instance.delete_chat_history.assert_called_once_with(session_id)
    
    @patch('app.chat_history.RedisClient')
    def test_get_all_sessions(self, mock_redis_client):
        """Tüm oturumları listeleme testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_redis_client.return_value = mock_instance
        mock_instance.get_all_sessions.return_value = ["session1", "session2"]
        
        history = ConversationHistory()
        
        # Act
        result = history.get_all_sessions()
        
        # Assert
        assert result == ["session1", "session2"]
        mock_instance.get_all_sessions.assert_called_once()
    
    @patch('app.chat_history.RedisClient')
    def test_get_all_sessions_exception(self, mock_redis_client):
        """Tüm oturumları listeleme hatası testi"""
        # Arrange
        mock_instance = MagicMock()
        mock_redis_client.return_value = mock_instance
        mock_instance.get_all_sessions.side_effect = Exception("Test error")
        
        history = ConversationHistory()
        
        # Act
        result = history.get_all_sessions()
        
        # Assert - Hata durumunda boş liste döndürmeli
        assert result == []
        mock_instance.get_all_sessions.assert_called_once()
    
    @patch('app.chat_history.RedisClient')
    def test_cleanup_expired(self, mock_redis_client):
        """Süresi dolan oturumları temizleme testi (artık kullanılmıyor)"""
        # Arrange
        mock_instance = MagicMock()
        mock_redis_client.return_value = mock_instance
        
        history = ConversationHistory()
        
        # Act - Bu metot artık boş, Redis TTL mekanizması otomatik temizliyor
        history.cleanup_expired()
        
        # Assert - Herhangi bir çağrı yapılmamalı
        mock_instance.assert_not_called() 