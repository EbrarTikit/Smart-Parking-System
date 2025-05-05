import logging
import sys
import os
import time
import io
import base64
from typing import Optional, List, Dict, Any, Union

# FastAPI 
from fastapi import FastAPI, File, UploadFile, HTTPException, Depends, BackgroundTasks, Body, Query
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# Veritabanı bağlantısı
from sqlalchemy import create_engine, Column, Integer, String, Float, DateTime, func, Boolean, Text, inspect
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session
import datetime

# CRUD ve Şemalar
from app.database import engine, SessionLocal, Base
from app.models import Vehicle, ParkingRecord
from app.schemas import (
    VehicleCreate, Vehicle as VehicleSchema, 
    ParkingRecordCreate, ParkingRecord as ParkingRecordSchema, 
    ParkingRecordUpdate, VehicleWithRecords
)
from app.crud import (
    get_vehicle, get_vehicle_by_license_plate, create_vehicle, 
    get_parking_record, get_active_parking_record_by_vehicle, create_parking_record,
    close_parking_record, calculate_parking_fee
)

# Konfigürasyon
from app.config import DATABASE_URL, RABBITMQ_URL

# Log yapılandırması
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

# Uygulama oluştur
app = FastAPI(title="License Plate Recognition Service",
             description="Araç plakalarını tespit ve tanıma servisi",
             version="1.0.0")

# CORS ayarları
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Veritabanı bağlantısı
try:
    engine = create_engine(DATABASE_URL)
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    Base = declarative_base()
    logger.info("Veritabanı bağlantısı başarıyla kuruldu")
except Exception as e:
    logger.error(f"Veritabanı bağlantısı kurulamadı: {str(e)}")
    engine = None
    SessionLocal = None
    Base = declarative_base()

# Model içe aktarımı
try:
    from app.model import process_image_for_plate_recognition
    logger.info("Plaka tanıma modeli başarıyla yüklendi")
    MODEL_AVAILABLE = True
except Exception as e:
    logger.error(f"Plaka tanıma modeli yüklenemedi: {str(e)}")
    import traceback
    traceback.print_exc()
    MODEL_AVAILABLE = False

# Veritabanı modelleri
class PlateRecord(Base):
    __tablename__ = "plate_records"

    id = Column(Integer, primary_key=True, index=True)
    plate_text = Column(String, index=True)
    confidence = Column(Float)
    timestamp = Column(DateTime, default=datetime.datetime.utcnow)
    image_path = Column(String, nullable=True)
    processed = Column(Boolean, default=False)
    bbox = Column(String, nullable=True)  # JSON formatında bounding box
    
    def __repr__(self):
        return f"<PlateRecord(id={self.id}, plate={self.plate_text}, confidence={self.confidence})>"

# Veritabanı oluştur
if engine is not None:
    try:
        Base.metadata.create_all(bind=engine)
        logger.info("Veritabanı tabloları başarıyla oluşturuldu")
    except Exception as e:
        logger.error(f"Veritabanı tabloları oluşturulamadı: {str(e)}")

# Bağımlılık
def get_db():
    """Database session oluşturur"""
    if SessionLocal is None:
        raise HTTPException(status_code=503, detail="Veritabanı bağlantısı kullanılamıyor")
    
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Pydantic modelleri
class PlateDetectionResponse(BaseModel):
    success: bool
    message: str
    plates: List[str] = []
    details: Dict[str, Any] = {}
    
class PlateInfo(BaseModel):
    plate_text: str
    confidence: float
    timestamp: str

class PlateImage(BaseModel):
    image: str = Field(..., description="Base64 kodlanmış görüntü verisi")
    file_name: Optional[str] = None
    save_debug: bool = False

# API Route'ları
@app.get("/health")
def health_check():
    """Servis sağlık kontrolü"""
    return {
        "status": "healthy",
        "timestamp": time.time(),
        "model_available": MODEL_AVAILABLE,
        "database_connected": engine is not None
    }

