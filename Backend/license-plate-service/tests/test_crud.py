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
from app.models import Base, Vehicle, ParkingRecord, ParkingSpace
from app.schemas import VehicleCreate, ParkingRecordCreate, ParkingSpaceCreate, ParkingRecordUpdate, ParkingSpaceUpdate
from app.crud import (
    get_vehicle, get_vehicle_by_license_plate, get_vehicles, create_vehicle, update_vehicle, delete_vehicle,
    get_parking_record, get_active_parking_record_by_vehicle, get_parking_records, create_parking_record, 
    update_parking_record, calculate_parking_fee, close_parking_record,
    get_parking_space, get_parking_space_by_number, get_parking_spaces, 
    get_available_parking_spaces, create_parking_space, update_parking_space
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
        license_plate="34ABC123",
        vehicle_type="Sedan",
        owner_name="Test Kullanıcısı"
    )

@pytest.fixture
def test_parking_record_data():
    """Test için otopark kaydı verisi"""
    return ParkingRecordCreate(
        vehicle_id=1  # Bu ID, test_db tarafından oluşturulacak gerçek bir araç ID'si ile değiştirilmeli
    )

@pytest.fixture
def test_parking_space_data():
    """Test için otopark alanı verisi"""
    return ParkingSpaceCreate(
        space_number="A101"
    )

# Vehicle CRUD Testleri
def test_create_vehicle(test_db, test_vehicle_data):
    """create_vehicle işlevini test eder"""
    db_vehicle = create_vehicle(test_db, test_vehicle_data)
    
    assert db_vehicle.id is not None
    assert db_vehicle.license_plate == test_vehicle_data.license_plate
    assert db_vehicle.vehicle_type == test_vehicle_data.vehicle_type
    assert db_vehicle.owner_name == test_vehicle_data.owner_name

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
        license_plate="34DEF456",
        vehicle_type="SUV",
        owner_name="İkinci Test Kullanıcısı"
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
        license_plate="34DEF456",
        vehicle_type="SUV",
        owner_name="Güncellenmiş Kullanıcı"
    )
    
    # Aracı güncelle
    updated_vehicle = update_vehicle(test_db, db_vehicle.id, updated_data)
    
    assert updated_vehicle is not None
    assert updated_vehicle.license_plate == updated_data.license_plate
    assert updated_vehicle.vehicle_type == updated_data.vehicle_type
    assert updated_vehicle.owner_name == updated_data.owner_name

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

# ParkingSpace CRUD Testleri
def test_create_parking_space(test_db, test_parking_space_data):
    """create_parking_space işlevini test eder"""
    db_space = create_parking_space(test_db, test_parking_space_data)
    
    assert db_space.id is not None
    assert db_space.space_number == test_parking_space_data.space_number
    assert db_space.is_occupied is False
    assert db_space.vehicle_id is None

def test_get_parking_space(test_db, test_parking_space_data):
    """get_parking_space işlevini test eder"""
    db_space = create_parking_space(test_db, test_parking_space_data)
    
    found_space = get_parking_space(test_db, db_space.id)
    
    assert found_space is not None
    assert found_space.id == db_space.id
    assert found_space.space_number == db_space.space_number

def test_get_parking_space_by_number(test_db, test_parking_space_data):
    """get_parking_space_by_number işlevini test eder"""
    db_space = create_parking_space(test_db, test_parking_space_data)
    
    found_space = get_parking_space_by_number(test_db, db_space.space_number)
    
    assert found_space is not None
    assert found_space.id == db_space.id
    assert found_space.space_number == db_space.space_number

def test_get_parking_spaces(test_db, test_parking_space_data):
    """get_parking_spaces işlevini test eder"""
    # İlk park alanını oluştur
    space1 = create_parking_space(test_db, test_parking_space_data)
    
    # İkinci park alanı için veri oluştur
    space2_data = ParkingSpaceCreate(
        space_number="A102"
    )
    
    # İkinci park alanını oluştur
    space2 = create_parking_space(test_db, space2_data)
    
    # Tüm park alanlarını sorgula
    spaces = get_parking_spaces(test_db)
    
    assert len(spaces) == 2
    assert any(s.space_number == space1.space_number for s in spaces)
    assert any(s.space_number == space2.space_number for s in spaces)

def test_get_available_parking_spaces(test_db, test_parking_space_data, test_vehicle_data):
    """get_available_parking_spaces işlevini test eder"""
    # İlk park alanını oluştur (boş)
    space1 = create_parking_space(test_db, test_parking_space_data)
    
    # İkinci park alanı için veri oluştur
    space2_data = ParkingSpaceCreate(
        space_number="A102"
    )
    
    # İkinci park alanını oluştur
    space2 = create_parking_space(test_db, space2_data)
    
    # Bir araç oluştur
    vehicle = create_vehicle(test_db, test_vehicle_data)
    
    # İlk park alanını dolu olarak işaretle
    update_data = ParkingSpaceUpdate(
        is_occupied=True,
        vehicle_id=vehicle.id
    )
    update_parking_space(test_db, space1.id, update_data)
    
    # Müsait park alanlarını sorgula
    available_spaces = get_available_parking_spaces(test_db)
    
    assert len(available_spaces) == 1
    assert available_spaces[0].space_number == space2.space_number
    assert available_spaces[0].is_occupied is False

def test_update_parking_space_crud(test_db, test_parking_space_data, test_vehicle_data):
    """update_parking_space işlevini test eder"""
    # Park alanını oluştur
    space = create_parking_space(test_db, test_parking_space_data)
    
    # Bir araç oluştur
    vehicle = create_vehicle(test_db, test_vehicle_data)
    
    # Park alanını güncelle
    update_data = ParkingSpaceUpdate(
        is_occupied=True,
        vehicle_id=vehicle.id
    )
    
    updated_space = update_parking_space(test_db, space.id, update_data)
    
    assert updated_space is not None
    assert updated_space.is_occupied is True
    assert updated_space.vehicle_id == vehicle.id 