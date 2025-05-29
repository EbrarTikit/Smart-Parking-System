from sqlalchemy.orm import Session
from sqlalchemy import and_
from datetime import datetime
from typing import List, Optional
import requests
import os
from . import models, schemas
import logging

# Vehicle CRUD operations
def get_vehicle(db: Session, vehicle_id: int):
    return db.query(models.Vehicle).filter(models.Vehicle.id == vehicle_id).first()

def get_vehicle_by_license_plate(db: Session, license_plate: str):
    return db.query(models.Vehicle).filter(models.Vehicle.license_plate == license_plate).first()

def get_vehicles(db: Session, skip: int = 0, limit: int = 100):
    return db.query(models.Vehicle).offset(skip).limit(limit).all()

def create_vehicle(db: Session, vehicle: schemas.VehicleCreate):
    db_vehicle = models.Vehicle(**vehicle.dict())
    db.add(db_vehicle)
    db.commit()
    db.refresh(db_vehicle)
    return db_vehicle

def update_vehicle(db: Session, vehicle_id: int, vehicle: schemas.VehicleCreate):
    db_vehicle = get_vehicle(db, vehicle_id)
    if db_vehicle:
        for key, value in vehicle.dict().items():
            setattr(db_vehicle, key, value)
        db.commit()
        db.refresh(db_vehicle)
    return db_vehicle

def delete_vehicle(db: Session, vehicle_id: int):
    db_vehicle = get_vehicle(db, vehicle_id)
    if db_vehicle:
        db.delete(db_vehicle)
        db.commit()
        return True
    return False

# Parking Record CRUD operations
def get_parking_record(db: Session, record_id: int):
    return db.query(models.ParkingRecord).filter(models.ParkingRecord.id == record_id).first()

def get_active_parking_record_by_vehicle(db: Session, vehicle_id: int, parking_id: int = None):
    """
    Aracın aktif park kaydını getirir.
    
    Args:
        db: Veritabanı oturumu
        vehicle_id: Araç ID'si
        parking_id: Otopark ID'si (belirtilirse sadece bu otoparkta kontrol yapar)
        
    Returns:
        Aktif park kaydı veya None
    """
    query = db.query(models.ParkingRecord).filter(
        and_(
            models.ParkingRecord.vehicle_id == vehicle_id,
            models.ParkingRecord.is_active == True
        )
    )
    
    # Eğer parking_id belirtilmişse, sadece o otoparkta kontrol et
    if parking_id is not None:
        query = query.filter(models.ParkingRecord.parking_id == parking_id)
        
    return query.first()

def get_parking_records(db: Session, skip: int = 0, limit: int = 100):
    return db.query(models.ParkingRecord).offset(skip).limit(limit).all()

def create_parking_record(db: Session, parking_record: schemas.ParkingRecordCreate):
    db_record = models.ParkingRecord(**parking_record.dict())
    db.add(db_record)
    db.commit()
    db.refresh(db_record)
    return db_record

def update_parking_record(db: Session, record_id: int, parking_record: schemas.ParkingRecordUpdate):
    db_record = get_parking_record(db, record_id)
    if db_record:
        for key, value in parking_record.dict().items():
            setattr(db_record, key, value)
        db_record.is_active = False  # Çıkış yapıldığında aktif değil
        db.commit()
        db.refresh(db_record)
    return db_record

