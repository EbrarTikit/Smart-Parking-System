"""
CRUD işlemleri için unit testler
"""

import pytest
import sys
import os
from datetime import datetime, timedelta
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

# Projenin kök dizinini sys.path'e ekle
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Test edilecek modülleri import et
from app.models import Base, Vehicle, ParkingRecord
from app.schemas import VehicleCreate, ParkingRecordCreate, ParkingRecordUpdate
from app.crud import (
    get_vehicle, get_vehicle_by_license_plate, get_vehicles, create_vehicle, update_vehicle, delete_vehicle,
    get_parking_record, get_active_parking_record_by_vehicle, get_parking_records, create_parking_record, 
    update_parking_record, calculate_parking_fee, close_parking_record
)

# Test için memory SQLite veritabanı kur
@pytest.fixture(scope="function")
def test_db():
    """
    Test için memory SQLite veritabanı oluşturur.
    Her test sonrası veritabanı otomatik olarak temizlenir.
    """
    # In-memory SQLite veritabanı oluştur
    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    
    # Tabloları oluştur
    Base.metadata.create_all(engine)
    
    # Test session oluştur
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    db = TestingSessionLocal()
    
    try:
        yield db
    finally:
        # Her test sonrası veritabanını kapat
        db.close()

# Test Verileri
@pytest.fixture
def test_vehicle_data():
    """Test için araç verisi"""
    return VehicleCreate(
        license_plate="34ABC123"
    )

@pytest.fixture
def test_parking_record_data():
    """Test için otopark kaydı verisi"""
    return ParkingRecordCreate(
        vehicle_id=1  # Bu ID, test_db tarafından oluşturulacak gerçek bir araç ID'si ile değiştirilmeli
    )

# Vehicle CRUD Testleri
def test_create_vehicle(test_db, test_vehicle_data):
    """create_vehicle işlevini test eder"""
    db_vehicle = create_vehicle(test_db, test_vehicle_data)
    
    assert db_vehicle.id is not None
    assert db_vehicle.license_plate == test_vehicle_data.license_plate

def test_get_vehicle(test_db, test_vehicle_data):
    """get_vehicle işlevini test eder"""
    db_vehicle = create_vehicle(test_db, test_vehicle_data)
    
    found_vehicle = get_vehicle(test_db, db_vehicle.id)
    assert found_vehicle is not None
    assert found_vehicle.id == db_vehicle.id
    assert found_vehicle.license_plate == db_vehicle.license_plate

def test_get_vehicle_by_license_plate(test_db, test_vehicle_data):
    """get_vehicle_by_license_plate işlevini test eder"""
    db_vehicle = create_vehicle(test_db, test_vehicle_data)
    
    found_vehicle = get_vehicle_by_license_plate(test_db, db_vehicle.license_plate)
    assert found_vehicle is not None
    assert found_vehicle.id == db_vehicle.id
    assert found_vehicle.license_plate == db_vehicle.license_plate

def test_get_vehicles(test_db, test_vehicle_data):
    """get_vehicles işlevini test eder"""
    # İlk aracı oluştur
    vehicle1 = create_vehicle(test_db, test_vehicle_data)
    
    # İkinci araç için veri oluştur
    vehicle2_data = VehicleCreate(
        license_plate="34DEF456"
    )
    
    # İkinci aracı oluştur
    vehicle2 = create_vehicle(test_db, vehicle2_data)
    
    # Tüm araçları sorgula
    vehicles = get_vehicles(test_db)
    
    assert len(vehicles) == 2
    assert any(v.license_plate == vehicle1.license_plate for v in vehicles)
    assert any(v.license_plate == vehicle2.license_plate for v in vehicles)

def test_update_vehicle_crud(test_db, test_vehicle_data):
    """update_vehicle işlevini test eder"""
    # Aracı oluştur
    db_vehicle = create_vehicle(test_db, test_vehicle_data)
    
    # Güncellenmiş veri hazırla
    updated_data = VehicleCreate(
        license_plate="34DEF456"
    )
    
    # Aracı güncelle
    updated_vehicle = update_vehicle(test_db, db_vehicle.id, updated_data)
    
    assert updated_vehicle is not None
    assert updated_vehicle.license_plate == updated_data.license_plate

def test_delete_vehicle_crud(test_db, test_vehicle_data):
    """delete_vehicle işlevini test eder"""
    # Aracı oluştur
    db_vehicle = create_vehicle(test_db, test_vehicle_data)
    
    # Aracı sil
    result = delete_vehicle(test_db, db_vehicle.id)
    
    # Silme işlemi başarılı olmalı
    assert result is True
    
    # Araç artık veritabanında olmamalı
    found_vehicle = get_vehicle(test_db, db_vehicle.id)
    assert found_vehicle is None