@app.post("/detect-plate", response_model=PlateDetectionResponse)
async def detect_license_plate(
    file: UploadFile = File(...),
    save_db: bool = Query(True, description="Sonuçları veritabanına kaydet"),
    save_debug: bool = Query(False, description="Debug görsellerini kaydet"),
    db: Session = Depends(get_db)
):
    """
    Yüklenen görüntüde plaka tespiti ve tanıma yapar
    """
    if not MODEL_AVAILABLE:
        return PlateDetectionResponse(
            success=False,
            message="Plaka tanıma modeli yüklenemedi, servis kullanılamıyor",
            plates=[],
            details={"error": "Model yüklenemedi"}
        )
        
    try:
        # Dosyayı oku
        contents = await file.read()
        
        # Plaka tanıma işlemini gerçekleştir
        results = process_image_for_plate_recognition(contents, save_debug=save_debug)
        
        if "error" in results:
            return PlateDetectionResponse(
                success=False,
                message=f"Plaka tanıma sırasında hata: {results['error']}",
                plates=[],
                details=results
            )
        
        # Sonuçları veritabanına kaydet
        if save_db and "license_plates" in results and results["license_plates"]:
            for plate_text in results["license_plates"]:
                # Plaka bilgisini bul
                plate_info = None
                plate_confidence = 0.0
                bbox_str = ""
                
                # Frame içindeki tüm araçlar ve plakalar
                for frame_num, frame_data in results["results"].items():
                    for obj_id, obj_data in frame_data.items():
                        if "license_plate" in obj_data and obj_data["license_plate"]["text"] == plate_text:
                            plate_info = obj_data["license_plate"]
                            plate_confidence = plate_info.get("text_score", 0.0)
                            bbox = plate_info.get("bbox", [0, 0, 0, 0])
                            bbox_str = f"{bbox[0]},{bbox[1]},{bbox[2]},{bbox[3]}"
                            break
                
                # Veritabanına kaydet
                db_record = PlateRecord(
                    plate_text=plate_text,
                    confidence=plate_confidence,
                    bbox=bbox_str,
                    image_path=f"plates/{int(time.time())}_{plate_text}.jpg",
                    processed=False
                )
                db.add(db_record)
            
            db.commit()
            logger.info(f"Toplam {len(results['license_plates'])} plaka kaydı veritabanına eklendi")
        
        return PlateDetectionResponse(
            success=True,
            message=f"Plaka tanıma başarılı, {len(results.get('license_plates', []))} plaka tespit edildi",
            plates=results.get("license_plates", []),
            details=results
        )
        
    except Exception as e:
        logger.error(f"Plaka tanıma sırasında beklenmeyen hata: {str(e)}")
        import traceback
        traceback.print_exc()
        return PlateDetectionResponse(
            success=False,
            message=f"Plaka tanıma sırasında beklenmeyen hata: {str(e)}",
            plates=[],
            details={"error": str(e)}
        )

@app.post("/detect-plate-base64", response_model=PlateDetectionResponse)
async def detect_license_plate_base64(
    plate_image: PlateImage,
    save_db: bool = Query(True, description="Sonuçları veritabanına kaydet"),
    db: Session = Depends(get_db)
):
    """
    Base64 kodlanmış görüntüde plaka tespiti ve tanıma yapar
    """
    if not MODEL_AVAILABLE:
        return PlateDetectionResponse(
                    success=False,
            message="Plaka tanıma modeli yüklenemedi, servis kullanılamıyor",
            plates=[],
            details={"error": "Model yüklenemedi"}
        )
    
    try:
        # Base64 kodunu çöz
        if "," in plate_image.image:
            # "data:image/jpeg;base64," gibi bir ön ek varsa kaldır
            image_data = plate_image.image.split(",")[1]
        else:
            image_data = plate_image.image
            
        image_bytes = base64.b64decode(image_data)
        
        # Plaka tanıma işlemini gerçekleştir
        results = process_image_for_plate_recognition(image_bytes, save_debug=plate_image.save_debug)
        
        if "error" in results:
            return PlateDetectionResponse(
                    success=False,
                message=f"Plaka tanıma sırasında hata: {results['error']}",
                plates=[],
                details=results
            )
        
        # Sonuçları veritabanına kaydet
        if save_db and "license_plates" in results and results["license_plates"]:
            for plate_text in results["license_plates"]:
                # Plaka bilgisini bul
                plate_info = None
                plate_confidence = 0.0
                bbox_str = ""
                
                # Frame içindeki tüm araçlar ve plakalar
                for frame_num, frame_data in results["results"].items():
                    for obj_id, obj_data in frame_data.items():
                        if "license_plate" in obj_data and obj_data["license_plate"]["text"] == plate_text:
                            plate_info = obj_data["license_plate"]
                            plate_confidence = plate_info.get("text_score", 0.0)
                            bbox = plate_info.get("bbox", [0, 0, 0, 0])
                            bbox_str = f"{bbox[0]},{bbox[1]},{bbox[2]},{bbox[3]}"
                            break
                
                # Veritabanına kaydet
                db_record = PlateRecord(
                    plate_text=plate_text,
                    confidence=plate_confidence,
                    bbox=bbox_str,
                    image_path=f"plates/{int(time.time())}_{plate_text}.jpg",
                    processed=False
                )
                db.add(db_record)
            
            db.commit()
            logger.info(f"Toplam {len(results['license_plates'])} plaka kaydı veritabanına eklendi")
        
        return PlateDetectionResponse(
            success=True,
            message=f"Plaka tanıma başarılı, {len(results.get('license_plates', []))} plaka tespit edildi",
            plates=results.get("license_plates", []),
            details=results
        )
        
    except Exception as e:
        logger.error(f"Plaka tanıma sırasında beklenmeyen hata: {str(e)}")
        import traceback
        traceback.print_exc()
        return PlateDetectionResponse(
            success=False,
            message=f"Plaka tanıma sırasında beklenmeyen hata: {str(e)}",
            plates=[],
            details={"error": str(e)}
        )

