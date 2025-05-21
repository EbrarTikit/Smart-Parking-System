import pytest
from fastapi.testclient import TestClient
import sys
import os
import io
from unittest.mock import patch, MagicMock, AsyncMock
from datetime import datetime
import base64

# Proje kök dizinini sys.path'e ekle
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Gerekli paketleri mockla
sys.modules['ultralytics'] = MagicMock()
sys.modules['ultralytics.YOLO'] = MagicMock()
sys.modules['easyocr'] = MagicMock()
sys.modules['filterpy'] = MagicMock()
sys.modules['filterpy.kalman'] = MagicMock()
sys.modules['app.model.util'] = MagicMock()
sys.modules['cv2'] = MagicMock()

# Testlerde kullanılacak sabitler
TEST_PLATE = "34ABC123"
CONFIDENCE_SCORE = 0.95

# Model mocklanmasını gerçekleştir - global mock'u kaldır
# @patch("app.model.process_image_for_plate_recognition")
# def setup_mock_model(mock_process):
#     # Test edilecek modülleri içe aktar
#     from app.main import app
#     from app.crud import get_vehicle_by_license_plate, create_vehicle, get_active_parking_record_by_vehicle
#     from app.websocket import RoomType

#     # Global test mock değerlerini ayarla
#     mock_process.return_value = {
#         "license_plates": [TEST_PLATE],
#         "confidence": CONFIDENCE_SCORE,
#         "processing_time": 0.156
#     }
#     return mock_process

# # Model mocklanmasını gerçekleştir
# mock_process = setup_mock_model()

# Test edilecek modülleri içe aktar
from app.main import app
from app.crud import get_vehicle_by_license_plate, create_vehicle, get_active_parking_record_by_vehicle
from app.websocket import RoomType