# ParkingRecord CRUD Testleri
def test_create_parking_record(test_db, test_vehicle_data):
    """create_parking_record işlevini test eder"""
    # Önce bir araç oluştur
    vehicle = create_vehicle(test_db, test_vehicle_data)
    
    # Otopark kaydı verisini hazırla
    record_data = ParkingRecordCreate(vehicle_id=vehicle.id)
    
    # Otopark kaydı oluştur
    db_record = create_parking_record(test_db, record_data)
    
    assert db_record.id is not None
    assert db_record.vehicle_id == vehicle.id
    assert db_record.entry_time is not None
    assert db_record.is_active is True
    assert db_record.exit_time is None
    assert db_record.parking_fee is None

def test_get_parking_record(test_db, test_vehicle_data):
    """get_parking_record işlevini test eder"""
    # Önce bir araç oluştur
    vehicle = create_vehicle(test_db, test_vehicle_data)
    
    # Otopark kaydı verisini hazırla ve oluştur
    record_data = ParkingRecordCreate(vehicle_id=vehicle.id)
    db_record = create_parking_record(test_db, record_data)
    
    # Kaydı sorgula
    found_record = get_parking_record(test_db, db_record.id)
    
    assert found_record is not None
    assert found_record.id == db_record.id
    assert found_record.vehicle_id == vehicle.id

def test_get_active_parking_record_by_vehicle(test_db, test_vehicle_data):
    """get_active_parking_record_by_vehicle işlevini test eder"""
    # Önce bir araç oluştur
    vehicle = create_vehicle(test_db, test_vehicle_data)
    
    # Otopark kaydı verisini hazırla ve oluştur
    record_data = ParkingRecordCreate(vehicle_id=vehicle.id)
    db_record = create_parking_record(test_db, record_data)
    
    # Aktif kaydı sorgula
    active_record = get_active_parking_record_by_vehicle(test_db, vehicle.id)
    
    assert active_record is not None
    assert active_record.id == db_record.id
    assert active_record.vehicle_id == vehicle.id
    assert active_record.is_active is True

def test_update_parking_record(test_db, test_vehicle_data):
    """update_parking_record işlevini test eder"""
    # Önce bir araç oluştur
    vehicle = create_vehicle(test_db, test_vehicle_data)
    
    # Otopark kaydı verisini hazırla ve oluştur
    record_data = ParkingRecordCreate(vehicle_id=vehicle.id)
    db_record = create_parking_record(test_db, record_data)
    
    # Çıkış zamanı oluştur
    exit_time = datetime.now()
    
    # Güncellenmiş veri hazırla
    update_data = ParkingRecordUpdate(
        exit_time=exit_time,
        parking_fee=2000  # 20 TL
    )
    
    # Kaydı güncelle
    updated_record = update_parking_record(test_db, db_record.id, update_data)
    
    assert updated_record is not None
    assert updated_record.exit_time is not None
    assert updated_record.parking_fee == 2000
    assert updated_record.is_active is False

def test_calculate_parking_fee():
    """calculate_parking_fee işlevini test eder"""
    # Test için zaman dilimlerini oluştur
    entry_time = datetime.now() - timedelta(hours=2, minutes=30)  # 2.5 saat önce
    exit_time = datetime.now()
    
    # Ücreti hesapla (saat başına 10 TL = 1000 kuruş)
    fee = calculate_parking_fee(entry_time, exit_time)
    
    # Yaklaşık 2.5 saat için 25 TL (2500 kuruş) olmalı
    assert fee >= 2500
    assert fee <= 2600  # Biraz tolerans ekle

def test_close_parking_record(test_db, test_vehicle_data):
    """close_parking_record işlevini test eder"""
    # Önce bir araç oluştur
    vehicle = create_vehicle(test_db, test_vehicle_data)
    
    # Otopark kaydı verisini hazırla ve oluştur
    record_data = ParkingRecordCreate(vehicle_id=vehicle.id)
    db_record = create_parking_record(test_db, record_data)
    
    # Kaydı kapat
    closed_record = close_parking_record(test_db, db_record.id)
    
    assert closed_record is not None
    assert closed_record.exit_time is not None
    assert closed_record.is_active is False
    assert closed_record.parking_fee is not None  # En az minimum ücret (10 TL = 1000 kuruş) olmalı
    assert closed_record.parking_fee >= 1000 