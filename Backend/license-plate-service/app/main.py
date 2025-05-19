import logging
import sys
import os
import time
import io
import base64
import uuid
import json
import asyncio
from typing import Optional, List, Dict, Any, Union

# FastAPI 
from fastapi import FastAPI, File, UploadFile, HTTPException, Depends, BackgroundTasks, Body, Query, WebSocket, WebSocketDisconnect
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

# WebSocket yönetimi
from app.websocket import manager, RoomType

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
class PlateInfo(BaseModel):
    plate_text: str
    confidence: float
    timestamp: str

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
    parking_id: int = 1  # Varsayılan otopark ID'si

class VehicleEntryResponse(BaseModel):
    success: bool
    message: str
    entry_time: Optional[str] = None
    vehicle: Optional[VehicleSchema] = None
    parking_record_id: Optional[int] = None

class VehicleExitRequest(BaseModel):
    license_plate: str
    parking_id: int = 1  # Varsayılan otopark ID'si

class VehicleExitResponse(BaseModel):
    success: bool
    message: str
    exit_time: Optional[str] = None
    entry_time: Optional[str] = None
    duration_hours: Optional[float] = None
    parking_fee: Optional[float] = None  # TL cinsinden
    parking_record_id: Optional[int] = None

# Asenkron task'leri güvenli şekilde çalıştırmak için yardımcı fonksiyon
def run_async(coroutine):
    """Senkron bir fonksiyondan asenkron bir coroutine'i çalıştırmak için güvenli yöntem"""
    try:
        loop = asyncio.get_event_loop()
    except RuntimeError:
        # Eğer mevcut event loop yoksa yeni bir tane oluştur
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
    
    # Eğer loop çalışmıyorsa yeni bir executor ile coroutine'i çalıştır
    if not loop.is_running():
        return loop.run_until_complete(coroutine)
    else:
        # Eğer loop zaten çalışıyorsa (uvicorn içinde), daha sonra çalıştırılmak üzere planla
        # Burası uvicorn'un ana event loop'u içinde çalışacaktır
        return asyncio.create_task(coroutine)

