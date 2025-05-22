"""
Dashboard için unit testler.
"""
import unittest
from unittest.mock import patch, MagicMock
from datetime import datetime

from app.dashboard import (
    get_dashboard_summary,
    get_traffic_metrics,
    get_performance_metrics,
    get_error_metrics
)

class TestDashboard(unittest.TestCase):
    """Dashboard için test sınıfı."""
    
    @patch('app.dashboard.get_system_health')
    @patch('app.dashboard.get_service_request_count')
    @patch('app.dashboard.get_error_logs')
    @patch('app.dashboard.get_vehicle_entry_count')
    @patch('app.dashboard.get_vehicle_exit_count')
    @patch('app.dashboard.get_service_avg_response_time')
    def test_get_dashboard_summary(self, mock_avg_time, mock_vehicle_exit, 
                                  mock_vehicle_entry, mock_error_logs, 
                                  mock_request_count, mock_health):
        """get_dashboard_summary fonksiyonunun doğru çalıştığını test eder."""
        # Mock dönüş değerleri
        mock_health.return_value = {"overall_status": "healthy"}
        mock_request_count.return_value = 100
        mock_error_logs.return_value = [{"message": "Error 1"}, {"message": "Error 2"}]
        mock_vehicle_entry.return_value = 50
        mock_vehicle_exit.return_value = 30
        mock_avg_time.return_value = 0.05
        
        # Fonksiyonu çağır
        result = get_dashboard_summary()
        
        # Sonucu doğrula
        self.assertEqual(result["health"], {"overall_status": "healthy"})
        self.assertEqual(result["total_requests"], 600)  # 6 servis * 100 istek
        self.assertEqual(result["total_errors"], 2)
        self.assertEqual(result["vehicle_entries"], 50)
        self.assertEqual(result["vehicle_exits"], 30)
        self.assertEqual(len(result["service_metrics"]), 6)  # 6 servis
        self.assertEqual(len(result["recent_errors"]), 2)
    
    @patch('app.dashboard.get_system_health')
    @patch('app.dashboard.get_service_request_count')
    @patch('app.dashboard.get_error_logs')
    def test_get_dashboard_summary_exception(self, mock_error_logs, 
                                           mock_request_count, mock_health):
        """get_dashboard_summary fonksiyonunun istisna durumunu test eder."""
        # Mock exception
        mock_health.side_effect = Exception("Database connection error")
        
        # Fonksiyonu çağır
        result = get_dashboard_summary()
        
        # Sonucu doğrula
        self.assertIn("error", result)
        self.assertEqual(result["error"], "Database connection error")
    
    @patch('app.dashboard.get_metric_range')
    def test_get_traffic_metrics(self, mock_metric_range):
        """get_traffic_metrics fonksiyonunun doğru çalıştığını test eder."""
        # Mock dönüş değerleri
        mock_entries = {"metric": "vehicle_entries_total", "values": [{"timestamp": "2023-01-01T00:00:00", "value": 10}]}
        mock_exits = {"metric": "vehicle_exits_total", "values": [{"timestamp": "2023-01-01T00:00:00", "value": 8}]}
        
        mock_metric_range.side_effect = [mock_entries, mock_exits]
        
        # Fonksiyonu çağır
        result = get_traffic_metrics(
            start_time="2023-01-01T00:00:00",
            end_time="2023-01-01T23:59:59",
            step="1h"
        )
        
        # Sonucu doğrula
        self.assertEqual(result["entries"], mock_entries)
        self.assertEqual(result["exits"], mock_exits)
    
    @patch('app.dashboard.get_metric_range')
    def test_get_traffic_metrics_exception(self, mock_metric_range):
        """get_traffic_metrics fonksiyonunun istisna durumunu test eder."""
        # Mock exception
        mock_metric_range.side_effect = Exception("Prometheus connection error")
        
        # Fonksiyonu çağır
        result = get_traffic_metrics()
        
        # Sonucu doğrula
        self.assertIn("error", result)
        self.assertEqual(result["error"], "Prometheus connection error")
    
    @patch('app.dashboard.get_metric_range')
    def test_get_performance_metrics_single_service(self, mock_metric_range):
        """get_performance_metrics fonksiyonunun tek servis için doğru çalıştığını test eder."""
        # Mock dönüş değerleri
        mock_requests = {"metric": "http_requests_total", "values": [{"timestamp": "2023-01-01T00:00:00", "value": 100}]}
        mock_time = {"metric": "response_time", "values": [{"timestamp": "2023-01-01T00:00:00", "value": 0.05}]}
        
        mock_metric_range.side_effect = [mock_requests, mock_time]
        
        # Fonksiyonu çağır
        result = get_performance_metrics(
            service="test-service",
            start_time="2023-01-01T00:00:00",
            end_time="2023-01-01T23:59:59"
        )
        
        # Sonucu doğrula
        self.assertIn("services", result)
        self.assertIn("test-service", result["services"])
        self.assertEqual(result["services"]["test-service"]["request_count"], mock_requests)
        self.assertEqual(result["services"]["test-service"]["response_time"], mock_time)
    
    @patch('app.dashboard.MONITORED_SERVICES', ['service1', 'service2'])
    @patch('app.dashboard.get_metric_range')
    def test_get_performance_metrics_all_services(self, mock_metric_range):
        """get_performance_metrics fonksiyonunun tüm servisler için doğru çalıştığını test eder."""
        # Mock dönüş değerleri
        mock_metric_range.return_value = {"metric": "test_metric", "values": []}
        
        # Fonksiyonu çağır
        result = get_performance_metrics()
        
        # Sonucu doğrula
        self.assertIn("services", result)
        self.assertIn("service1", result["services"])
        self.assertIn("service2", result["services"])
        
        # get_metric_range fonksiyonunun doğru sayıda çağrıldığını doğrula (2 servis * 2 metrik)
        self.assertEqual(mock_metric_range.call_count, 4)
    
    @patch('app.dashboard.get_error_logs')
    def test_get_error_metrics(self, mock_error_logs):
        """get_error_metrics fonksiyonunun doğru çalıştığını test eder."""
        # Mock dönüş değerleri
        now = datetime.now()
        mock_error_logs.return_value = [
            {"timestamp": now.replace(hour=10, minute=15).isoformat(), "message": "Error 1"},
            {"timestamp": now.replace(hour=10, minute=30).isoformat(), "message": "Error 2"},
            {"timestamp": now.replace(hour=10, minute=45).isoformat(), "message": "Error 3"},
            {"timestamp": now.replace(hour=11, minute=15).isoformat(), "message": "Error 4"}
        ]
        
        # Fonksiyonu çağır
        result = get_error_metrics(step="1h")
        
        # Sonucu doğrula
        self.assertEqual(result["metric"], "error_count")
        self.assertEqual(len(result["values"]), 2)  # 2 farklı saat (10 ve 11)
        
        # İlk saatte 3, ikinci saatte 1 hata var
        hour_10_count = next((item["value"] for item in result["values"] 
                            if item["timestamp"].startswith(now.replace(hour=10, minute=0).isoformat()[:13])), 0)
        hour_11_count = next((item["value"] for item in result["values"] 
                            if item["timestamp"].startswith(now.replace(hour=11, minute=0).isoformat()[:13])), 0)
        
        self.assertEqual(hour_10_count, 3)
        self.assertEqual(hour_11_count, 1)
    
    @patch('app.dashboard.get_error_logs')
    def test_get_error_metrics_exception(self, mock_error_logs):
        """get_error_metrics fonksiyonunun istisna durumunu test eder."""
        # Mock exception
        mock_error_logs.side_effect = Exception("Elasticsearch connection error")
        
        # Fonksiyonu çağır
        result = get_error_metrics()
        
        # Sonucu doğrula
        self.assertIn("error", result)
        self.assertEqual(result["error"], "Elasticsearch connection error")

if __name__ == '__main__':
    unittest.main() 