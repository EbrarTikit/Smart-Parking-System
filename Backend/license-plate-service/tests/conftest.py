"""
Tüm testler için ortak fixture'ları içeren dosya
"""

import pytest
import sys
import os
import io
import base64
import PIL.Image
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

# Projenin kök dizinini sys.path'e ekle
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Test edilecek modülleri import et
from app.models import Base, Vehicle, ParkingRecord, ParkingSpace
from app.schemas import VehicleCreate, ParkingRecordCreate, ParkingSpaceCreate
from app.database import SessionLocal

# Test için hafızada SQLite veritabanı kur
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

# Test verileri - Araç
@pytest.fixture
def test_vehicle(test_db):
    """Test için araç oluşturur"""
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
def test_vehicle_data():
    """Test için araç verisi"""
    return VehicleCreate(
        license_plate="34ABC123",
        vehicle_type="Sedan",
        owner_name="Test Kullanıcısı"
    )

# Test verileri - Otopark Kaydı
@pytest.fixture
def test_parking_record(test_db, test_vehicle):
    """Test için otopark kaydı oluşturur"""
    record = ParkingRecord(
        vehicle_id=test_vehicle.id,
        entry_time=None,  # Varsayılan olarak şimdiki zaman
        is_active=True
    )
    test_db.add(record)
    test_db.commit()
    test_db.refresh(record)
    return record

@pytest.fixture
def test_parking_record_data(test_vehicle):
    """Test için otopark kaydı verisi"""
    return ParkingRecordCreate(
        vehicle_id=test_vehicle.id
    )

# Test verileri - Park Alanı
@pytest.fixture
def test_parking_space(test_db):
    """Test için park alanı oluşturur"""
    space = ParkingSpace(
        space_number="A101",
        is_occupied=False
    )
    test_db.add(space)
    test_db.commit()
    test_db.refresh(space)
    return space

@pytest.fixture
def test_parking_space_data():
    """Test için park alanı verisi"""
    return ParkingSpaceCreate(
        space_number="A101"
    )

# Test görüntüsü
@pytest.fixture
def sample_image_bytes():
    """Test için örnek bir resim oluşturur ve bytes olarak döndürür"""
    # Boş bir görüntü oluştur
    image = PIL.Image.new('RGB', (500, 300), color='white')
    # BytesIO nesnesine kaydet
    img_byte_arr = io.BytesIO()
    image.save(img_byte_arr, format='JPEG')
    # Görüntü verilerini al
    img_byte_arr = img_byte_arr.getvalue()
    
    return img_byte_arr

@pytest.fixture
def sample_image_base64(sample_image_bytes):
    """Test için örnek bir resmin base64 kodlanmış halini döndürür"""
    # Bytes'ı base64'e dönüştür
    base64_encoded = base64.b64encode(sample_image_bytes).decode('utf-8')
    return base64_encoded

# Plaka tanıma modelini mock'la
@pytest.fixture(scope="function")
def mock_model_process():
    """Plaka tanıma modelini mock'lar"""
    with pytest.MonkeyPatch.context() as mp:
        def mock_plate_recognition(*args, **kwargs):
            return {
                "license_plates": ["34ABC123"],
                "results": {
                    "0": {
                        "1": {
                            "car": {"bbox": [100, 100, 400, 300]},
                            "license_plate": {
                                "bbox": [150, 150, 250, 180],
                                "text": "34ABC123",
                                "text_score": 0.95,
                                "bbox_score": 0.98
                            }
                        }
                    }
                }
            }
        
        try:
            mp.setattr("app.model.process_image_for_plate_recognition", mock_plate_recognition)
            yield mock_plate_recognition
        except (ImportError, AttributeError):
            # Model import edilemiyor olabilir, bu durumda geçici bir çözüm yap
            yield mock_plate_recognition 