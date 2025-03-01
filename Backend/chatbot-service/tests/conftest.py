import pytest
from unittest.mock import patch
import uuid
from app.routes import conversation_history

@pytest.fixture(autouse=True)
def mock_gemini_api():
    with patch('app.gemini_api.get_gemini_response') as mock:
        mock.return_value = "Bu bir test yanıtıdır."
        yield mock

@pytest.fixture(autouse=True)
def clear_conversation_history():
    # Her testten önce conversation history'yi temizle
    conversation_history.conversations.clear()
    yield
    # Her testten sonra tekrar temizle
    conversation_history.conversations.clear()

@pytest.fixture
def test_session_id():
    return str(uuid.uuid4()) 