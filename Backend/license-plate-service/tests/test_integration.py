import pytest
import requests
import json
import os
import sys
import logging
from datetime import datetime, timedelta
from unittest.mock import patch, MagicMock

# Proje kök dizinini sys.path'e ekle
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Test modüllerini içe aktar
from app.crud import calculate_parking_fee, close_parking_record
from app.models import ParkingRecord, Vehicle
from app.database import SessionLocal

# Logging yapılandırması
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Testlerde kullanılacak sabitler
TEST_PLATE = "34TEST99"
MOCK_PARKING_ID = 1
HOURLY_RATE = 15.0  # 15 TL/saat

class TestParkingIntegration:
    """
    Parking Management Service ile entegrasyonu test eden sınıf.
    Bu testler, gerçek API isteği göndermeden, mocklanmış yanıtlarla entegrasyon davranışını test eder.
    """
    
    @pytest.fixture
    def mock_parking_api(self):
        """Parking Management Service API'sini mocklar"""
        with patch("requests.get") as mock_get:
            # Mock yanıt nesnesi
            mock_response = MagicMock()
            mock_response.status_code = 200
            mock_response.json.return_value = {
                "id": MOCK_PARKING_ID,
                "name": "Test Otoparkı",
                "rate": HOURLY_RATE,
                "capacity": 100,
                "available_spots": 50
            }
            mock_get.return_value = mock_response
            yield mock_get
    
    @pytest.fixture
    def mock_db_session(self):
        """Veritabanı oturumunu mocklar"""
        session = MagicMock()
        
        # Commit ve refresh metodlarını mock'la
        session.commit = MagicMock()
        session.refresh = MagicMock()
        session.rollback = MagicMock()
        
        yield session
    
    def test_calculate_parking_fee_integration(self, mock_parking_api):
        """
        Parking Management Service'den ücret bilgisi çekme entegrasyonunu test eder.
        """
        # Giriş ve çıkış zamanlarını ayarla (2 saat fark)
        entry_time = datetime(2023, 1, 1, 10, 0, 0)
        exit_time = entry_time + timedelta(hours=2)
        
        # Ücret hesapla
        fee = calculate_parking_fee(entry_time, exit_time, MOCK_PARKING_ID)
        
        # Sonuç 3000 kuruş (30 TL - 15 TL/saat * 2 saat) olmalı
        assert fee == 3000
        
        # API çağrısı doğru URL ile yapılmış olmalı
        mock_parking_api.assert_called_once()
        assert f"/api/parkings/{MOCK_PARKING_ID}" in mock_parking_api.call_args[0][0]
    
    def test_close_parking_record_integration(self, mock_parking_api, mock_db_session):
        """
        Park kaydı kapatma ve Parking Management Service'den ücret hesaplama entegrasyonunu test eder.
        """
        # Test araç ve park kaydı nesneleri oluştur
        test_vehicle = Vehicle(id=1, license_plate=TEST_PLATE)
        
        # 2 saat önce başlayan park kaydı
        entry_time = datetime.now() - timedelta(hours=2)
        test_record = ParkingRecord(
            id=1,
            vehicle_id=test_vehicle.id,
            entry_time=entry_time,
            is_active=True
        )
        
        # mock_db_session.query ile Vehicle ve ParkingRecord nesnelerini getirmeyi mockla
        mock_db_session.query.return_value.filter.return_value.first.side_effect = [
            test_record  # İlk çağrıda park kaydını döndür
        ]
        
        # close_parking_record çağır
        closed_record = close_parking_record(mock_db_session, test_record.id, MOCK_PARKING_ID)
        
        # Kayıt kapatılmış olmalı
        assert closed_record is not None
        assert closed_record.is_active == False
        assert closed_record.exit_time is not None
        
        # Ücret hesaplanmış olmalı (~30 TL - 15 TL/saat * 2 saat)
        assert closed_record.parking_fee == 3000
        
        # Veritabanı metodları çağrılmış olmalı
        mock_db_session.commit.assert_called_once()
        mock_db_session.refresh.assert_called_once_with(test_record)
        
        # Parking Management Service API çağrısı yapılmış olmalı
        mock_parking_api.assert_called_once()
        assert f"/api/parkings/{MOCK_PARKING_ID}" in mock_parking_api.call_args[0][0]

