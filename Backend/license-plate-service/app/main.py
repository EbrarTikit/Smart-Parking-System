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
from sqlalchemy import create_engine, Column, Integer, String, Float, DateTime, func, Boolean, Text
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session
import datetime

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
    vehicle_type = Column(String, nullable=True)
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
    vehicle_type: Optional[str] = None
    
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
                timestamp=plate.timestamp.isoformat(),
                vehicle_type=plate.vehicle_type
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