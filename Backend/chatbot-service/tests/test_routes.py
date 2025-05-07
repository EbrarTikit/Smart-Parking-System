import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pytest
from fastapi.testclient import TestClient
from datetime import datetime, timedelta
from app.main import app
import uuid
from app.routes import conversation_history

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
    assert response.json() == {"status": "healthy", "service": "chatbot"}

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

def test_list_active_sessions(test_session_id, sample_conversation):
    response = client.get("/api/v1/sessions")
    assert response.status_code == 200
    sessions = response.json()["active_sessions"]
    assert len(sessions) > 0
    
    session = next(
        (s for s in sessions if s["session_id"] == test_session_id), 
        None
    )
    assert session is not None
    assert session["message_count"] == 2

def test_cleanup_expired_sessions(test_session_id):

    conversation_history.add_message(test_session_id, "user", "Test mesajı")
    

    conversation_history.conversations[test_session_id][0]["timestamp"] = \
        datetime.now() - timedelta(minutes=31)
    
    conversation_history.cleanup_expired()
    
    assert test_session_id not in conversation_history.conversations
