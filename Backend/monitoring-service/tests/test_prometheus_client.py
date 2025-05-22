"""
Prometheus client için unit testler.
"""
import unittest
from unittest.mock import patch, MagicMock
from datetime import datetime

from app.prometheus_client import (
    check_prometheus_connection,
    parse_time_window,
    get_metric_range,
    get_metric_instant,
    get_service_request_count,
    get_service_avg_response_time
)

class TestPrometheusClient(unittest.TestCase):
    """Prometheus client için test sınıfı."""
    
    @patch('app.prometheus_client.requests.get')
    def test_check_prometheus_connection_success(self, mock_get):
        """Prometheus bağlantı kontrolünün başarılı olduğu durumu test eder."""
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_get.return_value = mock_response
        
        # Fonksiyonu çağır
        result = check_prometheus_connection()
        
        # Sonucu doğrula
        self.assertTrue(result)
        mock_get.assert_called_once()
    
    @patch('app.prometheus_client.requests.get')
    def test_check_prometheus_connection_failure(self, mock_get):
        """Prometheus bağlantı kontrolünün başarısız olduğu durumu test eder."""
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 500
        mock_get.return_value = mock_response
        
        # Fonksiyonu çağır
        result = check_prometheus_connection()
        
        # Sonucu doğrula
        self.assertFalse(result)
    
    @patch('app.prometheus_client.requests.get')
    def test_check_prometheus_connection_exception(self, mock_get):
        """Prometheus bağlantı kontrolünün istisna fırlattığı durumu test eder."""
        # Mock exception
        mock_get.side_effect = Exception("Connection error")
        
        # Fonksiyonu çağır
        result = check_prometheus_connection()
        
        # Sonucu doğrula
        self.assertFalse(result)
    
    def test_parse_time_window(self):
        """parse_time_window fonksiyonunun doğru çalıştığını test eder."""
        # Saniye
        self.assertEqual(parse_time_window("30s"), 30)
        
        # Dakika
        self.assertEqual(parse_time_window("5m"), 300)
        
        # Saat
        self.assertEqual(parse_time_window("2h"), 7200)
        
        # Gün
        self.assertEqual(parse_time_window("1d"), 86400)
        
        # Geçersiz birim
        with patch('app.prometheus_client.logger.warning') as mock_warning:
            self.assertEqual(parse_time_window("10x"), parse_time_window("24h"))
            mock_warning.assert_called_once()
        
        # Geçersiz değer
        with patch('app.prometheus_client.logger.warning') as mock_warning:
            self.assertEqual(parse_time_window("abc"), parse_time_window("24h"))
            mock_warning.assert_called_once()
    
    @patch('app.prometheus_client.requests.get')
    def test_get_metric_range_success(self, mock_get):
        """get_metric_range fonksiyonunun başarılı olduğu durumu test eder."""
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "status": "success",
            "data": {
                "result": [
                    {
                        "values": [
                            [1621500000, "42.5"],
                            [1621500300, "43.2"]
                        ]
                    }
                ]
            }
        }
        mock_get.return_value = mock_response
        
        # Fonksiyonu çağır
        result = get_metric_range(
            metric_name="test_metric",
            start_time="2025-05-22T00:00:00.000Z",
            end_time="2025-05-22T23:59:59.999Z",
            step="5m"
        )
        
        # Sonucu doğrula
        self.assertEqual(result["metric"], "test_metric")
        self.assertEqual(len(result["values"]), 2)
        self.assertEqual(result["values"][0]["value"], 42.5)
        self.assertEqual(result["values"][1]["value"], 43.2)
        
        # requests.get fonksiyonunun doğru parametrelerle çağrıldığını doğrula
        mock_get.assert_called_once()
        call_args = mock_get.call_args[1]
        self.assertIn("params", call_args)
        params = call_args["params"]
        self.assertEqual(params["query"], "test_metric")
        self.assertEqual(params["step"], "5m")
    
    @patch('app.prometheus_client.requests.get')
    def test_get_metric_range_api_error(self, mock_get):
        """get_metric_range fonksiyonunun API hatası durumunu test eder."""
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 500
        mock_response.text = "Internal Server Error"
        mock_get.return_value = mock_response
        
        # Fonksiyonu çağır
        result = get_metric_range("test_metric")
        
        # Sonucu doğrula
        self.assertEqual(result["metric"], "test_metric")
        self.assertEqual(len(result["values"]), 0)
    
    @patch('app.prometheus_client.requests.get')
    def test_get_metric_range_api_failure(self, mock_get):
        """get_metric_range fonksiyonunun API başarısızlık durumunu test eder."""
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "status": "error",
            "error": "Query timeout"
        }
        mock_get.return_value = mock_response
        
        # Fonksiyonu çağır
        result = get_metric_range("test_metric")
        
        # Sonucu doğrula
        self.assertEqual(result["metric"], "test_metric")
        self.assertEqual(len(result["values"]), 0)
    
    @patch('app.prometheus_client.requests.get')
    def test_get_metric_range_empty_result(self, mock_get):
        """get_metric_range fonksiyonunun boş sonuç durumunu test eder."""
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "status": "success",
            "data": {
                "result": []
            }
        }
        mock_get.return_value = mock_response
        
        # Fonksiyonu çağır
        result = get_metric_range("test_metric")
        
        # Sonucu doğrula
        self.assertEqual(result["metric"], "test_metric")
        self.assertEqual(len(result["values"]), 0)
    
    @patch('app.prometheus_client.requests.get')
    def test_get_metric_instant_success(self, mock_get):
        """get_metric_instant fonksiyonunun başarılı olduğu durumu test eder."""
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "status": "success",
            "data": {
                "result": [
                    {
                        "value": [1621500000, "42.5"]
                    }
                ]
            }
        }
        mock_get.return_value = mock_response
        
        # Fonksiyonu çağır
        result = get_metric_instant("test_metric")
        
        # Sonucu doğrula
        self.assertEqual(result, 42.5)
        
        # requests.get fonksiyonunun doğru parametrelerle çağrıldığını doğrula
        mock_get.assert_called_once()
        call_args = mock_get.call_args[1]
        self.assertIn("params", call_args)
        params = call_args["params"]
        self.assertEqual(params["query"], "test_metric")
    
    @patch('app.prometheus_client.requests.get')
    def test_get_metric_instant_api_error(self, mock_get):
        """get_metric_instant fonksiyonunun API hatası durumunu test eder."""
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 500
        mock_get.return_value = mock_response
        
        # Fonksiyonu çağır
        result = get_metric_instant("test_metric")
        
        # Sonucu doğrula
        self.assertEqual(result, 0.0)
    
    @patch('app.prometheus_client.get_metric_instant')
    def test_get_service_request_count(self, mock_get_metric):
        """get_service_request_count fonksiyonunun doğru çalıştığını test eder."""
        # Mock get_metric_instant yanıtı
        mock_get_metric.return_value = 42.0
        
        # Fonksiyonu çağır
        count = get_service_request_count("test-service")
        
        # Sonucu doğrula
        self.assertEqual(count, 42)
        
        # get_metric_instant fonksiyonunun doğru parametrelerle çağrıldığını doğrula
        mock_get_metric.assert_called_once()
        call_args = mock_get_metric.call_args[0]
        self.assertEqual(call_args[0], 'http_requests_total{service="test-service"}')
    
    @patch('app.prometheus_client.get_metric_instant')
    def test_get_service_avg_response_time(self, mock_get_metric):
        """get_service_avg_response_time fonksiyonunun doğru çalıştığını test eder."""
        # Mock get_metric_instant yanıtı
        mock_get_metric.return_value = 0.125
        
        # Fonksiyonu çağır
        avg_time = get_service_avg_response_time("test-service")
        
        # Sonucu doğrula
        self.assertEqual(avg_time, 0.125)
        
        # get_metric_instant fonksiyonunun doğru parametrelerle çağrıldığını doğrula
        mock_get_metric.assert_called_once()

if __name__ == '__main__':
    unittest.main() 