"""
Parking rate entegrasyonu test scripti.
Bu script, License Plate Service'in Parking Management Service'ten ücret bilgilerini
doğru şekilde alıp almadığını test eder.

Çalıştırmadan önce her iki servisin de aktif olduğundan emin olun.
"""

import requests
import time
import sys
import json
import logging
import os

# Logging konfigürasyonu
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Test konfigürasyon ayarları - Docker içinde çalışırken servis isimleriyle erişim sağla
# Docker içinde mi yoksa doğrudan mı çalıştığını belirle
is_docker = os.path.exists('/.dockerenv')

# Docker içinde veya dışında olma durumuna göre URL'leri ayarla
if is_docker:
    # Docker içinde çalışırken servis isimlerini kullan
    LICENSE_PLATE_SERVICE_URL = "http://license-plate-service:8000"
    PARKING_MANAGEMENT_URL = "http://parking-management-service:8081"
    logger.info("Docker içinde çalışıyor - Servis isimleri kullanılıyor")
else:
    # Doğrudan makinede çalışırken localhost kullan
    LICENSE_PLATE_SERVICE_URL = "http://localhost:8005"
    PARKING_MANAGEMENT_URL = "http://localhost:8081"
    logger.info("Doğrudan makinede çalışıyor - localhost kullanılıyor")

TEST_PLATE = "34TEST99"  # Test için kullanılacak plaka

def print_header(message):
    """Başlık formatında mesaj yazdır"""
    print("\n" + "="*80)
    print(f" {message}")
    print("="*80)

def make_request(url, method="GET", data=None, headers=None):
    """HTTP isteği gönder ve sonucu yazdır"""
    try:
        if method.upper() == "GET":
            response = requests.get(url, headers=headers)
        elif method.upper() == "POST":
            response = requests.post(url, json=data, headers=headers)
        else:
            print(f"Desteklenmeyen HTTP methodu: {method}")
            return None
        
        print(f"İstek: {method} {url}")
        if data:
            print(f"Gönderilen veri: {json.dumps(data, ensure_ascii=False, indent=2)}")
        
        print(f"Yanıt kodu: {response.status_code}")
        
        try:
            json_response = response.json()
            print(f"Yanıt: {json.dumps(json_response, ensure_ascii=False, indent=2)}")
            return json_response
        except:
            print(f"Yanıt (metin): {response.text}")
            return response.text
            
    except Exception as e:
        print(f"Hata: {str(e)}")
        return None

