from sqlalchemy import Column, Integer, String, DateTime, Boolean, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from .database import Base

class Vehicle(Base):
    __tablename__ = "vehicles"

    id = Column(Integer, primary_key=True, index=True)
    license_plate = Column(String, unique=True, index=True)
    vehicle_type = Column(String, nullable=True)
    owner_name = Column(String, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    # İlişkiler
    parking_records = relationship("ParkingRecord", back_populates="vehicle")

class ParkingRecord(Base):
    __tablename__ = "parking_records"

    id = Column(Integer, primary_key=True, index=True)
    vehicle_id = Column(Integer, ForeignKey("vehicles.id"))
    entry_time = Column(DateTime(timezone=True), server_default=func.now())
    exit_time = Column(DateTime(timezone=True), nullable=True)
    is_active = Column(Boolean, default=True)
    parking_fee = Column(Integer, nullable=True)  # Kuruş cinsinden
    
    # İlişkiler
    vehicle = relationship("Vehicle", back_populates="parking_records")

class ParkingSpace(Base):
    __tablename__ = "parking_spaces"

    id = Column(Integer, primary_key=True, index=True)
    space_number = Column(String, unique=True, index=True)
    is_occupied = Column(Boolean, default=False)
    vehicle_id = Column(Integer, ForeignKey("vehicles.id"), nullable=True)
    last_updated = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
