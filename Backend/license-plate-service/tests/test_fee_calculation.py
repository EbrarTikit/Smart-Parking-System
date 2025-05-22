import pytest
from datetime import datetime, timedelta
from unittest.mock import patch, MagicMock
import requests
import logging
import sys
import os

# Proje kök dizinini sys.path'e ekle
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Test edilecek fonksiyonları içe aktar
from app.crud import calculate_parking_fee

# Test sabitlerini ayarla
MOCK_PARKING_ID = 1
HOURLY_RATE = 10.0  # 10 TL/saat
ONE_HOUR = timedelta(hours=1)
TWO_HOURS = timedelta(hours=2)
HALF_HOUR = timedelta(minutes=30)

class TestFeeCalculation:
    """calculate_parking_fee fonksiyonu için test suite"""
    
    @pytest.fixture
    def mock_parking_service(self):
        """Parking Management Service'i mocklar"""
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
    
    def test_calculate_fee_one_hour(self, mock_parking_service):
        """Tam bir saat için ücret hesaplama testi"""
        # Giriş ve çıkış zamanlarını ayarla (1 saat fark)
        entry_time = datetime(2023, 1, 1, 10, 0, 0)
        exit_time = entry_time + ONE_HOUR
        
        # Ücret hesapla (10 TL/saat * 1 saat = 10 TL = 1000 kuruş)
        fee = calculate_parking_fee(entry_time, exit_time, MOCK_PARKING_ID)
        
        # Sonuç 1000 kuruş (10 TL) olmalı
        assert fee == 1000
        
        # API çağrısı doğru URL ile yapılmış olmalı
        mock_parking_service.assert_called_once()
        # API'nin son kısmı doğru olmalı
        assert f"/api/parkings/{MOCK_PARKING_ID}" in mock_parking_service.call_args[0][0]
    
    def test_calculate_fee_partial_hour(self, mock_parking_service):
        """Kısmi süre için ücret hesaplama testi"""
        # Giriş ve çıkış zamanlarını ayarla (30 dakika fark)
        entry_time = datetime(2023, 1, 1, 10, 0, 0)
        exit_time = entry_time + HALF_HOUR
        
        # Ücret hesapla (10 TL/saat * 0.5 saat = 5 TL)
        # Ancak minimum 1 saatlik ücret uygulanır
        fee = calculate_parking_fee(entry_time, exit_time, MOCK_PARKING_ID)
        
        # Minimum ücret 10 TL (1000 kuruş) olmalı
        assert fee == 1000
    
    def test_calculate_fee_two_hours(self, mock_parking_service):
        """İki saat için ücret hesaplama testi"""
        # Giriş ve çıkış zamanlarını ayarla (2 saat fark)
        entry_time = datetime(2023, 1, 1, 10, 0, 0)
        exit_time = entry_time + TWO_HOURS
        
        # Ücret hesapla (10 TL/saat * 2 saat = 20 TL = 2000 kuruş)
        fee = calculate_parking_fee(entry_time, exit_time, MOCK_PARKING_ID)
        
        # Sonuç 2000 kuruş (20 TL) olmalı
        assert fee == 2000
    
    def test_calculate_fee_one_hour_thirty_minutes(self, mock_parking_service):
        """1 saat 30 dakika için ücret hesaplama testi"""
        # Giriş ve çıkış zamanlarını ayarla (1.5 saat fark)
        entry_time = datetime(2023, 1, 1, 10, 0, 0)
        exit_time = entry_time + ONE_HOUR + HALF_HOUR
        
        # Ücret hesapla (10 TL/saat * 1.5 saat = 15 TL = 1500 kuruş)
        fee = calculate_parking_fee(entry_time, exit_time, MOCK_PARKING_ID)
        
        # Sonuç 1500 kuruş (15 TL) olmalı
        assert fee == 1500
    
    def test_different_timezone_handling(self, mock_parking_service):
        """Farklı timezone'lar ile ücret hesaplama testi"""
        # Giriş zamanını UTC olarak ayarla
        import pytz
        entry_time = datetime(2023, 1, 1, 10, 0, 0, tzinfo=pytz.UTC)
        # Çıkış zamanını timezone'suz ayarla (1 saat sonra)
        exit_time = datetime(2023, 1, 1, 11, 0, 0)
        
        # Ücret hesapla (fonksiyon timezone farkını düzeltmeli)
        fee = calculate_parking_fee(entry_time, exit_time, MOCK_PARKING_ID)
        
        # Sonuç 1000 kuruş (10 TL) olmalı
        assert fee == 1000
    
    def test_api_error_handling(self):
        """API hatası durumunda ücret hesaplama testi"""
        # Requests.get çağrısını mockla
        with patch("requests.get") as mock_get:
            # API hatası simüle et
            mock_get.side_effect = requests.exceptions.RequestException("Bağlantı hatası")
            
            # Giriş ve çıkış zamanlarını ayarla
            entry_time = datetime(2023, 1, 1, 10, 0, 0)
            exit_time = entry_time + ONE_HOUR
            
            # Fonksiyon ValueError fırlatmalı
            with pytest.raises(ValueError) as e:
                calculate_parking_fee(entry_time, exit_time, MOCK_PARKING_ID)
            
            # Hata mesajı doğru olmalı
            assert "ücret bilgisi alınamadı" in str(e.value).lower()
    
    def test_api_returns_invalid_data(self):
        """API'nin geçersiz veri döndürdüğü durumda ücret hesaplama testi"""
        # Requests.get çağrısını mockla
        with patch("requests.get") as mock_get:
            # Mock yanıt nesnesi
            mock_response = MagicMock()
            mock_response.status_code = 200
            # rate alanı yok
            mock_response.json.return_value = {
                "id": MOCK_PARKING_ID,
                "name": "Test Otoparkı"
            }
            mock_get.return_value = mock_response
            
            # Giriş ve çıkış zamanlarını ayarla
            entry_time = datetime(2023, 1, 1, 10, 0, 0)
            exit_time = entry_time + ONE_HOUR
            
            # Fonksiyon ValueError fırlatmalı
            with pytest.raises(ValueError) as e:
                calculate_parking_fee(entry_time, exit_time, MOCK_PARKING_ID)
            
            # Hata mesajı doğru olmalı
            assert "ücret bilgisi alınamadı" in str(e.value).lower()
    
    def test_api_returns_null_rate(self):
        """API'nin null rate döndürdüğü durumda ücret hesaplama testi"""
        # Requests.get çağrısını mockla
        with patch("requests.get") as mock_get:
            # Mock yanıt nesnesi
            mock_response = MagicMock()
            mock_response.status_code = 200
            # rate null
            mock_response.json.return_value = {
                "id": MOCK_PARKING_ID,
                "name": "Test Otoparkı",
                "rate": None
            }
            mock_get.return_value = mock_response
            
            # Giriş ve çıkış zamanlarını ayarla
            entry_time = datetime(2023, 1, 1, 10, 0, 0)
            exit_time = entry_time + ONE_HOUR
            
            # Fonksiyon ValueError fırlatmalı
            with pytest.raises(ValueError) as e:
                calculate_parking_fee(entry_time, exit_time, MOCK_PARKING_ID)
            
            # Hata mesajı doğru olmalı
            assert "ücret bilgisi alınamadı" in str(e.value).lower()
    
    def test_api_returns_invalid_status_code(self):
        """API'nin geçersiz durum kodu döndürdüğü durumda ücret hesaplama testi"""
        # Requests.get çağrısını mockla
        with patch("requests.get") as mock_get:
            # Mock yanıt nesnesi
            mock_response = MagicMock()
            mock_response.status_code = 404  # Not Found
            mock_get.return_value = mock_response
            
            # Giriş ve çıkış zamanlarını ayarla
            entry_time = datetime(2023, 1, 1, 10, 0, 0)
            exit_time = entry_time + ONE_HOUR
            
            # Fonksiyon ValueError fırlatmalı
            with pytest.raises(ValueError) as e:
                calculate_parking_fee(entry_time, exit_time, MOCK_PARKING_ID)
            
            # Hata mesajı doğru olmalı
            assert "ücret bilgisi alınamadı" in str(e.value).lower()
    
    def test_different_parking_ids(self):
        """Farklı otopark ID'leri için ücret hesaplama testi"""
        # İki farklı otopark ID'si için mock'lama yap
        parking_ids = [1, 2]
        rates = [10.0, 15.0]  # Farklı ücretler
        
        for idx, parking_id in enumerate(parking_ids):
            # Requests.get çağrısını mockla
            with patch("requests.get") as mock_get:
                # Mock yanıt nesnesi
                mock_response = MagicMock()
                mock_response.status_code = 200
                mock_response.json.return_value = {
                    "id": parking_id,
                    "name": f"Test Otoparkı {parking_id}",
                    "rate": rates[idx]
                }
                mock_get.return_value = mock_response
                
                # Giriş ve çıkış zamanlarını ayarla (1 saat fark)
                entry_time = datetime(2023, 1, 1, 10, 0, 0)
                exit_time = entry_time + ONE_HOUR
                
                # Ücret hesapla
                fee = calculate_parking_fee(entry_time, exit_time, parking_id)
                
                # Sonuç beklenen ücret olmalı
                expected_fee = int(rates[idx] * 100)  # TL -> kuruş
                assert fee == expected_fee
                
                # API çağrısı doğru URL ile yapılmış olmalı
                mock_get.assert_called_once()
                assert f"/api/parkings/{parking_id}" in mock_get.call_args[0][0] 