def calculate_parking_fee(entry_time: datetime, exit_time: datetime, parking_id: int = 1) -> int:
    """
    Park süresine göre ücret hesaplar (kuruş cinsinden)
    
    Parking Management Service'ten ilgili otoparkın ücret bilgisini çeker.
    
    Args:
        entry_time: Giriş zamanı
        exit_time: Çıkış zamanı
        parking_id: Otopark ID'si
        
    Returns:
        Hesaplanan ücret (kuruş cinsinden, int)
    """
    logger = logging.getLogger(__name__)
    
    logger.info(f"=== ÜCRET HESAPLAMA BAŞLADI - Otopark ID: {parking_id} ===")
    logger.info(f"Giriş zamanı: {entry_time}, Çıkış zamanı: {exit_time}")
    
    # Timezone uyumsuzluğunu kontrol et ve düzelt
    if entry_time.tzinfo != exit_time.tzinfo:
        # Eğer biri timezone bilgisi içeriyorsa, diğeri içermiyorsa
        if entry_time.tzinfo is None and exit_time.tzinfo is not None:
            import pytz
            entry_time = pytz.UTC.localize(entry_time)
            logger.info(f"Giriş zamanı timezone'a lokalize edildi: {entry_time}")
        elif entry_time.tzinfo is not None and exit_time.tzinfo is None:
            import pytz
            exit_time = pytz.UTC.localize(exit_time)
            logger.info(f"Çıkış zamanı timezone'a lokalize edildi: {exit_time}")
    
    # API ile ücret bilgisini çek
    logger.info(f"Otopark (ID={parking_id}) için API üzerinden ücret bilgisi çekiliyor...")
    
    # Parking Management Service'ten ücret bilgisini çek
    parking_service_url = os.getenv("PARKING_MANAGEMENT_SERVICE_URL", "http://parking-management-service:8081")
    
    # Docker compose için kontrol etmeye gerek yok, varsayılan olarak doğru URL'yi kullanacak
    logger.info(f"Kullanılacak otopark servisi URL: {parking_service_url}")
    
    # Saatlik ücret bilgisini al (TL/saat)
    hourly_rate = None
    
    try:
        response = requests.get(f"{parking_service_url}/api/parkings/{parking_id}")
        logger.info(f"API yanıtı - Status: {response.status_code}")
        
        if response.status_code == 200:
            parking_data = response.json()
            
            if "rate" in parking_data and parking_data["rate"] is not None:
                try:
                    hourly_rate = float(parking_data["rate"])  # String olarak gelirse float'a çevir
                    logger.info(f"API'den alınan ücret: {hourly_rate} TL/saat")
                except (ValueError, TypeError) as e:
                    logger.error(f"Rate değeri float'a çevrilemedi: {str(e)}")
                    hourly_rate = None
            else:
                logger.warning(f"API yanıtında 'rate' alanı bulunamadı/null.")
        else:
            # Servis erişilemiyorsa hata ver
            logger.error(f"Otopark servisi erişilemedi. Status code: {response.status_code}")
    except Exception as e:
        # Hata durumda bildir
        logger.error(f"Parking service error: {str(e)}")
    
    # Eğer API'den ücret alınamadıysa, hata ver
    if hourly_rate is None:
        error_msg = f"Otopark ID={parking_id} için ücret bilgisi alınamadı! API'yi kontrol edin."
        logger.error(error_msg)
        raise ValueError(error_msg)
    
    # Otopark süresini hesapla
    duration = exit_time - entry_time
    hours = duration.total_seconds() / 3600
    logger.info(f"Park süresi: {hours:.4f} saat")
    
    # Hesaplama (TL -> kuruş)
    raw_fee = hours * hourly_rate * 100
    fee = int(raw_fee)
    logger.info(f"Ham ücret: {hours:.4f} saat * {hourly_rate:.2f} TL/saat * 100 = {fee} kuruş")
    
    # Minimum ücret 1 saatlik ücretten az olamaz
    min_fee = int(hourly_rate * 100)
    logger.info(f"Minimum ücret: {hourly_rate:.2f} TL/saat * 100 = {min_fee} kuruş")
    
    # Final ücreti hesapla
    final_fee = max(fee, min_fee)
    logger.info(f"Final ücret: max({fee}, {min_fee}) = {final_fee} kuruş")
    
    # Sonuç
    logger.info(f"Döndürülen ücret: {final_fee} kuruş ({final_fee/100:.2f} TL)")
    return final_fee

def close_parking_record(db: Session, record_id: int, parking_id: int = 1):
    """
    Park kaydını kapatır ve park ücretini hesaplar.
    
    Args:
        db: Database session
        record_id: Kapatılacak park kaydı ID'si
        parking_id: Otopark ID'si (ücret hesaplama için)
        
    Returns:
        Kapatılan park kaydı veya None (hata durumunda)
    """
    logger = logging.getLogger(__name__)
    
    logger.info(f"===> Park kaydı kapama başlatılıyor. Kayıt ID: {record_id}, Otopark ID: {parking_id}")
    
    # Kaydı al
    db_record = get_parking_record(db, record_id)
    
    if not db_record:
        logger.error(f"Kayıt bulunamadı! ID: {record_id}")
        return None
        
    if not db_record.is_active:
        logger.warning(f"Kayıt zaten kapatılmış! ID: {record_id}")
        return db_record
    
    try:
        # Timezone bilgisi içermeyen bir datetime nesnesi oluştur
        exit_time = datetime.now()
        
        # Eğer entry_time timezone bilgisi içeriyorsa, exit_time'ı da timezone bilgisiyle oluştur
        if db_record.entry_time.tzinfo is not None:
            import pytz
            exit_time = datetime.now(pytz.UTC)
            logger.info(f"entry_time timezone bilgisi içeriyor, exit_time de aynı timezone kullanacak: {exit_time}")
            
        try:
            # Ücret hesaplama - API'den otomatik olarak doğru ücreti alır
            fee = calculate_parking_fee(db_record.entry_time, exit_time, parking_id)
            logger.info(f"Hesaplanan ücret: {fee/100:.2f} TL ({fee} kuruş)")
        except ValueError as e:
            # API'den ücret alamadıysak hatayı göster ve işlemi durdur
            logger.error(f"Ücret hesaplanamadı: {str(e)}")
            raise ValueError(f"Otopark ID={parking_id} için ücret bilgisi alınamadı. İşlem iptal edildi.")
        
        # Veritabanını güncelle
        db_record.exit_time = exit_time
        db_record.is_active = False
        db_record.parking_fee = fee
        
        # Database güncellemesini logla
        logger.info(f"Veritabanı güncellemesi: exit_time={exit_time}, is_active=False, parking_fee={fee}")
        
        try:
            db.commit()
            db.refresh(db_record)
            logger.info(f"✅ Park kaydı başarıyla kapatıldı. Kayıt ID: {db_record.id}, Ücret: {db_record.parking_fee/100:.2f} TL")
            
            # Son kontrol - veritabanına doğru değer kaydedildi mi?
            if db_record.parking_fee != fee:
                logger.error(f"❌ VERİTABANI HATA: Kaydedilen ücret ({db_record.parking_fee}) hesaplanan ücretten ({fee}) farklı!")
        except Exception as e:
            logger.error(f"Kayıt kapatılırken hata: {str(e)}")
            db.rollback()
            raise
    
    except Exception as e:
        logger.error(f"Park kaydı kapatılırken beklenmeyen hata: {str(e)}")
        import traceback
        traceback.print_exc()
        raise
    
    logger.info(f"===> Park kaydı kapama tamamlandı. Kayıt ID: {record_id}")
    return db_record 