@app.get("/plates", response_model=List[PlateInfo])
def get_all_plates(
    limit: int = Query(10, description="Maksimum kayıt sayısı"),
    skip: int = Query(0, description="Atlama sayısı"),
    db: Session = Depends(get_db)
):
    """
    Veritabanındaki plaka kayıtlarını listeler
    """
    try:
        plates = db.query(PlateRecord).order_by(PlateRecord.timestamp.desc()).offset(skip).limit(limit).all()
        
        result = []
        for plate in plates:
            result.append(PlateInfo(
                plate_text=plate.plate_text,
                confidence=plate.confidence,
                timestamp=plate.timestamp.isoformat()
            ))
            
        return result
    except Exception as e:
        logger.error(f"Plaka kayıtları alınırken hata: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Veritabanı hatası: {str(e)}")

@app.get("/")
def root():
    """Ana sayfa - API dokümantasyonuna yönlendirme"""
    return {
        "message": "License Plate Recognition Service",
        "docs": "/docs",
        "status": "running",
        "model_status": "available" if MODEL_AVAILABLE else "unavailable",
        "database_status": "connected" if engine is not None else "disconnected"
    }

# Uygulama başlatıldığında
@app.on_event("startup")
def startup_event():
    logger.info("License Plate Recognition Service başlatılıyor...")
    
    # Klasörleri oluştur
    os.makedirs("./debug_plates", exist_ok=True)
    
    # Veritabanı tablolarını oluştur
    try:
        # Bağlantı kontrolü
        if engine is not None:
            inspector = inspect(engine)
            
            # PlateRecord tablosunu oluştur
            if not inspector.has_table("plate_records"):
                Base.metadata.create_all(bind=engine)
                logger.info("PlateRecord tablosu oluşturuldu")
            
            # Araç ve park kayıtları tablolarını oluştur
            if not inspector.has_table("vehicles") or not inspector.has_table("parking_records"):
                from app.models import Vehicle, ParkingRecord
                Base.metadata.create_all(bind=engine)
                logger.info("Araç ve park kayıtları tabloları oluşturuldu")
            
            logger.info("Veritabanı tabloları hazır")
        else:
            logger.warning("Veritabanı bağlantısı olmadığı için tablolar oluşturulamadı")
    except Exception as e:
        logger.error(f"Veritabanı tabloları oluşturulurken hata: {str(e)}")
    
    logger.info(f"Model durumu: {'Kullanılabilir' if MODEL_AVAILABLE else 'Kullanılamıyor'}")
    logger.info(f"Veritabanı durumu: {'Bağlı' if engine is not None else 'Bağlı değil'}")
    logger.info("Servis başlatıldı!")

