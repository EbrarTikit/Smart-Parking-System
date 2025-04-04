from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

# Vehicle Schemas
class VehicleBase(BaseModel):
    license_plate: str
    vehicle_type: Optional[str] = None
    owner_name: Optional[str] = None

class VehicleCreate(VehicleBase):
    pass

class Vehicle(VehicleBase):
    id: int
    created_at: datetime
    updated_at: Optional[datetime] = None

    class Config:
        orm_mode = True

# Parking Record Schemas
class ParkingRecordBase(BaseModel):
    vehicle_id: int
    
class ParkingRecordCreate(ParkingRecordBase):
    pass

class ParkingRecordUpdate(BaseModel):
    exit_time: datetime
    parking_fee: Optional[int] = None

class ParkingRecord(ParkingRecordBase):
    id: int
    entry_time: datetime
    exit_time: Optional[datetime] = None
    is_active: bool
    parking_fee: Optional[int] = None

    class Config:
        orm_mode = True

# Parking Space Schemas
class ParkingSpaceBase(BaseModel):
    space_number: str
    
class ParkingSpaceCreate(ParkingSpaceBase):
    pass

class ParkingSpaceUpdate(BaseModel):
    is_occupied: bool
    vehicle_id: Optional[int] = None

class ParkingSpace(ParkingSpaceBase):
    id: int
    is_occupied: bool
    vehicle_id: Optional[int] = None
    last_updated: datetime

    class Config:
        orm_mode = True

# License Plate Recognition Schemas
class LicensePlateResponse(BaseModel):
    success: bool
    license_plate: Optional[str] = None
    message: Optional[str] = None

# Vehicle with Parking Records
class VehicleWithRecords(Vehicle):
    parking_records: List[ParkingRecord] = []

    class Config:
        orm_mode = True 