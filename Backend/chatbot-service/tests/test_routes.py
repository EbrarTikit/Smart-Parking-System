import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pytest
from fastapi.testclient import TestClient
from datetime import datetime, timedelta
from app.main import app
import uuid
from app.routes import conversation_history
from unittest.mock import patch

client = TestClient(app)

@pytest.fixture
def test_session_id():
    return str(uuid.uuid4())

@pytest.fixture
def sample_conversation(test_session_id):

    conversation_history.add_message(test_session_id, "user", "Merhaba")
    conversation_history.add_message(test_session_id, "assistant", "Size nasıl yardımcı olabilirim?")
    return conversation_history

def test_health_check():
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    response_data = response.json()
    assert "status" in response_data
    assert response_data["status"] == "ok"
    assert "timestamp" in response_data

def test_chat_empty_message():
    response = client.post("/api/v1/chat", json={"message": ""})
    assert response.status_code == 400
    assert response.json()["detail"] == "Boş mesaj gönderilemez."

def test_chat_new_session():
    response = client.post("/api/v1/chat", json={"message": "Merhaba"})
    assert response.status_code == 200
    assert "response" in response.json()
    assert "session_id" in response.json()

def test_chat_existing_session(test_session_id):
    

    response1 = client.post(
        "/api/v1/chat", 
        json={"message": "Merhaba", "session_id": test_session_id}
    )
    assert response1.status_code == 200


    response2 = client.post(
        "/api/v1/chat", 
        json={"message": "Otopark fiyatları nedir?", "session_id": test_session_id}
    )
    assert response2.status_code == 200
    assert response2.json()["session_id"] == test_session_id

def test_get_chat_history(test_session_id, sample_conversation):
    response = client.get(f"/api/v1/chat/{test_session_id}/history")
    assert response.status_code == 200
    history = response.json()
    assert len(history) == 2
    assert history[0]["role"] == "user"
    assert history[0]["content"] == "Merhaba"

def test_get_nonexistent_chat_history():
    fake_session_id = str(uuid.uuid4())
    response = client.get(f"/api/v1/chat/{fake_session_id}/history")
    assert response.status_code == 404
    assert response.json()["detail"] == "Sohbet geçmişi bulunamadı"

def test_clear_chat_history(test_session_id, sample_conversation):

    history_response = client.get(f"/api/v1/chat/{test_session_id}/history")
    assert history_response.status_code == 200
    
    response = client.delete(f"/api/v1/chat/{test_session_id}")
    assert response.status_code == 200
    
    history_response = client.get(f"/api/v1/chat/{test_session_id}/history")
    assert history_response.status_code == 404

@patch('app.routes.conversation_history.get_all_sessions')
def test_list_active_sessions(mock_get_sessions, test_session_id, sample_conversation):
    # Mock session listesini ayarla
    mock_get_sessions.return_value = [test_session_id]
    
    response = client.get("/api/v1/sessions")
    assert response.status_code == 200
    sessions = response.json()["sessions"]
    assert "count" in response.json()
    assert len(sessions) > 0
    assert test_session_id in sessions

@patch('app.chat_history.RedisClient')
def test_cleanup_expired_sessions(mock_redis_client, test_session_id):
    # Redis TTL kullanıldığı için bu test artık farklı çalışır
    # Sadece metodun varlığını test ederiz - gerçek temizleme Redis tarafında yapılır
    
    # Metot var mı kontrol edelim
    assert hasattr(conversation_history, 'cleanup_expired')
    
    # Metod çağrılabilir olmalı
    conversation_history.cleanup_expired()