# Uygulama kapatıldığında
@app.on_event("shutdown")
def shutdown_event():
    logger.info("License Plate Recognition Service kapatılıyor...")

# Uygulamayı doğrudan çalıştırma
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

# -------------- OTOPARK GİRİŞ-ÇIKIŞ ENDPOINTLERİ --------------

class VehicleEntryRequest(BaseModel):
    license_plate: str

class VehicleEntryResponse(BaseModel):
    success: bool
    message: str
    entry_time: Optional[str] = None
    vehicle: Optional[VehicleSchema] = None
    parking_record_id: Optional[int] = None

class VehicleExitRequest(BaseModel):
    license_plate: str

class VehicleExitResponse(BaseModel):
    success: bool
    message: str
    exit_time: Optional[str] = None
    entry_time: Optional[str] = None
    duration_hours: Optional[float] = None
    parking_fee: Optional[float] = None  # TL cinsinden
    parking_record_id: Optional[int] = None

@app.post("/vehicle/entry", response_model=VehicleEntryResponse)
def register_vehicle_entry(
    entry: VehicleEntryRequest,
    db: Session = Depends(get_db)
):
    """
    Araç otoparka giriş yaptığında, plaka bilgisini kaydet ve giriş kaydı oluştur.
    (Yedek Yöntem: Kamera/görüntü işleme sisteminin çalışmadığı durumlarda yedek yöntem olarak)
    """
    try:
        # Plaka kontrolü
        if not entry.license_plate:
            return VehicleEntryResponse(
                success=False,
                message="Plaka bilgisi gereklidir"
            )
        
        # Araç veritabanında var mı kontrol et
        db_vehicle = get_vehicle_by_license_plate(db, entry.license_plate)
        
        # Yoksa yeni araç kaydı oluştur
        if not db_vehicle:
            vehicle_data = VehicleCreate(
                license_plate=entry.license_plate
            )
            db_vehicle = create_vehicle(db, vehicle_data)
            logger.info(f"Yeni araç kaydı oluşturuldu: {entry.license_plate}")
        
        # Aktif park kaydı var mı kontrol et
        active_record = get_active_parking_record_by_vehicle(db, db_vehicle.id)
        if active_record:
            return VehicleEntryResponse(
                success=False,
                message=f"Bu araç zaten otoparkta park halinde. Giriş zamanı: {active_record.entry_time}",
                entry_time=active_record.entry_time.isoformat(),
                vehicle=VehicleSchema.from_orm(db_vehicle),
                parking_record_id=active_record.id
            )
        
        # Yeni park kaydı oluştur
        parking_record_data = ParkingRecordCreate(vehicle_id=db_vehicle.id)
        new_record = create_parking_record(db, parking_record_data)
        
        return VehicleEntryResponse(
            success=True,
            message=f"Araç girişi başarıyla kaydedildi: {entry.license_plate}",
            entry_time=new_record.entry_time.isoformat(),
            vehicle=VehicleSchema.from_orm(db_vehicle),
            parking_record_id=new_record.id
        )
        
    except Exception as e:
        logger.error(f"Araç girişi kaydedilirken hata: {str(e)}")
        import traceback
        traceback.print_exc()
        return VehicleEntryResponse(
            success=False,
            message=f"Araç girişi kaydedilirken hata: {str(e)}"
        )