def test_parking_rates():
    """Farklı otopark oranlarıyla test et"""
    print_header("OTOPARK ÜCRETLERİ TESTİ BAŞLATILIYOR")
    
    # 1. Tüm otoparkları listele
    print_header("ADIM 1: TÜM OTOPARKLARI LİSTELE")
    parkings = make_request(f"{PARKING_MANAGEMENT_URL}/api/parkings")
    
    if not parkings or not isinstance(parkings, list) or len(parkings) == 0:
        print("Otopark bulunamadı veya Parking Management Service çalışmıyor!")
        return False
    
    # Detaylı otopark bilgilerini logla
    logger.info(f"Toplam {len(parkings)} otopark bulundu")
    for p in parkings:
        logger.info(f"Otopark ID={p['id']}, Ad={p['name']}, Ücret={p['rate']} TL/saat")
    
    # Kullanılacak otopark ID'lerini belirle
    parking_ids = [p["id"] for p in parkings[:min(2, len(parkings))]]
    
    # Test sonuçlarını saklamak için dict
    test_results = {}
    
    # 2. Araç girişi
    print_header(f"ADIM 2: ARAÇ GİRİŞİ YAP ({TEST_PLATE})")
    entry_data = {
        "license_plate": TEST_PLATE
    }
    entry_response = make_request(f"{LICENSE_PLATE_SERVICE_URL}/vehicle/entry", method="POST", data=entry_data)
    
    if not entry_response or not entry_response.get("success"):
        print("Araç girişi başarısız! Test durduruluyor.")
        return False
    
    print(f"Araç başarıyla giriş yaptı: {TEST_PLATE}")
    
    # Bir saniye bekleyelim ki asgari bir süre geçsin
    print("Kısa bekleyiş yapılıyor... (1 saniye)")
    time.sleep(1)
    
    # 3. Farklı otopark ID'leri için çıkış testi
    for idx, parking_id in enumerate(parking_ids):
        print_header(f"ADIM {3+idx}: OTOPARK ID={parking_id} İÇİN ÇIKIŞ TESTİ")
        
        # Otopark bilgilerini göster
        parking_info = next((p for p in parkings if p["id"] == parking_id), None)
        if parking_info:
            expected_rate = parking_info['rate']
            print(f"Otopark Adı: {parking_info['name']}")
            print(f"Saatlik Ücret: {expected_rate} TL")
            logger.info(f"Otopark: ID={parking_id}, Ad={parking_info['name']}, Ücret={expected_rate} TL/saat")
        else:
            expected_rate = None
            print(f"Otopark ID={parking_id} için bilgi bulunamadı!")
        
        # Araç çıkışı yap
        exit_data = {
            "license_plate": TEST_PLATE,
            "parking_id": parking_id
        }
        exit_response = make_request(f"{LICENSE_PLATE_SERVICE_URL}/vehicle/exit", method="POST", data=exit_data)
        
        if not exit_response or not exit_response.get("success"):
            print(f"Otopark ID={parking_id} için çıkış başarısız!")
            test_results[parking_id] = {"success": False}
            continue
        
        print(f"Çıkış Sonuçları:")
        duration = exit_response.get('duration_hours', 0)
        actual_fee = exit_response.get('parking_fee', 0)
        print(f"Park Süresi: {duration} saat")
        print(f"Ücret: {actual_fee} TL")
        logger.info(f"Hesaplanan: Süre={duration} saat, Ücret={actual_fee} TL")
        
        # Ücret doğru mu kontrolü
        if expected_rate is not None:
            # Minimum 1 saatlik ücret kontrolü
            expected_min_fee = expected_rate
            
            if abs(actual_fee - expected_min_fee) < 0.01:  # Küçük virgül sonrası farklar için tolerans
                print(f"✅ ÜCRET DOĞRU: Beklenen minimum ({expected_min_fee} TL) = Gerçek ({actual_fee} TL)")
                test_results[parking_id] = {"success": True, "rate_correct": True}
            else:
                print(f"❌ ÜCRET HATALI: Beklenen minimum ({expected_min_fee} TL) ≠ Gerçek ({actual_fee} TL)")
                test_results[parking_id] = {"success": True, "rate_correct": False}
                logger.error(f"ÜCRET HATASI: Beklenen={expected_min_fee} TL, Gerçek={actual_fee} TL, Fark={expected_min_fee-actual_fee} TL")
        
        # Eğer bu son test değilse, tekrar araç girişi yap
        if idx < len(parking_ids) - 1:
            print_header(f"ADIM {3+idx}.1: YENİDEN ARAÇ GİRİŞİ YAP")
            make_request(f"{LICENSE_PLATE_SERVICE_URL}/vehicle/entry", method="POST", data=entry_data)
            print("Kısa bekleyiş yapılıyor... (1 saniye)")
            time.sleep(1)

    # Test sonuçlarını değerlendir
    print_header("TEST SONUÇLARI")
    all_rates_correct = True
    
    for parking_id, result in test_results.items():
        parking_info = next((p for p in parkings if p["id"] == parking_id), None)
        
        if not result.get("success"):
            print(f"❌ Otopark ID={parking_id} için çıkış işlemi başarısız!")
            all_rates_correct = False
        elif not result.get("rate_correct", True):
            print(f"❌ Otopark ID={parking_id} ({parking_info['name']}) için ücret hesaplaması hatalı!")
            all_rates_correct = False
        else:
            print(f"✅ Otopark ID={parking_id} ({parking_info['name']}) için ücret hesaplaması doğru!")
    
    if all_rates_correct:
        print("\n✅ Tüm otoparklar için ücret hesaplaması doğru çalışıyor!")
    else:
        print("\n❌ En az bir otopark için ücret hesaplaması hatalı!")
    
    print_header("TEST TAMAMLANDI")
    return all_rates_correct

if __name__ == "__main__":
    try:
        success = test_parking_rates()
        if success:
            print("\nTest başarıyla tamamlandı!")
        else:
            print("\nTest başarısız!")
    except Exception as e:
        print(f"\nTest sırasında beklenmeyen hata: {str(e)}")
        sys.exit(1) 