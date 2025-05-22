"""
Pytest yapılandırması için conftest.py dosyası.
"""
import pytest
from unittest.mock import MagicMock, patch
from fastapi.testclient import TestClient

from app.main import app

@pytest.fixture
def test_client():
    """Test client fixture."""
    return TestClient(app)

@pytest.fixture
def mock_elasticsearch():
    """Mock Elasticsearch client fixture."""
    with patch('app.elasticsearch_client.Elasticsearch') as mock:
        mock_es = MagicMock()
        mock.return_value = mock_es
        yield mock_es

@pytest.fixture
def mock_prometheus():
    """Mock Prometheus requests fixture."""
    with patch('app.prometheus_client.requests.get') as mock:
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {"status": "success", "data": {"result": []}}
        mock.return_value = mock_response
        yield mock 