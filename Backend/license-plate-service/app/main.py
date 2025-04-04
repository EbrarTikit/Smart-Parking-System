from fastapi import FastAPI, UploadFile, File, Depends, HTTPException, Query, status
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from typing import List, Optional
from datetime import datetime
import io

from . import models, schemas, crud
from .database import engine , SessionLocal
from .recognition import recognize_license_plate

# Veritabanı tablolarını oluştur
models.Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="License Plate Recognition Service",
    description="Plaka tanıma ve otopark yönetim servisi",
    version="1.0.0"
)

# CORS ayarları
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Bağımlılık
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Plaka tanıma endpoint'i
@app.post("/recognize/", response_model=schemas.LicensePlateResponse)
async def recognize(file: UploadFile = File(...), db: Session = Depends(get_db)):
    try:
        # Dosya içeriğini oku
        contents = await file.read()
        
        # Plaka tanıma işlemini gerçekleştir
        success, result = recognize_license_plate(contents)
        
        if not success:
            return schemas.LicensePlateResponse(
                success=False,
                message=result
            )
        
        # Plaka veritabanında var mı kontrol et
        license_plate = result
        db_vehicle = crud.get_vehicle_by_license_plate(db, license_plate)
        
        # Eğer yoksa yeni araç kaydı oluştur
        if db_vehicle is None:
            vehicle_create = schemas.VehicleCreate(license_plate=license_plate)
            db_vehicle = crud.create_vehicle(db, vehicle_create)
        
        # Başarılı yanıt döndür
        return schemas.LicensePlateResponse(
            success=True,
            license_plate=license_plate,
            message="Plaka başarıyla tanındı"
        )
        
    except Exception as e:
        return schemas.LicensePlateResponse(
            success=False,
            message=f"Hata: {str(e)}"
        )

# Araç giriş kaydı oluşturma endpoint'i
@app.post("/vehicles/{license_plate}/entry", response_model=schemas.ParkingRecord)
def create_entry_record(license_plate: str, db: Session = Depends(get_db)):
    # Aracı bul veya oluştur
    db_vehicle = crud.get_vehicle_by_license_plate(db, license_plate)
    if not db_vehicle:
        vehicle_create = schemas.VehicleCreate(license_plate=license_plate)
        db_vehicle = crud.create_vehicle(db, vehicle_create)
    
    # Aktif park kaydı var mı kontrol et
    active_record = crud.get_active_parking_record_by_vehicle(db, db_vehicle.id)
    if active_record:
        raise HTTPException(
            status_code=400,
            detail="Bu araç zaten otoparkta kayıtlı"
        )
    
    # Yeni park kaydı oluştur
    parking_record = schemas.ParkingRecordCreate(vehicle_id=db_vehicle.id)
    return crud.create_parking_record(db, parking_record)

# Araç çıkış kaydı oluşturma endpoint'i
@app.post("/vehicles/{license_plate}/exit", response_model=schemas.ParkingRecord)
def create_exit_record(license_plate: str, db: Session = Depends(get_db)):
    # Aracı bul
    db_vehicle = crud.get_vehicle_by_license_plate(db, license_plate)
    if not db_vehicle:
        raise HTTPException(
            status_code=404,
            detail="Araç bulunamadı"
        )
    
    # Aktif park kaydı var mı kontrol et
    active_record = crud.get_active_parking_record_by_vehicle(db, db_vehicle.id)
    if not active_record:
        raise HTTPException(
            status_code=400,
            detail="Bu araç için aktif park kaydı bulunamadı"
        )
    
    # Park kaydını kapat
    return crud.close_parking_record(db, active_record.id)

# Araç listesi endpoint'i
@app.get("/vehicles/", response_model=List[schemas.Vehicle])
def read_vehicles(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    vehicles = crud.get_vehicles(db, skip=skip, limit=limit)
    return vehicles

# Araç detayı endpoint'i
@app.get("/vehicles/{license_plate}", response_model=schemas.VehicleWithRecords)
def read_vehicle(license_plate: str, db: Session = Depends(get_db)):
    db_vehicle = crud.get_vehicle_by_license_plate(db, license_plate)
    if db_vehicle is None:
        raise HTTPException(status_code=404, detail="Araç bulunamadı")
    return db_vehicle

# Park kayıtları endpoint'i
@app.get("/parking-records/", response_model=List[schemas.ParkingRecord])
def read_parking_records(
    skip: int = 0, 
    limit: int = 100, 
    active_only: bool = False,
    db: Session = Depends(get_db)
):
    records = crud.get_parking_records(db, skip=skip, limit=limit)
    if active_only:
        records = [r for r in records if r.is_active]
    return records

# Sağlık kontrolü endpoint'i
@app.get("/health")
def health_check():
    return {"status": "ok", "timestamp": datetime.now().isoformat()}