class TestProcessPlateAPIs:
    """Plaka işleme API endpointleri için test suite"""
    
    @pytest.fixture
    def client(self):
        """FastAPI test client döndürür"""
        return TestClient(app=app, base_url="http://test")
    
    @pytest.fixture
    def test_image(self):
        """Test için plaka görüntüsü veri nesnesi döndürür"""
        # 1x1 piksel boyutunda basit bir beyaz PNG görüntüsü oluştur
        image_content = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")
        return io.BytesIO(image_content)

    # Önemli değişiklik: model ve mock dizaynını düzeltiyoruz
    @pytest.fixture
    def mock_process_image(self, monkeypatch):
        """Plaka tanıma işlemini mocklar"""
        # Önce app.main içindeki MODEL_AVAILABLE değişkenini True olarak ayarla
        monkeypatch.setattr("app.main.MODEL_AVAILABLE", True)
        
        # Mock fonksiyonu oluştur
        mock = MagicMock(name="process_image_for_plate_recognition")
        mock.return_value = {
            "license_plates": [TEST_PLATE],
            "confidence": CONFIDENCE_SCORE,
            "processing_time": 0.156
        }
        
        # Fonksiyonu app.main modülüne patch et
        monkeypatch.setattr("app.main.process_image_for_plate_recognition", mock)
        
        # Modelin kendisini de mockla
        monkeypatch.setattr("app.model.process_image_for_plate_recognition", mock)
        
        return mock
    
    # Bu fixture'ı kaldırıyoruz çünkü mock_process_image ile çakışıyor
    # @pytest.fixture
    # def mock_model_import(self):
    #     """Model modülü içe aktarımını mocklar"""
    #     # app.main içindeki model yükleme kısmını mockla
    #     with patch("app.main.MODEL_AVAILABLE", True), \
    #          patch("app.main.process_image_for_plate_recognition") as mock_process:
            
    #         mock_process.return_value = {
    #             "license_plates": [TEST_PLATE],
    #             "confidence": CONFIDENCE_SCORE,
    #             "processing_time": 0.156
    #         }
    #         yield mock_process
    
    # Bu fixture'ı kaldırıyoruz çünkü artık doğrudan mock_process_image kullanıyoruz
    # @pytest.fixture(autouse=True)
    # def setup_model_patch(self, mock_model_import):
    #     """Her testten önce otomatik olarak model modülünü mockla"""
    #     pass
    
    @pytest.fixture
    def mock_db_session(self):
        """Veritabanı oturumunu mocklar"""
        with patch("app.database.SessionLocal") as mock_session:
            # SessionLocal sınıfından dönen mock session nesnesi
            mock_db = MagicMock()
            mock_session.return_value = mock_db
            
            # Oturum contextini simüle et
            mock_db.__enter__.return_value = mock_db
            mock_db.__exit__.return_value = None
            
            yield mock_db
    
    @pytest.fixture
    def mock_vehicle_entry(self):
        """Araç girişi işlemlerini mocklar"""
        # Bu fixture ile crud modülündeki get_vehicle_by_license_plate ve create_vehicle fonksiyonlarını mockla
        with patch("app.main.get_vehicle_by_license_plate") as mock_get_vehicle, \
             patch("app.main.create_vehicle") as mock_create_vehicle, \
             patch("app.main.get_active_parking_record_by_vehicle") as mock_get_record, \
             patch("app.main.create_parking_record") as mock_create_record:
            
            # Araç bulunamadı
            mock_get_vehicle.return_value = None
            
            # Mock araç
            mock_vehicle = MagicMock()
            mock_vehicle.id = 1
            mock_vehicle.license_plate = TEST_PLATE
            mock_create_vehicle.return_value = mock_vehicle
            
            # Aktif park kaydı yok
            mock_get_record.return_value = None
            
            # Mock park kaydı
            mock_record = MagicMock()
            mock_record.id = 1
            mock_record.vehicle_id = 1
            mock_record.entry_time = datetime.now()
            mock_record.is_active = True
            mock_create_record.return_value = mock_record
            
            yield (mock_get_vehicle, mock_create_vehicle, mock_get_record, mock_create_record)
    
    @pytest.fixture
    def mock_vehicle_exit(self):
        """Araç çıkışı işlemlerini mocklar"""
        # Bu fixture ile crud modülündeki get_vehicle_by_license_plate ve close_parking_record fonksiyonlarını mockla
        with patch("app.main.get_vehicle_by_license_plate") as mock_get_vehicle, \
             patch("app.main.get_active_parking_record_by_vehicle") as mock_get_record, \
             patch("app.main.close_parking_record") as mock_close_record:
            
            # Mock araç
            mock_vehicle = MagicMock()
            mock_vehicle.id = 1
            mock_vehicle.license_plate = TEST_PLATE
            mock_get_vehicle.return_value = mock_vehicle
            
            # Mock aktif park kaydı
            mock_record = MagicMock()
            mock_record.id = 1
            mock_record.vehicle_id = 1
            mock_record.entry_time = datetime.now()
            mock_record.is_active = True
            mock_get_record.return_value = mock_record
            
            # Mock kapanan park kaydı
            mock_closed_record = MagicMock()
            mock_closed_record.id = 1
            mock_closed_record.vehicle_id = 1
            mock_closed_record.entry_time = datetime(2023, 1, 1, 10, 0, 0)
            mock_closed_record.exit_time = datetime(2023, 1, 1, 12, 0, 0)
            mock_closed_record.is_active = False
            mock_closed_record.parking_fee = 2000  # 20 TL
            mock_close_record.return_value = mock_closed_record
            
            yield (mock_get_vehicle, mock_get_record, mock_close_record)
    
    @pytest.fixture
    def mock_websocket_manager(self):
        """WebSocket yöneticisini mocklar"""
        with patch("app.main.run_async") as mock_run_async, \
             patch("app.main.manager.send_parking_record_update") as mock_send_update:
            # Asenkron run_async fonksiyonu
            mock_run_async.return_value = None
            # WebSocket gönderimi
            mock_send_update.return_value = None
            
            yield (mock_run_async, mock_send_update)
    
    def test_process_plate_entry_success(self, client, test_image, mock_process_image, mock_vehicle_entry, mock_websocket_manager):
        """Başarılı araç girişi plaka işleme testi"""
        mock_get_vehicle, mock_create_vehicle, mock_get_record, mock_create_record = mock_vehicle_entry
        
        # Çoklu parça dosya yükleme için hazırlanmış istek
        files = {"file": ("test.png", test_image, "image/png")}
        
        # API isteği gönder
        response = client.post("/process-plate-entry", files=files)
        
        # Yanıt başarılı olmalı
        assert response.status_code == 200
        
        # Yanıt içeriği doğru olmalı
        data = response.json()
        assert data["success"] == True
        assert "entry_time" in data
        assert data["vehicle"]["license_plate"] == TEST_PLATE
        assert data["parking_record_id"] == 1
        
        # Mock çağrılarının yapıldığını kontrol et - spesifik parametreleri kontrol etmiyoruz
        mock_process_image.assert_called_once()
        assert mock_get_vehicle.call_count > 0
        assert mock_create_vehicle.call_count > 0
        assert mock_get_record.call_count > 0
        assert mock_create_record.call_count > 0
    
    def test_process_plate_entry_no_plate_found(self, client, test_image, mock_process_image):
        """Plaka bulunamadığında girişi plaka işleme testi"""
        # Plaka tanıma işlemini mockla - plaka bulunamadı
        mock_process_image.return_value = {"license_plates": [], "processing_time": 0.156}
        
        # Çoklu parça dosya yükleme için hazırlanmış istek
        files = {"file": ("test.png", test_image, "image/png")}
        
        # API isteği gönder
        response = client.post("/process-plate-entry", files=files)
        
        # Yanıt başarısız olmalı
        assert response.status_code == 200
        
        # Yanıt içeriği doğru olmalı
        data = response.json()
        assert data["success"] == False
        assert "Görüntüde plaka bulunamadı" in data["message"]
        
        # Mock çağrısı doğru olmalı
        mock_process_image.assert_called_once()
    
    def test_process_plate_entry_error(self, client, test_image, mock_process_image):
        """Plaka tanıma hata verdiğinde girişi plaka işleme testi"""
        # Plaka tanıma işlemini mockla - hata döndür
        mock_process_image.return_value = {"error": "Görüntü işleme hatası"}
        
        # Çoklu parça dosya yükleme için hazırlanmış istek
        files = {"file": ("test.png", test_image, "image/png")}
        
        # API isteği gönder
        response = client.post("/process-plate-entry", files=files)
        
        # Yanıt başarısız olmalı
        assert response.status_code == 200
        
        # Yanıt içeriği doğru olmalı
        data = response.json()
        assert data["success"] == False
        assert "Plaka tanıma sırasında hata" in data["message"]
        
        # Mock çağrısı doğru olmalı
        mock_process_image.assert_called_once()
    
    def test_process_plate_entry_vehicle_already_parked(self, client, test_image, mock_process_image, mock_db_session):
        """Zaten park edilmiş araç için girişi plaka işleme testi"""
        # Plaka tanıma işlemini mockla
        mock_process_image.return_value = {
            "license_plates": [TEST_PLATE],
            "confidence": CONFIDENCE_SCORE,
            "processing_time": 0.156
        }
        
        # Mock araç
        mock_vehicle = MagicMock()
        mock_vehicle.id = 1
        mock_vehicle.license_plate = TEST_PLATE
        
        # Mock park kaydı - aktif
        mock_record = MagicMock()
        mock_record.id = 1
        mock_record.vehicle_id = 1
        mock_record.entry_time = datetime.now()
        mock_record.is_active = True
        
        # Mock arama sonuçlarını ayarla
        with patch("app.main.get_vehicle_by_license_plate") as mock_get_vehicle, \
             patch("app.main.get_active_parking_record_by_vehicle") as mock_get_record:
            
            # Araç bulundu
            mock_get_vehicle.return_value = mock_vehicle
            
            # Aktif park kaydı var
            mock_get_record.return_value = mock_record
            
            # Çoklu parça dosya yükleme için hazırlanmış istek
            files = {"file": ("test.png", test_image, "image/png")}
            
            # API isteği gönder
            response = client.post("/process-plate-entry", files=files)
            
            # Yanıt başarısız olmalı
            assert response.status_code == 200
            
            # Yanıt içeriği doğru olmalı
            data = response.json()
            assert data["success"] == False
            assert "zaten otoparkta park halinde" in data["message"]
            
            # Mock çağrıları doğru olmalı
            mock_process_image.assert_called_once()
            assert mock_get_vehicle.call_count > 0
            assert mock_get_record.call_count > 0
    
    def test_process_plate_exit_success(self, client, test_image, mock_process_image, mock_vehicle_exit, mock_websocket_manager):
        """Başarılı araç çıkışı plaka işleme testi"""
        mock_get_vehicle, mock_get_record, mock_close_record = mock_vehicle_exit
        
        # Çoklu parça dosya yükleme için hazırlanmış istek
        files = {"file": ("test.png", test_image, "image/png")}
        
        # API isteği gönder
        response = client.post("/process-plate-exit", files=files)
        
        # Yanıt başarılı olmalı
        assert response.status_code == 200
        
        # Yanıt içeriği doğru olmalı
        data = response.json()
        assert data["success"] == True
        assert "exit_time" in data
        assert "entry_time" in data
        assert "duration_hours" in data
        assert "parking_fee" in data
        
        # Mock çağrıları doğru olmalı
        mock_process_image.assert_called_once()
        assert mock_get_vehicle.call_count > 0
        assert mock_get_record.call_count > 0
        assert mock_close_record.call_count > 0
    
    def test_process_plate_exit_no_plate_found(self, client, test_image, mock_process_image):
        """Plaka bulunamadığında çıkışı plaka işleme testi"""
        # Plaka tanıma işlemini mockla - plaka bulunamadı
        mock_process_image.return_value = {"license_plates": [], "processing_time": 0.156}
        
        # Çoklu parça dosya yükleme için hazırlanmış istek
        files = {"file": ("test.png", test_image, "image/png")}
        
        # API isteği gönder
        response = client.post("/process-plate-exit", files=files)
        
        # Yanıt başarısız olmalı
        assert response.status_code == 200
        
        # Yanıt içeriği doğru olmalı
        data = response.json()
        assert data["success"] == False
        assert "Görüntüde plaka bulunamadı" in data["message"]
        
        # Mock çağrısı doğru olmalı
        mock_process_image.assert_called_once()
    
    def test_process_plate_exit_no_vehicle(self, client, test_image, mock_process_image, mock_db_session):
        """Veritabanında olmayan araç için çıkışı plaka işleme testi"""
        # Plaka tanıma işlemini mockla
        mock_process_image.return_value = {
            "license_plates": [TEST_PLATE],
            "confidence": CONFIDENCE_SCORE,
            "processing_time": 0.156
        }
        
        # Araç bulunamadı mockla
        with patch("app.main.get_vehicle_by_license_plate") as mock_get_vehicle:
            # Araç bulunamadı
            mock_get_vehicle.return_value = None
            
            # Çoklu parça dosya yükleme için hazırlanmış istek
            files = {"file": ("test.png", test_image, "image/png")}
            
            # API isteği gönder
            response = client.post("/process-plate-exit", files=files)
            
            # Yanıt başarısız olmalı
            assert response.status_code == 200
            
            # Yanıt içeriği doğru olmalı
            data = response.json()
            assert data["success"] == False
            assert "araç kaydı bulunamadı" in data["message"]
            
            # Mock çağrıları doğru olmalı
            mock_process_image.assert_called_once()
            assert mock_get_vehicle.call_count > 0
    
    def test_process_plate_exit_no_active_record(self, client, test_image, mock_process_image, mock_db_session):
        """Aktif park kaydı olmayan araç için çıkışı plaka işleme testi"""
        # Plaka tanıma işlemini mockla
        mock_process_image.return_value = {
            "license_plates": [TEST_PLATE],
            "confidence": CONFIDENCE_SCORE,
            "processing_time": 0.156
        }
        
        # Mock araç
        mock_vehicle = MagicMock()
        mock_vehicle.id = 1
        mock_vehicle.license_plate = TEST_PLATE
        
        # Mock olarak araç bulundu ama aktif park kaydı yok
        with patch("app.main.get_vehicle_by_license_plate") as mock_get_vehicle, \
             patch("app.main.get_active_parking_record_by_vehicle") as mock_get_record:
            
            # Araç bulundu
            mock_get_vehicle.return_value = mock_vehicle
            
            # Aktif park kaydı yok
            mock_get_record.return_value = None
            
            # Çoklu parça dosya yükleme için hazırlanmış istek
            files = {"file": ("test.png", test_image, "image/png")}
            
            # API isteği gönder
            response = client.post("/process-plate-exit", files=files)
            
            # Yanıt başarısız olmalı
            assert response.status_code == 200
            
            # Yanıt içeriği doğru olmalı
            data = response.json()
            assert data["success"] == False
            assert "aktif park kaydı bulunamadı" in data["message"]
            
            # Mock çağrıları doğru olmalı
            mock_process_image.assert_called_once()
            assert mock_get_vehicle.call_count > 0
            assert mock_get_record.call_count > 0
    
    def test_process_plate_exit_error(self, client, test_image, mock_process_image):
        """Plaka tanıma hata verdiğinde çıkışı plaka işleme testi"""
        # Plaka tanıma işlemini mockla - hata döndür
        mock_process_image.return_value = {"error": "Görüntü işleme hatası"}
        
        # Çoklu parça dosya yükleme için hazırlanmış istek
        files = {"file": ("test.png", test_image, "image/png")}
        
        # API isteği gönder
        response = client.post("/process-plate-exit", files=files)
        
        # Yanıt başarısız olmalı
        assert response.status_code == 200
        
        # Yanıt içeriği doğru olmalı
        data = response.json()
        assert data["success"] == False
        assert "Plaka tanıma sırasında hata" in data["message"]
        
        # Mock çağrısı doğru olmalı
        mock_process_image.assert_called_once() 