class TestEntryExitFlow:
    """
    Araç giriş-çıkış akışını end-to-end test eden sınıf.
    Bu testler, servislerin birlikte çalışması durumunda tüm akışı test eder.
    """
    
    @pytest.fixture
    def mock_apis(self):
        """Tüm dış servisleri mocklar"""
        with patch("requests.get") as mock_get, \
             patch("requests.post") as mock_post, \
             patch("app.model.process_image_for_plate_recognition") as mock_process_image, \
             patch("app.main.get_vehicle_by_license_plate") as mock_get_vehicle, \
             patch("app.main.create_vehicle") as mock_create_vehicle, \
             patch("app.main.get_active_parking_record_by_vehicle") as mock_get_record, \
             patch("app.main.create_parking_record") as mock_create_record, \
             patch("app.main.close_parking_record") as mock_close_record:
            
            # Parking Management Service API mock'u
            mock_response = MagicMock()
            mock_response.status_code = 200
            mock_response.json.return_value = {
                "id": MOCK_PARKING_ID,
                "name": "Test Otoparkı",
                "rate": HOURLY_RATE,
                "capacity": 100,
                "available_spots": 50
            }
            mock_get.return_value = mock_response
            
            # Plaka tanıma mock'u
            mock_process_image.return_value = {
                "license_plates": [TEST_PLATE],
                "confidence": 0.92,
                "processing_time": 0.156
            }
            
            # Araç bulunamadı -> yeni oluşturulacak
            mock_get_vehicle.return_value = None
            
            # Mock araç
            mock_vehicle = MagicMock()
            mock_vehicle.id = 1
            mock_vehicle.license_plate = TEST_PLATE
            mock_create_vehicle.return_value = mock_vehicle
            
            # Aktif park kaydı yok -> yeni oluşturulacak
            mock_get_record.return_value = None
            
            # Mock park kaydı
            mock_record = MagicMock()
            mock_record.id = 1
            mock_record.vehicle_id = 1
            mock_record.entry_time = datetime.now() - timedelta(hours=2)
            mock_record.is_active = True
            mock_create_record.return_value = mock_record
            
            # Kapatılan park kaydı
            mock_closed_record = MagicMock()
            mock_closed_record.id = 1
            mock_closed_record.vehicle_id = 1
            mock_closed_record.entry_time = datetime.now() - timedelta(hours=2)
            mock_closed_record.exit_time = datetime.now()
            mock_closed_record.is_active = False
            mock_closed_record.parking_fee = 3000  # 30 TL
            mock_close_record.return_value = mock_closed_record
            
            yield {
                "mock_get": mock_get,
                "mock_post": mock_post,
                "mock_process_image": mock_process_image,
                "mock_get_vehicle": mock_get_vehicle,
                "mock_create_vehicle": mock_create_vehicle,
                "mock_get_record": mock_get_record,
                "mock_create_record": mock_create_record,
                "mock_close_record": mock_close_record
            }
    
    def test_complete_entry_exit_flow(self, mock_apis):
        """
        Tam bir araç giriş-çıkış akışını test eder.
        Bu test, bir aracın girişi ve sonrasında çıkışında tüm servis entegrasyonlarını kontrol eder.
        """
        from fastapi.testclient import TestClient
        from app.main import app
        
        client = TestClient(app)
        
        # Test görüntüsü oluştur (base64 kodlu boş bir PNG)
        image_data = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
        image_bytes = image_data.encode("utf-8")
        
        # 1. Araç girişi testi
        with patch("app.main.run_async") as mock_run_async:
            files = {"file": ("test.png", image_bytes, "image/png")}
            
            # process-plate-entry endpoint'ine istek gönder
            entry_response = client.post("/process-plate-entry", files=files)
            
            # Yanıt başarılı olmalı
            assert entry_response.status_code == 200
            entry_data = entry_response.json()
            assert entry_data["success"] == True
            assert entry_data["vehicle"]["license_plate"] == TEST_PLATE
            assert entry_data["parking_record_id"] == 1
            
            # Mock çağrıları doğru olmalı
            mock_apis["mock_process_image"].assert_called_once()
            mock_apis["mock_get_vehicle"].assert_called_once()
            mock_apis["mock_create_vehicle"].assert_called_once()
            mock_apis["mock_get_record"].assert_called_once()
            mock_apis["mock_create_record"].assert_called_once()
        
        # 2. Araç çıkışı testi - API çağrılarını sıfırla ve mock'ları ayarla
        mock_apis["mock_process_image"].reset_mock()
        mock_apis["mock_get_vehicle"].reset_mock()
        mock_apis["mock_get_record"].reset_mock()
        
        # get_vehicle artık araç bulmalı
        mock_vehicle = MagicMock()
        mock_vehicle.id = 1
        mock_vehicle.license_plate = TEST_PLATE
        mock_apis["mock_get_vehicle"].return_value = mock_vehicle
        
        # get_record artık aktif kayıt bulmalı
        mock_record = MagicMock()
        mock_record.id = 1
        mock_record.vehicle_id = 1
        mock_record.entry_time = datetime.now() - timedelta(hours=2)
        mock_record.is_active = True
        mock_apis["mock_get_record"].return_value = mock_record
        
        with patch("app.main.run_async") as mock_run_async:
            files = {"file": ("test.png", image_bytes, "image/png")}
            
            # process-plate-exit endpoint'ine istek gönder
            exit_response = client.post("/process-plate-exit?parking_id=1", files=files)
            
            # Yanıt başarılı olmalı
            assert exit_response.status_code == 200
            exit_data = exit_response.json()
            assert exit_data["success"] == True
            assert "exit_time" in exit_data
            assert "entry_time" in exit_data
            assert "duration_hours" in exit_data
            assert "parking_fee" in exit_data
            assert exit_data["parking_record_id"] == 1
            
            # Ücret doğru olmalı
            assert exit_data["parking_fee"] == 30.0  # 30 TL
            
            # Mock çağrıları doğru olmalı
            mock_apis["mock_process_image"].assert_called_once()
            mock_apis["mock_get_vehicle"].assert_called_once()
            mock_apis["mock_get_record"].assert_called_once()
            mock_apis["mock_close_record"].assert_called_once()
            mock_apis["mock_get"].assert_called_once()  # Parking API çağrısı

