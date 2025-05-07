"""
Veritabanı modellerini test eden unit testler
"""

import pytest
import sys
import os
from datetime import datetime
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

# Projenin kök dizinini sys.path'e ekle
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Test edilecek modülleri import et
from app.models import Base, Vehicle, ParkingRecord, ParkingSpace
from app.database import SessionLocal

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
def sample_vehicle(test_db):
    """Test için örnek bir araç oluşturur"""
    vehicle = Vehicle(
        license_plate="34ABC123",
        vehicle_type="Sedan",
        owner_name="Test Kullanıcısı"
    )
    test_db.add(vehicle)
    test_db.commit()
    test_db.refresh(vehicle)
    return vehicle

@pytest.fixture
def sample_parking_record(test_db, sample_vehicle):
    """Test için örnek bir otopark kaydı oluşturur"""
    record = ParkingRecord(
        vehicle_id=sample_vehicle.id,
        entry_time=datetime.now(),
        is_active=True
    )
    test_db.add(record)
    test_db.commit()
    test_db.refresh(record)
    return record

@pytest.fixture
def sample_parking_space(test_db):
    """Test için örnek bir otopark alanı oluşturur"""
    space = ParkingSpace(
        space_number="A101",
        is_occupied=False
    )
    test_db.add(space)
    test_db.commit()
    test_db.refresh(space)
    return space

# Model Testleri
def test_vehicle_model_created(sample_vehicle):
    """Vehicle modelinin doğru şekilde oluşturulduğunu test eder"""
    assert sample_vehicle.id is not None
    assert sample_vehicle.license_plate == "34ABC123"
    assert sample_vehicle.vehicle_type == "Sedan"
    assert sample_vehicle.owner_name == "Test Kullanıcısı"
    assert sample_vehicle.created_at is not None

def test_parking_record_model_created(sample_parking_record, sample_vehicle):
    """ParkingRecord modelinin doğru şekilde oluşturulduğunu test eder"""
    assert sample_parking_record.id is not None
    assert sample_parking_record.vehicle_id == sample_vehicle.id
    assert sample_parking_record.entry_time is not None
    assert sample_parking_record.exit_time is None
    assert sample_parking_record.is_active is True
    assert sample_parking_record.parking_fee is None

def test_parking_space_model_created(sample_parking_space):
    """ParkingSpace modelinin doğru şekilde oluşturulduğunu test eder"""
    assert sample_parking_space.id is not None
    assert sample_parking_space.space_number == "A101"
    assert sample_parking_space.is_occupied is False
    assert sample_parking_space.vehicle_id is None
    assert sample_parking_space.last_updated is not None

def test_vehicle_parking_record_relationship(test_db, sample_vehicle, sample_parking_record):
    """Vehicle ve ParkingRecord arasındaki ilişkiyi test eder"""
    # Veritabanından tazelenmiş vehicle nesnesi al
    vehicle = test_db.query(Vehicle).filter(Vehicle.id == sample_vehicle.id).first()
    
    # İlişkilerin doğruluğunu kontrol et
    assert len(vehicle.parking_records) == 1
    assert vehicle.parking_records[0].id == sample_parking_record.id
    assert vehicle.parking_records[0].vehicle_id == vehicle.id

def test_update_vehicle(test_db, sample_vehicle):
    """Vehicle modelinin güncellenebilirliğini test eder"""
    # Aracı güncelle
    sample_vehicle.license_plate = "34DEF456"
    sample_vehicle.vehicle_type = "SUV"
    test_db.commit()
    
    # Güncellenmiş aracı sorgula
    updated_vehicle = test_db.query(Vehicle).filter(Vehicle.id == sample_vehicle.id).first()
    
    # Güncellemenin başarılı olduğunu kontrol et
    assert updated_vehicle.license_plate == "34DEF456"
    assert updated_vehicle.vehicle_type == "SUV"

def test_delete_vehicle(test_db, sample_vehicle):
    """Vehicle modelinin silinebilirliğini test eder"""
    # Aracı sil
    test_db.delete(sample_vehicle)
    test_db.commit()
    
    # Aracın silindiğini kontrol et
    deleted_vehicle = test_db.query(Vehicle).filter(Vehicle.id == sample_vehicle.id).first()
    assert deleted_vehicle is None

def test_close_parking_record(test_db, sample_parking_record):
    """ParkingRecord'ın kapatılabilirliğini test eder"""
    # Kaydı güncelle
    exit_time = datetime.now()
    sample_parking_record.exit_time = exit_time
    sample_parking_record.is_active = False
    sample_parking_record.parking_fee = 2000  # 20 TL
    test_db.commit()
    
    # Güncellenmiş kaydı sorgula
    updated_record = test_db.query(ParkingRecord).filter(ParkingRecord.id == sample_parking_record.id).first()
    
    # Güncellemenin başarılı olduğunu kontrol et
    assert updated_record.exit_time is not None
    assert updated_record.is_active is False
    assert updated_record.parking_fee == 2000

def test_update_parking_space_occupancy(test_db, sample_parking_space, sample_vehicle):
    """ParkingSpace'in işgal durumunu güncelleme testleri"""
    # Park alanını güncelle
    sample_parking_space.is_occupied = True
    sample_parking_space.vehicle_id = sample_vehicle.id
    test_db.commit()
    
    # Güncellenmiş park alanını sorgula
    updated_space = test_db.query(ParkingSpace).filter(ParkingSpace.id == sample_parking_space.id).first()
    
    # Güncellemenin başarılı olduğunu kontrol et
    assert updated_space.is_occupied is True
    assert updated_space.vehicle_id == sample_vehicle.id 