@app.post("/vehicle/entry", response_model=VehicleEntryResponse)
def register_vehicle_entry(
    entry: VehicleEntryRequest,
    db: Session = Depends(get_db),
    background_tasks: BackgroundTasks = BackgroundTasks()
):
    """
    Araç otoparka giriş yaptığında, plaka bilgisini kaydet ve giriş kaydı oluştur.
    (Yedek Yöntem: Kamera/görüntü işleme sisteminin çalışmadığı durumlarda yedek yöntem olarak)
    """
    try:
        # Logger oluştur
        logger.info(f"Araç girişi işlemi başlatılıyor: {entry.license_plate}, Otopark ID: {entry.parking_id}")
        
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
            response = VehicleEntryResponse(
                success=False,
                message=f"Bu araç zaten otoparkta park halinde. Giriş zamanı: {active_record.entry_time}",
                entry_time=active_record.entry_time.isoformat(),
                vehicle=VehicleSchema.from_orm(db_vehicle),
                parking_record_id=active_record.id
            )
            
            # WebSocket bildirimi gönder (zaten park halinde)
            background_tasks.add_task(
                lambda: run_async(manager.send_vehicle_update({
                    "id": db_vehicle.id,
                    "license_plate": db_vehicle.license_plate,
                    "status": "already_parked",
                    "message": f"Araç zaten park halinde: {entry.license_plate}",
                    "parking_record_id": active_record.id,
                    "entry_time": active_record.entry_time.isoformat(),
                    "parking_id": entry.parking_id  # Otopark ID'sini ekle
                }))
            )
            
            return response
        
        # Yeni park kaydı oluştur
        parking_record_data = ParkingRecordCreate(
            vehicle_id=db_vehicle.id,
            parking_id=entry.parking_id  # Otopark ID'sini ekle
        )
        new_record = create_parking_record(db, parking_record_data)
        
        response = VehicleEntryResponse(
            success=True,
            message=f"Araç girişi başarıyla kaydedildi: {entry.license_plate}",
            entry_time=new_record.entry_time.isoformat(),
            vehicle=VehicleSchema.from_orm(db_vehicle),
            parking_record_id=new_record.id
        )
        
        # WebSocket bildirimi gönder (yeni giriş)
        background_tasks.add_task(
            lambda: run_async(manager.send_parking_record_update({
                "id": new_record.id,
                "vehicle_id": db_vehicle.id,
                "license_plate": db_vehicle.license_plate,
                "action": "entry",
                "entry_time": new_record.entry_time.isoformat(),
                "message": f"Araç girişi: {entry.license_plate}",
                "parking_id": entry.parking_id  # Otopark ID'sini ekle
            }))
        )
        
        return response
        
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
    db: Session = Depends(get_db),
    background_tasks: BackgroundTasks = BackgroundTasks()
):
    """
    Araç otoparktan çıkış yaptığında, çıkış kaydı oluştur ve ücretlendirme yap
    """
    try:
        # Logger oluştur
        logger.info(f"Araç çıkışı işlemi başlatılıyor: {exit_req.license_plate}, Otopark ID: {exit_req.parking_id}")
        
        # Plaka kontrolü
        if not exit_req.license_plate:
            return VehicleExitResponse(
                success=False,
                message="Plaka bilgisi gereklidir"
            )
        
        # Araç veritabanında var mı kontrol et
        db_vehicle = get_vehicle_by_license_plate(db, exit_req.license_plate)
        if not db_vehicle:
            response = VehicleExitResponse(
                success=False,
                message=f"Bu plakaya ait araç kaydı bulunamadı: {exit_req.license_plate}"
            )
            
            # WebSocket bildirimi gönder (araç bulunamadı)
            background_tasks.add_task(
                lambda: run_async(manager.broadcast({
                    "type": "error",
                    "action": "exit_failed",
                    "reason": "vehicle_not_found",
                    "license_plate": exit_req.license_plate,
                    "message": f"Araç bulunamadı: {exit_req.license_plate}"
                }, RoomType.ADMIN))
            )
            
            return response
        
        # Aktif park kaydı var mı kontrol et
        active_record = get_active_parking_record_by_vehicle(db, db_vehicle.id)
        if not active_record:
            response = VehicleExitResponse(
                success=False,
                message=f"Bu araca ait aktif park kaydı bulunamadı: {exit_req.license_plate}"
            )
        
            # WebSocket bildirimi gönder (aktif kayıt yok)
            background_tasks.add_task(
                lambda: run_async(manager.broadcast({
                    "type": "error",
                    "action": "exit_failed",
                    "reason": "no_active_record",
                    "license_plate": exit_req.license_plate,
                    "vehicle_id": db_vehicle.id,
                    "message": f"Aktif park kaydı yok: {exit_req.license_plate}"
                }, RoomType.ADMIN))
            )
            
            return response
        
        # Park kaydını kapat ve ücretlendirme yap (otopark ID'si ile)
        logger.info(f"Araç çıkışı için park kaydı kapatılıyor: {active_record.id}, Otopark ID: {exit_req.parking_id}")
        closed_record = close_parking_record(db, active_record.id, exit_req.parking_id)
        
        # Otopark süresini hesapla (saat olarak)
        duration = (closed_record.exit_time - closed_record.entry_time).total_seconds() / 3600
        logger.info(f"Hesaplanan park süresi: {duration:.4f} saat")
        
        # ÜCRET DÖNÜŞÜMÜ (KRİTİK)
        # Not: ParkingRecord.parking_fee kuruş cinsinden saklanıyor (örn: 3000 = 30 TL)
        if closed_record.parking_fee is not None:
            fee_kurus = closed_record.parking_fee  # Integer değer (kuruş)
            fee_tl = float(fee_kurus) / 100.0  # TL'ye çevir (doğru yöntem)
            
            logger.info(f"Veritabanından çekilen ücret: {fee_kurus} kuruş")
            logger.info(f"TL'ye çevrilen ücret: {fee_tl:.2f} TL")
        else:
            fee_tl = 0
            logger.warning(f"Park kaydında ücret bilgisi bulunamadı!")
        
        # Yanıt nesnesi oluştur
        response = VehicleExitResponse(
            success=True,
            message=f"Araç çıkışı başarıyla kaydedildi: {exit_req.license_plate}",
            exit_time=closed_record.exit_time.isoformat(),
            entry_time=closed_record.entry_time.isoformat(),
            duration_hours=round(duration, 2),
            parking_fee=round(fee_tl, 2),  # TL cinsinden (kuruştan çevrilmiş)
            parking_record_id=closed_record.id
        )
        
        # WebSocket bildirimi gönder (çıkış başarılı)
        background_tasks.add_task(
            lambda: run_async(manager.send_parking_record_update({
                "id": closed_record.id,
                "vehicle_id": db_vehicle.id,
                "license_plate": exit_req.license_plate,
                "parking_id": exit_req.parking_id,
                "action": "exit",
                "entry_time": closed_record.entry_time.isoformat(),
                "exit_time": closed_record.exit_time.isoformat(),
                "duration_hours": round(duration, 2),
                "parking_fee": round(fee_tl, 2),
                "message": f"Araç çıkışı: {exit_req.license_plate}, Ücret: {fee_tl:.2f} TL"
            }))
        )
        
        logger.info(f"Çıkış cevabı hazırlandı: {response}")
        return response
        
    except Exception as e:
        logger.error(f"Araç çıkışı kaydedilirken hata: {str(e)}")
        import traceback
        traceback.print_exc()
        
        # WebSocket hata bildirimi
        try:
            background_tasks.add_task(
                lambda: run_async(manager.broadcast({
                    "type": "error",
                    "action": "exit_error",
                    "license_plate": exit_req.license_plate,
                    "parking_id": exit_req.parking_id,
                    "error": str(e),
                    "message": f"Çıkış işlemi sırasında hata: {str(e)}"
                }, RoomType.ADMIN))
            )
        except:
            pass
            
        return VehicleExitResponse(
            success=False,
            message=f"Araç çıkışı kaydedilirken hata: {str(e)}"
        )