class TestEdgeCaseScenarios:
    """
    Sınır durumlar ve hata senaryoları için entegrasyon testleri
    """
    
    @pytest.fixture
    def mock_env(self):
        """Test ortamını hazırlar"""
        # Gerçek URL yerine test URL'i kullan
        original_url = os.environ.get("PARKING_MANAGEMENT_SERVICE_URL")
        os.environ["PARKING_MANAGEMENT_SERVICE_URL"] = "http://test-parking-service:8081"
        
        yield
        
        # Testi tamamladıktan sonra orijinal değeri geri yükle
        if original_url:
            os.environ["PARKING_MANAGEMENT_SERVICE_URL"] = original_url
        else:
            del os.environ["PARKING_MANAGEMENT_SERVICE_URL"]
    
    def test_service_unreachable(self, mock_env):
        """
        Parking Management Service erişilemediğinde ücret hesaplama davranışını test eder
        """
        # Giriş ve çıkış zamanlarını ayarla
        entry_time = datetime(2023, 1, 1, 10, 0, 0)
        exit_time = entry_time + timedelta(hours=1)
        
        # requests.get'i patch et ve ConnectionError fırlat
        with patch("requests.get") as mock_get:
            mock_get.side_effect = requests.exceptions.ConnectionError("Bağlantı hatası")
            
            # Fonksiyon ValueError fırlatmalı
            with pytest.raises(ValueError) as e:
                calculate_parking_fee(entry_time, exit_time, MOCK_PARKING_ID)
            
            # Hata mesajı doğru olmalı
            assert "ücret bilgisi alınamadı" in str(e.value).lower()
    
    def test_invalid_parking_id(self, mock_env):
        """
        Geçersiz otopark ID'si için ücret hesaplama davranışını test eder
        """
        # Giriş ve çıkış zamanlarını ayarla
        entry_time = datetime(2023, 1, 1, 10, 0, 0)
        exit_time = entry_time + timedelta(hours=1)
        
        # requests.get'i patch et ve 404 dönüş kodu ile yanıt döndür
        with patch("requests.get") as mock_get:
            mock_response = MagicMock()
            mock_response.status_code = 404
            mock_get.return_value = mock_response
            
            # Fonksiyon ValueError fırlatmalı
            with pytest.raises(ValueError) as e:
                calculate_parking_fee(entry_time, exit_time, 9999)  # Geçersiz ID
            
            # Hata mesajı doğru olmalı
            assert "ücret bilgisi alınamadı" in str(e.value).lower()
            
            # API çağrısı geçersiz otopark ID'si ile yapılmış olmalı
            mock_get.assert_called_once()
            assert "/api/parkings/9999" in mock_get.call_args[0][0] 