from fastapi import FastAPI, Depends, HTTPException, status, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
import os
import logging
from datetime import datetime
from typing import List, Dict, Any, Optional

# Kendi modüllerimizi içe aktar
from app.sagas.parking_entry_saga import ParkingEntrySagaOrchestrator, VehicleEntry, router as saga_router

# Logger yapılandırması
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# FastAPI uygulaması oluştur
app = FastAPI(
    title="Parking Management Service",
    description="Otopark yönetim servisi ve SAGA orchestrator",
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

# SAGA router'ını uygulama router'ına ekle
app.include_router(saga_router, prefix="/api/v1", tags=["saga"])

# SAGA orchestrator örneği
orchestrator = ParkingEntrySagaOrchestrator(
    base_url=os.getenv("BASE_URL", "http://localhost")
)

# Sağlık kontrolü endpoint'i
@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "timestamp": datetime.now().isoformat(),
        "service": "parking_management_service"
    }

# Test aracı giriş endpoint'i
@app.post("/test/vehicle-entry", status_code=status.HTTP_202_ACCEPTED)
async def test_vehicle_entry(
    license_plate: str,
    user_id: Optional[str] = None,
    background_tasks: BackgroundTasks = None
):
    """Test amaçlı araç giriş işlemi başlatır"""
    try:
        vehicle_entry = VehicleEntry(
            license_plate=license_plate,
            user_id=user_id,
            entry_timestamp=datetime.now()
        )
        
        saga = await orchestrator.start_saga(vehicle_entry)
        
        return {
            "saga_id": saga.saga_id,
            "message": f"Test araç giriş işlemi başlatıldı: {license_plate}",
            "status": saga.status
        }
    except Exception as e:
        logger.exception(f"Test araç giriş işlemi başlatılırken hata: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"İşlem başlatılamadı: {str(e)}"
        )

# Test aracı çıkış endpoint'i
@app.post("/test/vehicle-exit", status_code=status.HTTP_202_ACCEPTED)
async def test_vehicle_exit(
    license_plate: str,
    user_id: Optional[str] = None
):
    """Test amaçlı araç çıkış işlemi başlatır"""
    try:
        saga = await orchestrator.start_vehicle_exit_saga(license_plate, user_id)
        
        return {
            "saga_id": saga.saga_id,
            "message": f"Test araç çıkış işlemi başlatıldı: {license_plate}",
            "status": saga.status
        }
    except Exception as e:
        logger.exception(f"Test araç çıkış işlemi başlatılırken hata: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"İşlem başlatılamadı: {str(e)}"
        ) 