from sqlalchemy.orm import Session
from sqlalchemy import and_
from datetime import datetime
from typing import List, Optional
from . import models, schemas

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

def get_active_parking_record_by_vehicle(db: Session, vehicle_id: int):
    return db.query(models.ParkingRecord).filter(
        and_(
            models.ParkingRecord.vehicle_id == vehicle_id,
            models.ParkingRecord.is_active == True
        )
    ).first()

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

def calculate_parking_fee(entry_time: datetime, exit_time: datetime) -> int:
    """Otopark ücretini hesaplar (kuruş cinsinden)"""
    # Örnek ücretlendirme: Saat başına 10 TL (1000 kuruş)
    duration = exit_time - entry_time
    hours = duration.total_seconds() / 3600
    fee = int(hours * 1000)  # 10 TL/saat = 1000 kuruş/saat
    return max(fee, 1000)  # Minimum ücret 10 TL

def close_parking_record(db: Session, record_id: int):
    db_record = get_parking_record(db, record_id)
    if db_record and db_record.is_active:
        exit_time = datetime.now()
        fee = calculate_parking_fee(db_record.entry_time, exit_time)
        
        db_record.exit_time = exit_time
        db_record.is_active = False
        db_record.parking_fee = fee
        
        db.commit()
        db.refresh(db_record)
    return db_record

# Parking Space CRUD operations
def get_parking_space(db: Session, space_id: int):
    return db.query(models.ParkingSpace).filter(models.ParkingSpace.id == space_id).first()

def get_parking_space_by_number(db: Session, space_number: str):
    return db.query(models.ParkingSpace).filter(models.ParkingSpace.space_number == space_number).first()

def get_parking_spaces(db: Session, skip: int = 0, limit: int = 100):
    return db.query(models.ParkingSpace).offset(skip).limit(limit).all()

def get_available_parking_spaces(db: Session):
    return db.query(models.ParkingSpace).filter(models.ParkingSpace.is_occupied == False).all()

def create_parking_space(db: Session, parking_space: schemas.ParkingSpaceCreate):
    db_space = models.ParkingSpace(**parking_space.dict())
    db.add(db_space)
    db.commit()
    db.refresh(db_space)
    return db_space

def update_parking_space(db: Session, space_id: int, parking_space: schemas.ParkingSpaceUpdate):
    db_space = get_parking_space(db, space_id)
    if db_space:
        for key, value in parking_space.dict().items():
            setattr(db_space, key, value)
        db.commit()
        db.refresh(db_space)
    return db_space 