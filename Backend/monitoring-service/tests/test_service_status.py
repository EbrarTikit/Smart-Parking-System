"""
Service status için unit testler.
"""
import unittest
from unittest.mock import patch, MagicMock
from datetime import datetime, timedelta

from app.service_status import (
    get_service_status,
    get_all_services_status,
    get_system_health
)

class TestServiceStatus(unittest.TestCase):
    """Service status için test sınıfı."""
    
    @patch('app.service_status.get_service_last_log')
    @patch('app.service_status.get_service_error_count')
    @patch('app.service_status.get_service_request_count')
    @patch('app.service_status.get_service_avg_response_time')
    def test_get_service_status_healthy(self, mock_avg_time, mock_req_count, 
                                        mock_error_count, mock_last_log):
        """Sağlıklı servis durumu testi."""
        # Mock dönüş değerleri
        now = datetime.now()
        mock_last_log.return_value = {"timestamp": now.isoformat(), "message": "Service running"}
        mock_error_count.return_value = 0
        mock_req_count.return_value = 100
        mock_avg_time.return_value = 0.05
        
        # Fonksiyonu çağır
        result = get_service_status("test-service")
        
        # Sonucu doğrula
        self.assertEqual(result["service"], "test-service")
        self.assertEqual(result["status"], "healthy")
        self.assertEqual(result["error_count"], 0)
        self.assertEqual(result["request_count"], 100)
        self.assertEqual(result["avg_response_time"], 0.05)
    
    @patch('app.service_status.get_service_last_log')
    @patch('app.service_status.get_service_error_count')
    @patch('app.service_status.get_service_request_count')
    @patch('app.service_status.get_service_avg_response_time')
    def test_get_service_status_warning(self, mock_avg_time, mock_req_count, 
                                        mock_error_count, mock_last_log):
        """Uyarı durumundaki servis testi."""
        # Mock dönüş değerleri
        now = datetime.now()
        mock_last_log.return_value = {"timestamp": now.isoformat(), "message": "Service running"}
        mock_error_count.return_value = 5  # 0'dan büyük, 10'dan küçük
        mock_req_count.return_value = 100
        mock_avg_time.return_value = 0.15
        
        # Fonksiyonu çağır
        result = get_service_status("test-service")
        
        # Sonucu doğrula
        self.assertEqual(result["status"], "warning")
        self.assertEqual(result["error_count"], 5)
    
    @patch('app.service_status.get_service_last_log')
    @patch('app.service_status.get_service_error_count')
    @patch('app.service_status.get_service_request_count')
    @patch('app.service_status.get_service_avg_response_time')
    def test_get_service_status_error(self, mock_avg_time, mock_req_count, 
                                      mock_error_count, mock_last_log):
        """Hata durumundaki servis testi."""
        # Mock dönüş değerleri
        now = datetime.now()
        mock_last_log.return_value = {"timestamp": now.isoformat(), "message": "Service running"}
        mock_error_count.return_value = 15  # 10'dan büyük
        mock_req_count.return_value = 100
        mock_avg_time.return_value = 0.25
        
        # Fonksiyonu çağır
        result = get_service_status("test-service")
        
        # Sonucu doğrula
        self.assertEqual(result["status"], "error")
        self.assertEqual(result["error_count"], 15)
    
    @patch('app.service_status.get_service_last_log')
    @patch('app.service_status.get_service_error_count')
    @patch('app.service_status.get_service_request_count')
    @patch('app.service_status.get_service_avg_response_time')
    def test_get_service_status_inactive_no_logs(self, mock_avg_time, mock_req_count, 
                                                mock_error_count, mock_last_log):
        """Log kaydı olmayan inaktif servis testi."""
        # Mock dönüş değerleri
        mock_last_log.return_value = None  # Log kaydı yok
        mock_error_count.return_value = 0
        mock_req_count.return_value = 0
        mock_avg_time.return_value = 0
        
        # Fonksiyonu çağır
        result = get_service_status("test-service")
        
        # Sonucu doğrula
        self.assertEqual(result["status"], "inactive")
        self.assertIsNone(result["last_seen"])
    
    @patch('app.service_status.get_service_last_log')
    @patch('app.service_status.get_service_error_count')
    @patch('app.service_status.get_service_request_count')
    @patch('app.service_status.get_service_avg_response_time')
    def test_get_service_status_inactive_old_logs(self, mock_avg_time, mock_req_count, 
                                                 mock_error_count, mock_last_log):
        """Eski log kaydı olan inaktif servis testi."""
        # Mock dönüş değerleri
        now = datetime.now()
        old_time = now - timedelta(minutes=10)  # 5 dakikadan eski
        mock_last_log.return_value = {"timestamp": old_time.isoformat(), "message": "Service running"}
        mock_error_count.return_value = 0
        mock_req_count.return_value = 50
        mock_avg_time.return_value = 0.1
        
        # Fonksiyonu çağır
        result = get_service_status("test-service")
        
        # Sonucu doğrula
        self.assertEqual(result["status"], "inactive")
        self.assertEqual(result["last_seen"], old_time.isoformat())
    
    @patch('app.service_status.get_service_last_log')
    @patch('app.service_status.get_service_error_count')
    @patch('app.service_status.get_service_request_count')
    @patch('app.service_status.get_service_avg_response_time')
    def test_get_service_status_exception(self, mock_avg_time, mock_req_count, 
                                         mock_error_count, mock_last_log):
        """İstisna durumundaki servis testi."""
        # Mock exception
        mock_last_log.side_effect = Exception("Database connection error")
        
        # Fonksiyonu çağır
        result = get_service_status("test-service")
        
        # Sonucu doğrula
        self.assertEqual(result["status"], "unknown")
        self.assertEqual(result["error"], "Database connection error")
    
    @patch('app.service_status.MONITORED_SERVICES', ['service1', 'service2', 'service3'])
    @patch('app.service_status.get_service_status')
    def test_get_all_services_status(self, mock_get_status):
        """Tüm servislerin durumunu getirme testi."""
        # Mock dönüş değerleri
        mock_get_status.side_effect = [
            {"service": "service1", "status": "healthy"},
            {"service": "service2", "status": "warning"},
            {"service": "service3", "status": "error"}
        ]
        
        # Fonksiyonu çağır
        result = get_all_services_status()
        
        # Sonucu doğrula
        self.assertEqual(len(result), 3)
        self.assertEqual(result[0]["service"], "service1")
        self.assertEqual(result[1]["service"], "service2")
        self.assertEqual(result[2]["service"], "service3")
        
        # get_service_status fonksiyonunun doğru sayıda çağrıldığını doğrula
        self.assertEqual(mock_get_status.call_count, 3)
    
    @patch('app.service_status.get_all_services_status')
    def test_get_system_health_healthy(self, mock_all_services):
        """Sağlıklı sistem durumu testi."""
        # Mock dönüş değerleri
        mock_all_services.return_value = [
            {"service": "service1", "status": "healthy"},
            {"service": "service2", "status": "healthy"},
            {"service": "service3", "status": "healthy"}
        ]
        
        # Fonksiyonu çağır
        result = get_system_health()
        
        # Sonucu doğrula
        self.assertEqual(result["overall_status"], "healthy")
        self.assertEqual(result["services"]["total"], 3)
        self.assertEqual(result["services"]["healthy"], 3)
        self.assertEqual(result["services"]["warning"], 0)
        self.assertEqual(result["services"]["error"], 0)
    
    @patch('app.service_status.get_all_services_status')
    def test_get_system_health_warning(self, mock_all_services):
        """Uyarı durumundaki sistem testi."""
        # Mock dönüş değerleri
        mock_all_services.return_value = [
            {"service": "service1", "status": "healthy"},
            {"service": "service2", "status": "warning"},
            {"service": "service3", "status": "healthy"},
            {"service": "service4", "status": "inactive"}
        ]
        
        # Fonksiyonu çağır
        result = get_system_health()
        
        # Sonucu doğrula
        self.assertEqual(result["overall_status"], "warning")
        self.assertEqual(result["services"]["total"], 4)
        self.assertEqual(result["services"]["healthy"], 2)
        self.assertEqual(result["services"]["warning"], 1)
        self.assertEqual(result["services"]["inactive"], 1)
    
    @patch('app.service_status.get_all_services_status')
    def test_get_system_health_error(self, mock_all_services):
        """Hata durumundaki sistem testi."""
        # Mock dönüş değerleri
        mock_all_services.return_value = [
            {"service": "service1", "status": "healthy"},
            {"service": "service2", "status": "warning"},
            {"service": "service3", "status": "error"},
            {"service": "service4", "status": "healthy"}
        ]
        
        # Fonksiyonu çağır
        result = get_system_health()
        
        # Sonucu doğrula
        self.assertEqual(result["overall_status"], "error")
        self.assertEqual(result["services"]["total"], 4)
        self.assertEqual(result["services"]["healthy"], 2)
        self.assertEqual(result["services"]["warning"], 1)
        self.assertEqual(result["services"]["error"], 1)
    
    @patch('app.service_status.get_all_services_status')
    def test_get_system_health_unknown(self, mock_all_services):
        """Bilinmeyen durumundaki sistem testi."""
        # Mock dönüş değerleri
        mock_all_services.return_value = [
            {"service": "service1", "status": "healthy"},
            {"service": "service2", "status": "unknown"},
            {"service": "service3", "status": "healthy"}
        ]
        
        # Fonksiyonu çağır
        result = get_system_health()
        
        # Sonucu doğrula
        self.assertEqual(result["overall_status"], "unknown")
        self.assertEqual(result["services"]["total"], 3)
        self.assertEqual(result["services"]["healthy"], 2)
        self.assertEqual(result["services"]["unknown"], 1)

if __name__ == '__main__':
    unittest.main() 