@app.post("/vehicle/exit", response_model=VehicleExitResponse)
def register_vehicle_exit(
    exit_req: VehicleExitRequest,
    db: Session = Depends(get_db)
):
    """
    Araç otoparktan çıkış yaptığında, çıkış kaydı oluştur ve ücretlendirme yap
    """
    try:
        # Plaka kontrolü
        if not exit_req.license_plate:
            return VehicleExitResponse(
                success=False,
                message="Plaka bilgisi gereklidir"
            )
        
        # Araç veritabanında var mı kontrol et
        db_vehicle = get_vehicle_by_license_plate(db, exit_req.license_plate)
        if not db_vehicle:
            return VehicleExitResponse(
                success=False,
                message=f"Bu plakaya ait araç kaydı bulunamadı: {exit_req.license_plate}"
            )
        
        # Aktif park kaydı var mı kontrol et
        active_record = get_active_parking_record_by_vehicle(db, db_vehicle.id)
        if not active_record:
            return VehicleExitResponse(
                success=False,
                message=f"Bu araca ait aktif park kaydı bulunamadı: {exit_req.license_plate}"
            )
        
        # Park kaydını kapat ve ücretlendirme yap
        closed_record = close_parking_record(db, active_record.id)
        
        # Otopark süresini hesapla (saat olarak)
        duration = (closed_record.exit_time - closed_record.entry_time).total_seconds() / 3600
        
        # Ücret kuruş cinsinden, TL'ye çevir
        fee_tl = closed_record.parking_fee / 100.0 if closed_record.parking_fee else 0
        
        return VehicleExitResponse(
            success=True,
            message=f"Araç çıkışı başarıyla kaydedildi: {exit_req.license_plate}",
            exit_time=closed_record.exit_time.isoformat(),
            entry_time=closed_record.entry_time.isoformat(),
            duration_hours=round(duration, 2),
            parking_fee=round(fee_tl, 2),
            parking_record_id=closed_record.id
        )
        
    except Exception as e:
        logger.error(f"Araç çıkışı kaydedilirken hata: {str(e)}")
        import traceback
        traceback.print_exc()
        return VehicleExitResponse(
            success=False,
            message=f"Araç çıkışı kaydedilirken hata: {str(e)}"
        )

@app.post("/process-plate-entry", response_model=VehicleEntryResponse)
async def process_plate_for_entry(
    file: UploadFile = File(...),
    save_debug: bool = Query(False, description="Debug görsellerini kaydet"),
    db: Session = Depends(get_db)
):
    """
    Yüklenen görüntüdeki plakayı tanı ve araç girişi olarak işle
    """
    try:
        # Dosyayı oku
        contents = await file.read()
        
        # Plaka tanıma işlemini gerçekleştir
        results = process_image_for_plate_recognition(contents, save_debug=save_debug)
        
        if "error" in results:
            return VehicleEntryResponse(
                success=False,
                message=f"Plaka tanıma sırasında hata: {results['error']}"
            )
        
        # Plaka bulunamadıysa
        if not results.get("license_plates", []):
            return VehicleEntryResponse(
                success=False,
                message="Görüntüde plaka bulunamadı"
            )
        
        # İlk plakayı al
        license_plate = results["license_plates"][0]
        
        # Araç girişi yap
        vehicle_entry = VehicleEntryRequest(license_plate=license_plate)
        return register_vehicle_entry(vehicle_entry, db)
        
    except Exception as e:
        logger.error(f"Plaka tanıma ve araç girişi sırasında hata: {str(e)}")
        import traceback
        traceback.print_exc()
        return VehicleEntryResponse(
            success=False,
            message=f"Plaka tanıma ve araç girişi sırasında hata: {str(e)}"
        )

@app.post("/process-plate-exit", response_model=VehicleExitResponse)
async def process_plate_for_exit(
    file: UploadFile = File(...),
    save_debug: bool = Query(False, description="Debug görsellerini kaydet"),
    db: Session = Depends(get_db)
):
    """
    Yüklenen görüntüdeki plakayı tanı ve araç çıkışı olarak işle
    """
    try:
        # Dosyayı oku
        contents = await file.read()
        
        # Plaka tanıma işlemini gerçekleştir
        results = process_image_for_plate_recognition(contents, save_debug=save_debug)
        
        if "error" in results:
            return VehicleExitResponse(
                success=False,
                message=f"Plaka tanıma sırasında hata: {results['error']}"
            )
        
        # Plaka bulunamadıysa
        if not results.get("license_plates", []):
            return VehicleExitResponse(
                success=False,
                message="Görüntüde plaka bulunamadı"
            )
        
        # İlk plakayı al
        license_plate = results["license_plates"][0]
        
        # Araç çıkışı yap
        vehicle_exit = VehicleExitRequest(license_plate=license_plate)
        return register_vehicle_exit(vehicle_exit, db)
        
    except Exception as e:
        logger.error(f"Plaka tanıma ve araç çıkışı sırasında hata: {str(e)}")
        import traceback
        traceback.print_exc()
        return VehicleExitResponse(
            success=False,
            message=f"Plaka tanıma ve araç çıkışı sırasında hata: {str(e)}"
        )