@app.post("/process-plate-entry", response_model=VehicleEntryResponse)
async def process_plate_for_entry(
    file: UploadFile = File(...),
    save_debug: bool = Query(False, description="Debug görsellerini kaydet"),
    parking_id: int = Query(1, description="Otopark ID'si"),
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
        vehicle_entry = VehicleEntryRequest(license_plate=license_plate, parking_id=parking_id)
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
    parking_id: int = Query(1, description="Otopark ID'si"),
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
        vehicle_exit = VehicleExitRequest(license_plate=license_plate, parking_id=parking_id)
        return register_vehicle_exit(vehicle_exit, db)
        
    except Exception as e:
        logger.error(f"Plaka tanıma ve araç çıkışı sırasında hata: {str(e)}")
        import traceback
        traceback.print_exc()
        return VehicleExitResponse(
            success=False,
            message=f"Plaka tanıma ve araç çıkışı sırasında hata: {str(e)}"
        )

# -------------- WEBSOCKET ENDPOINTLERİ --------------

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    """
    Genel WebSocket bağlantı noktası. 
    Admin paneli veya diğer istemciler için genel bir WebSocket kanalı sağlar.
    """
    # Bağlantıyı kabul et
    await websocket.accept()
    
    # Benzersiz bir client_id oluştur
    client_id = str(uuid.uuid4())
    
    # Bağlantıyı kaydet
    manager.add_connection(client_id, websocket, RoomType.ALL)
    
    try:
        # Hoş geldin mesajı gönder
        await websocket.send_text(json.dumps({
            "type": "welcome",
            "message": "Bağlantı başarıyla kuruldu!",
            "client_id": client_id,
            "timestamp": datetime.datetime.now().isoformat()
        }))
        
        # İstemciden gelen mesajları dinle
        while True:
            data = await websocket.receive_text()
            try:
                message = json.loads(data)
                message_type = message.get("type", "unknown")
                
                # İstemciden gelen mesaj türüne göre işlem yap
                if message_type == "ping":
                    await websocket.send_text(json.dumps({
                        "type": "pong",
                        "timestamp": datetime.datetime.now().isoformat()
                    }))
                elif message_type == "status":
                    await websocket.send_text(json.dumps({
                        "type": "status",
                        "data": manager.get_connection_status(),
                        "timestamp": datetime.datetime.now().isoformat()
                    }))
                else:
                    logger.debug(f"Bilinmeyen mesaj türü: {message_type}, data: {data[:100]}")
                    
            except json.JSONDecodeError:
                logger.warning(f"Geçersiz JSON formatı: {data[:100]}")
                await websocket.send_text(json.dumps({
                    "type": "error",
                    "message": "Geçersiz JSON formatı",
                    "timestamp": datetime.datetime.now().isoformat()
                }))
                
    except WebSocketDisconnect:
        # Bağlantı kapandığında
        manager.remove_connection(client_id)
        logger.info(f"Client bağlantısı kapandı: {client_id}")
    except Exception as e:
        # Diğer hatalar
        logger.error(f"WebSocket hatası: {str(e)}")
        manager.remove_connection(client_id)

@app.websocket("/ws/admin")
async def websocket_admin_endpoint(websocket: WebSocket):
    """
    Admin paneli için özel WebSocket bağlantı noktası.
    Yönetici arayüzü için tüm sistem güncellemelerini içerir.
    """
    # Bağlantıyı kabul et
    await websocket.accept()
    
    # Benzersiz bir client_id oluştur
    client_id = f"admin-{str(uuid.uuid4())}"
    
    # Bağlantıyı kaydet (admin odasına)
    manager.add_connection(client_id, websocket, RoomType.ADMIN)
    
    try:
        # Hoş geldin mesajı gönder
        await websocket.send_text(json.dumps({
            "type": "welcome",
            "message": "Admin WebSocket bağlantısı başarıyla kuruldu!",
            "client_id": client_id,
            "timestamp": datetime.datetime.now().isoformat()
        }))
        
        # İstemciden gelen mesajları dinle
        while True:
            data = await websocket.receive_text()
            try:
                message = json.loads(data)
                message_type = message.get("type", "unknown")
                
                # Admin komutlarını işle
                if message_type == "broadcast":
                    # Tüm istemcilere mesaj gönder
                    message_data = message.get("data", {})
                    target_room = message.get("room", RoomType.ALL)
                    room_id = message.get("room_id")
                    
                    await manager.broadcast({
                        "type": "broadcast",
                        "source": "admin",
                        "data": message_data
                    }, target_room, room_id)
                    
                    await websocket.send_text(json.dumps({
                        "type": "broadcast_sent",
                        "timestamp": datetime.datetime.now().isoformat()
                    }))
                    
                elif message_type == "status":
                    # Bağlantı durumunu gönder
                    await websocket.send_text(json.dumps({
                        "type": "status",
                        "data": manager.get_connection_status(),
                        "timestamp": datetime.datetime.now().isoformat()
                    }))
                
                elif message_type == "parking_change":
                    # Otopark değişikliği olayını işle
                    parking_data = message.get("data", {})
                    parking_id = parking_data.get("parking_id")
                    
                    if parking_id:
                        logger.info(f"Admin tarafından otopark değişikliği: Otopark ID={parking_id}")
                        
                        # Otopark ile ilgili bilgileri döndür (ileride otopark API'den çekilebilir)
                        await websocket.send_text(json.dumps({
                            "type": "parking_info",
                            "data": {
                                "parking_id": parking_id,
                                "timestamp": datetime.datetime.now().isoformat()
                            }
                        }))
                    else:
                        logger.warning(f"Geçersiz otopark ID'si parking_change mesajında: {message}")
                
                else:
                    logger.debug(f"Bilinmeyen admin mesajı: {message_type}")
                    
            except json.JSONDecodeError:
                logger.warning(f"Geçersiz JSON formatı: {data[:100]}")
                await websocket.send_text(json.dumps({
                    "type": "error",
                    "message": "Geçersiz JSON formatı",
                    "timestamp": datetime.datetime.now().isoformat()
                }))
                
    except WebSocketDisconnect:
        # Bağlantı kapandığında
        manager.remove_connection(client_id)
        logger.info(f"Admin bağlantısı kapandı: {client_id}")
    except Exception as e:
        # Diğer hatalar
        logger.error(f"Admin WebSocket hatası: {str(e)}")
        manager.remove_connection(client_id)

@app.websocket("/ws/parking/{parking_id}")
async def websocket_parking_endpoint(websocket: WebSocket, parking_id: int):
    """
    Belirli bir otopark için WebSocket bağlantı noktası.
    Otopark ile ilgili tüm güncellemeleri içerir.
    """
    # Bağlantıyı kabul et
    await websocket.accept()
    
    # Benzersiz bir client_id oluştur
    client_id = f"parking-{parking_id}-{str(uuid.uuid4())}"
    
    # Bağlantıyı kaydet (otopark odasına)
    manager.add_connection(client_id, websocket, RoomType.PARKING, str(parking_id))
    
    try:
        # Hoş geldin mesajı gönder
        await websocket.send_text(json.dumps({
            "type": "welcome",
            "message": f"Otopark ID={parking_id} WebSocket bağlantısı başarıyla kuruldu!",
            "client_id": client_id,
            "parking_id": parking_id,
            "timestamp": datetime.datetime.now().isoformat()
        }))
        
        # İstemciden gelen mesajları dinle
        while True:
            data = await websocket.receive_text()
            try:
                message = json.loads(data)
                message_type = message.get("type", "unknown")
                
                # Otopark komutlarını işle
                if message_type == "status":
                    # Bağlantı durumunu gönder
                    await websocket.send_text(json.dumps({
                        "type": "status",
                        "data": {
                            "connection_count": manager.count_connections(RoomType.PARKING, str(parking_id)),
                            "parking_id": parking_id
                        },
                        "timestamp": datetime.datetime.now().isoformat()
                    }))
                    
                else:
                    logger.debug(f"Bilinmeyen otopark mesajı: {message_type}")
                    
            except json.JSONDecodeError:
                logger.warning(f"Geçersiz JSON formatı: {data[:100]}")
                await websocket.send_text(json.dumps({
                    "type": "error",
                    "message": "Geçersiz JSON formatı",
                    "timestamp": datetime.datetime.now().isoformat()
                }))
                
    except WebSocketDisconnect:
        # Bağlantı kapandığında
        manager.remove_connection(client_id)
        logger.info(f"Otopark bağlantısı kapandı: {client_id}")
    except Exception as e:
        # Diğer hatalar
        logger.error(f"Otopark WebSocket hatası: {str(e)}")
        manager.remove_connection(client_id)

@app.websocket("/ws/vehicle/{license_plate}")
async def websocket_vehicle_endpoint(websocket: WebSocket, license_plate: str):
    """
    Belirli bir araç için WebSocket bağlantı noktası.
    Araç ile ilgili tüm güncellemeleri içerir.
    """
    # Bağlantıyı kabul et
    await websocket.accept()
    
    # Benzersiz bir client_id oluştur
    client_id = f"vehicle-{license_plate}-{str(uuid.uuid4())}"
    
    # Bağlantıyı kaydet (araç odasına)
    manager.add_connection(client_id, websocket, RoomType.VEHICLE, license_plate)
    
    try:
        # Hoş geldin mesajı gönder
        await websocket.send_text(json.dumps({
            "type": "welcome",
            "message": f"Araç plaka={license_plate} WebSocket bağlantısı başarıyla kuruldu!",
            "client_id": client_id,
            "license_plate": license_plate,
            "timestamp": datetime.datetime.now().isoformat()
        }))
        
        # İstemciden gelen mesajları dinle
        while True:
            data = await websocket.receive_text()
            try:
                message = json.loads(data)
                message_type = message.get("type", "unknown")
                
                # Araç komutlarını işle
                if message_type == "status":
                    # Bağlantı durumunu gönder
                    await websocket.send_text(json.dumps({
                        "type": "status",
                        "data": {
                            "connection_count": manager.count_connections(RoomType.VEHICLE, license_plate),
                            "license_plate": license_plate
                        },
                        "timestamp": datetime.datetime.now().isoformat()
                    }))
                    
                else:
                    logger.debug(f"Bilinmeyen araç mesajı: {message_type}")
                    
            except json.JSONDecodeError:
                logger.warning(f"Geçersiz JSON formatı: {data[:100]}")
                await websocket.send_text(json.dumps({
                    "type": "error",
                    "message": "Geçersiz JSON formatı",
                    "timestamp": datetime.datetime.now().isoformat()
                }))
                
    except WebSocketDisconnect:
        # Bağlantı kapandığında
        manager.remove_connection(client_id)
        logger.info(f"Araç bağlantısı kapandı: {client_id}")
    except Exception as e:
        # Diğer hatalar
        logger.error(f"Araç WebSocket hatası: {str(e)}")
        manager.remove_connection(client_id)

# WebSocket bilgi endpointi
@app.get("/ws/info")
def websocket_info():
    """WebSocket bağlantı durumunu görüntüle"""
    return manager.get_connection_status()

@app.get("/active-vehicles")
def get_active_vehicles(
    limit: int = Query(50, description="Maksimum kayıt sayısı"),
    parking_id: int = Query(None, description="Otopark ID'si (filtreleme için)"),
    db: Session = Depends(get_db)
):
    """
    Şu anda otoparkta bulunan araçların listesini döndürür
    """
    try:
        # Aktif park kayıtlarını getir
        query = db.query(ParkingRecord).filter(
            ParkingRecord.exit_time == None  # Çıkış yapmamış olanlar
        ).join(Vehicle)
        
        # Otopark ID'si filtresi ekle
        if parking_id is not None:
            query = query.filter(ParkingRecord.parking_id == parking_id)
            
        active_records = query.order_by(ParkingRecord.entry_time.desc()).limit(limit).all()
        
        result = []
        for record in active_records:
            vehicle = record.vehicle  # İlişkili araç
            result.append({
                "id": vehicle.id,
                "license_plate": vehicle.license_plate,
                "entry_time": record.entry_time.isoformat(),
                "parking_record_id": record.id,
                "parking_id": record.parking_id or 1  # Default 1 olarak ayarla
            })
            
        return result
    except Exception as e:
        logger.error(f"Aktif araçlar alınırken hata: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Veritabanı hatası: {str(e)}")

@app.get("/recent-activities")
def get_recent_activities(
    limit: int = Query(20, description="Maksimum kayıt sayısı"),
    parking_id: int = Query(None, description="Otopark ID'si (filtreleme için)"),
    db: Session = Depends(get_db)
):
    """
    Son giriş/çıkış aktivitelerinin listesini döndürür
    """
    try:
        # Baz sorguları oluştur
        completed_query = db.query(ParkingRecord).filter(
            ParkingRecord.exit_time != None  # Çıkış yapmış olanlar
        ).join(Vehicle)
        
        active_query = db.query(ParkingRecord).filter(
            ParkingRecord.exit_time == None  # Çıkış yapmamış olanlar
        ).join(Vehicle)
        
        # Otopark ID'si filtresi ekle
        if parking_id is not None:
            completed_query = completed_query.filter(ParkingRecord.parking_id == parking_id)
            active_query = active_query.filter(ParkingRecord.parking_id == parking_id)
        
        # En son tamamlanan (çıkış yapılmış) park kayıtlarını getir    
        completed_records = completed_query.order_by(ParkingRecord.exit_time.desc()).limit(limit).all()
        
        # En son giriş yapılmış (aktif) park kayıtlarını getir
        active_records = active_query.order_by(ParkingRecord.entry_time.desc()).limit(limit).all()
        
        result = []
        
        # Çıkış aktiviteleri
        for record in completed_records:
            vehicle = record.vehicle
            
            # Park süresini hesapla (saat olarak)
            duration = (record.exit_time - record.entry_time).total_seconds() / 3600
            
            # Ücret TL'ye çevrilmeli
            fee_tl = float(record.parking_fee) / 100.0 if record.parking_fee else 0
            
            result.append({
                "id": record.id,
                "vehicle_id": vehicle.id,
                "license_plate": vehicle.license_plate,
                "action": "exit",
                "entry_time": record.entry_time.isoformat(),
                "exit_time": record.exit_time.isoformat(),
                "duration_hours": round(duration, 2),
                "parking_fee": round(fee_tl, 2),
                "message": f"Araç çıkışı: {vehicle.license_plate}",
                "parking_id": record.parking_id or 1  # Default 1 olarak ayarla
            })
        
        # Giriş aktiviteleri
        for record in active_records:
            vehicle = record.vehicle
            result.append({
                "id": record.id,
                "vehicle_id": vehicle.id,
                "license_plate": vehicle.license_plate,
                "action": "entry",
                "entry_time": record.entry_time.isoformat(),
                "message": f"Araç girişi: {vehicle.license_plate}",
                "parking_id": record.parking_id or 1  # Default 1 olarak ayarla
            })
        
        # Tarihe göre sırala (en yeni en üstte)
        result.sort(key=lambda x: x.get("exit_time", x.get("entry_time")), reverse=True)
        
        # Sınırla
        return result[:limit]
        
    except Exception as e:
        logger.error(f"Son aktiviteler alınırken hata: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Veritabanı hatası: {str(e)}")