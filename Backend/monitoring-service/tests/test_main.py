"""
Main API için unit testler.
"""
import unittest
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient
from datetime import datetime

from app.main import app

class TestMainAPI(unittest.TestCase):
    """Main API için test sınıfı."""
    
    def setUp(self):
        """Test client oluştur."""
        self.client = TestClient(app)
    
    @patch('app.main.es.cluster.health')
    @patch('app.main.requests.get')
    def test_health_endpoint_healthy(self, mock_prometheus_get, mock_es_health):
        """Health endpoint'inin sağlıklı durumu test eder."""
        # Mock dönüş değerleri
        mock_es_health.return_value = {"status": "green"}
        
        mock_prometheus_response = MagicMock()
        mock_prometheus_response.status_code = 200
        mock_prometheus_get.return_value = mock_prometheus_response
        
        # Endpoint'i çağır
        response = self.client.get("/health")
        
        # Sonucu doğrula
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["status"], "healthy")
        self.assertEqual(data["elasticsearch"], "green")
        self.assertEqual(data["prometheus"], "healthy")
    
    @patch('app.main.es.cluster.health')
    @patch('app.main.requests.get')
    def test_health_endpoint_degraded(self, mock_prometheus_get, mock_es_health):
        """Health endpoint'inin bozuk durumu test eder."""
        # Mock dönüş değerleri
        mock_es_health.return_value = {"status": "yellow"}
        
        mock_prometheus_response = MagicMock()
        mock_prometheus_response.status_code = 500
        mock_prometheus_get.return_value = mock_prometheus_response
        
        # Endpoint'i çağır
        response = self.client.get("/health")
        
        # Sonucu doğrula
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["status"], "degraded")
        self.assertEqual(data["elasticsearch"], "yellow")
        self.assertEqual(data["prometheus"], "unhealthy")
    
    @patch('app.main.es.cluster.health')
    def test_health_endpoint_es_error(self, mock_es_health):
        """Health endpoint'inin Elasticsearch hatası durumunu test eder."""
        # Mock exception
        mock_es_health.side_effect = Exception("Connection error")
        
        # Endpoint'i çağır
        response = self.client.get("/health")
        
        # Sonucu doğrula
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["status"], "degraded")
        self.assertEqual(data["elasticsearch"], "unhealthy")
    
    @patch('app.main.es.search')
    def test_logs_endpoint(self, mock_es_search):
        """Logs endpoint'ini test eder."""
        # Mock dönüş değerleri
        mock_es_search.return_value = {
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
        
        # Endpoint'i çağır
        response = self.client.get("/logs?service=test-service&level=INFO&limit=10")
        
        # Sonucu doğrula
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]["timestamp"], "2025-05-22T10:00:00.000Z")
        self.assertEqual(data[0]["level"], "INFO")
        self.assertEqual(data[0]["service"], "test-service")
        self.assertEqual(data[0]["message"], "Test log message")
        
        # es.search fonksiyonunun doğru parametrelerle çağrıldığını doğrula
        mock_es_search.assert_called_once()
        call_args = mock_es_search.call_args[1]
        self.assertIn("index", call_args)
        self.assertIn("body", call_args)
        
        # Query'nin doğru oluşturulduğunu doğrula
        query = call_args["body"]["query"]["bool"]["must"]
        self.assertEqual(len(query), 2)  # service ve level filtresi
    
    @patch('app.main.es.search')
    def test_logs_endpoint_error(self, mock_es_search):
        """Logs endpoint'inin hata durumunu test eder."""
        # Mock exception
        mock_es_search.side_effect = Exception("Elasticsearch error")
        
        # Endpoint'i çağır
        response = self.client.get("/logs")
        
        # Sonucu doğrula
        self.assertEqual(response.status_code, 500)
        data = response.json()
        self.assertIn("detail", data)
        self.assertIn("Elasticsearch error", data["detail"])
    
    @patch('app.main.es.search')
    def test_errors_endpoint(self, mock_es_search):
        """Errors endpoint'ini test eder."""
        # Mock dönüş değerleri
        mock_es_search.return_value = {
            "hits": {
                "hits": [
                    {
                        "_source": {
                            "@timestamp": "2025-05-22T10:00:00.000Z",
                            "level": "ERROR",
                            "service": "test-service",
                            "message": "Test error message"
                        }
                    }
                ]
            }
        }
        
        # Endpoint'i çağır
        response = self.client.get("/errors?service=test-service&limit=10")
        
        # Sonucu doğrula
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]["level"], "ERROR")
        self.assertEqual(data[0]["message"], "Test error message")
    
    @patch('app.main.get_all_services_status')
    def test_services_status_endpoint(self, mock_get_status):
        """Services status endpoint'ini test eder."""
        # Mock dönüş değerleri
        mock_get_status.return_value = [
            {"service": "service1", "status": "healthy"},
            {"service": "service2", "status": "warning"}
        ]
        
        # Endpoint'i çağır
        response = self.client.get("/services/status")
        
        # Sonucu doğrula
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data), 2)
        self.assertEqual(data[0]["service"], "service1")
        self.assertEqual(data[0]["status"], "healthy")
        self.assertEqual(data[1]["service"], "service2")
        self.assertEqual(data[1]["status"], "warning")
    
    @patch('app.main.get_metric_range')
    def test_metrics_endpoint(self, mock_get_metric):
        """Metrics endpoint'ini test eder."""
        # Mock dönüş değerleri
        mock_get_metric.return_value = {
            "metric": "test_metric",
            "values": [
                {"timestamp": "2025-05-22T10:00:00.000Z", "value": 42.5}
            ]
        }
        
        # Endpoint'i çağır
        response = self.client.get("/metrics/test_metric?step=5m")
        
        # Sonucu doğrula
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["metric"], "test_metric")
        self.assertEqual(len(data["values"]), 1)
        self.assertEqual(data["values"][0]["value"], 42.5)
    
    @patch('app.main.get_dashboard_summary')
    def test_dashboard_summary_endpoint(self, mock_get_summary):
        """Dashboard summary endpoint'ini test eder."""
        # Mock dönüş değerleri
        mock_get_summary.return_value = {
            "health": {"overall_status": "healthy"},
            "total_requests": 1000,
            "total_errors": 5
        }
        
        # Endpoint'i çağır
        response = self.client.get("/dashboard/summary")
        
        # Sonucu doğrula
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["health"]["overall_status"], "healthy")
        self.assertEqual(data["total_requests"], 1000)
        self.assertEqual(data["total_errors"], 5)

if __name__ == '__main__':
    unittest.main() 