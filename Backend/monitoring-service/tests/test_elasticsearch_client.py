"""
Elasticsearch client için unit testler.
"""
import unittest
from unittest.mock import patch, MagicMock
from datetime import datetime

from app.elasticsearch_client import (
    get_elasticsearch_client,
    check_elasticsearch_connection,
    get_logs,
    get_error_logs,
    get_service_error_count,
    get_service_last_log
)

class TestElasticsearchClient(unittest.TestCase):
    """Elasticsearch client için test sınıfı."""
    
    @patch('app.elasticsearch_client.Elasticsearch')
    def test_get_elasticsearch_client(self, mock_elasticsearch):
        """get_elasticsearch_client fonksiyonunun doğru çalıştığını test eder."""
        # Mock Elasticsearch nesnesi
        mock_es = MagicMock()
        mock_elasticsearch.return_value = mock_es
        
        # Fonksiyonu çağır
        client = get_elasticsearch_client()
        
        # Elasticsearch'in doğru parametrelerle çağrıldığını doğrula
        mock_elasticsearch.assert_called_once()
        self.assertEqual(client, mock_es)
    
    @patch('app.elasticsearch_client.get_elasticsearch_client')
    def test_check_elasticsearch_connection_success(self, mock_get_client):
        """Elasticsearch bağlantı kontrolünün başarılı olduğu durumu test eder."""
        # Mock client
        mock_client = MagicMock()
        mock_client.ping.return_value = True
        mock_get_client.return_value = mock_client
        
        # Fonksiyonu çağır
        result = check_elasticsearch_connection()
        
        # Sonucu doğrula
        self.assertTrue(result)
        mock_client.ping.assert_called_once()
    
    @patch('app.elasticsearch_client.get_elasticsearch_client')
    def test_check_elasticsearch_connection_failure(self, mock_get_client):
        """Elasticsearch bağlantı kontrolünün başarısız olduğu durumu test eder."""
        # Mock client
        mock_client = MagicMock()
        mock_client.ping.side_effect = Exception("Connection error")
        mock_get_client.return_value = mock_client
        
        # Fonksiyonu çağır
        result = check_elasticsearch_connection()
        
        # Sonucu doğrula
        self.assertFalse(result)
    
    @patch('app.elasticsearch_client.get_elasticsearch_client')
    def test_get_logs(self, mock_get_client):
        """get_logs fonksiyonunun doğru çalıştığını test eder."""
        # Mock client ve yanıtı
        mock_client = MagicMock()
        mock_response = {
            "hits": {
                "hits": [
                    {
                        "_source": {
                            "@timestamp": "2025-05-22T10:00:00.000Z",
                            "level": "INFO",
                            "service": "test-service",
                            "message": "Test log message"
                        }
                    }
                ]
            }
        }
        mock_client.search.return_value = mock_response
        mock_get_client.return_value = mock_client
        
        # Fonksiyonu çağır
        logs = get_logs(
            service="test-service",
            level="INFO",
            start_time="2025-05-22T00:00:00.000Z",
            end_time="2025-05-22T23:59:59.999Z",
            limit=10
        )
        
        # Sonucu doğrula
        self.assertEqual(len(logs), 1)
        self.assertEqual(logs[0]["timestamp"], "2025-05-22T10:00:00.000Z")
        self.assertEqual(logs[0]["level"], "INFO")
        self.assertEqual(logs[0]["service"], "test-service")
        self.assertEqual(logs[0]["message"], "Test log message")
        
        # search fonksiyonunun doğru parametrelerle çağrıldığını doğrula
        mock_client.search.assert_called_once()
        call_args = mock_client.search.call_args[1]
        self.assertIn("index", call_args)
        self.assertIn("body", call_args)
        
        # Query'nin doğru oluşturulduğunu doğrula
        query = call_args["body"]["query"]["bool"]["must"]
        self.assertEqual(len(query), 3)  # service, level ve time range filtresi
    
    @patch('app.elasticsearch_client.get_logs')
    def test_get_error_logs(self, mock_get_logs):
        """get_error_logs fonksiyonunun doğru çalıştığını test eder."""
        # Mock get_logs yanıtı
        mock_get_logs.return_value = [{"message": "Error log"}]
        
        # Fonksiyonu çağır
        logs = get_error_logs(
            service="test-service",
            start_time="2025-05-22T00:00:00.000Z",
            end_time="2025-05-22T23:59:59.999Z"
        )
        
        # Sonucu doğrula
        self.assertEqual(len(logs), 1)
        self.assertEqual(logs[0]["message"], "Error log")
        
        # get_logs fonksiyonunun doğru parametrelerle çağrıldığını doğrula
        mock_get_logs.assert_called_once_with(
            "test-service", 
            "ERROR", 
            "2025-05-22T00:00:00.000Z", 
            "2025-05-22T23:59:59.999Z", 
            100
        )
    
    @patch('app.elasticsearch_client.get_elasticsearch_client')
    def test_get_service_error_count(self, mock_get_client):
        """get_service_error_count fonksiyonunun doğru çalıştığını test eder."""
        # Mock client ve yanıtı
        mock_client = MagicMock()
        mock_client.count.return_value = {"count": 5}
        mock_get_client.return_value = mock_client
        
        # Fonksiyonu çağır
        count = get_service_error_count(
            service="test-service",
            start_time="2025-05-22T00:00:00.000Z",
            end_time="2025-05-22T23:59:59.999Z"
        )
        
        # Sonucu doğrula
        self.assertEqual(count, 5)
        
        # count fonksiyonunun doğru parametrelerle çağrıldığını doğrula
        mock_client.count.assert_called_once()
        call_args = mock_client.count.call_args[1]
        self.assertIn("index", call_args)
        self.assertIn("body", call_args)
        
        # Query'nin doğru oluşturulduğunu doğrula
        query = call_args["body"]["query"]["bool"]["must"]
        self.assertEqual(len(query), 3)  # service, level ve time range filtresi
    
    @patch('app.elasticsearch_client.get_logs')
    def test_get_service_last_log(self, mock_get_logs):
        """get_service_last_log fonksiyonunun doğru çalıştığını test eder."""
        # Mock get_logs yanıtı
        mock_log = {"message": "Last log"}
        mock_get_logs.return_value = [mock_log]
        
        # Fonksiyonu çağır
        log = get_service_last_log("test-service")
        
        # Sonucu doğrula
        self.assertEqual(log, mock_log)
        
        # get_logs fonksiyonunun doğru parametrelerle çağrıldığını doğrula
        mock_get_logs.assert_called_once_with(service="test-service", limit=1)
    
    @patch('app.elasticsearch_client.get_logs')
    def test_get_service_last_log_empty(self, mock_get_logs):
        """get_service_last_log fonksiyonunun boş sonuç döndüğü durumu test eder."""
        # Mock get_logs yanıtı
        mock_get_logs.return_value = []
        
        # Fonksiyonu çağır
        log = get_service_last_log("test-service")
        
        # Sonucu doğrula
        self.assertIsNone(log)

if __name__ == '__main__':
    unittest.main() 