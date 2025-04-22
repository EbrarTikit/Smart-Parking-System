from fastapi import FastAPI, Depends, HTTPException, status, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
import os
import logging
from datetime import datetime
from typing import List, Dict, Any, Optional

from app.sagas.parking_entry_saga import ParkingEntrySagaOrchestrator, VehicleEntry, router as saga_router

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Parking Management Service",
    description="Otopark yönetim servisi ve SAGA orchestrator",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(saga_router, prefix="/api/v1", tags=["saga"])

orchestrator = ParkingEntrySagaOrchestrator(
    base_url=os.getenv("BASE_URL", "http://localhost")
)

@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "timestamp": datetime.now().isoformat(),
        "service": "parking_management_service"
    }

@app.post("/test/vehicle-entry", status_code=status.HTTP_202_ACCEPTED)
async def test_vehicle_entry(
    license_plate: str,
    user_id: Optional[str] = None,
    background_tasks: BackgroundTasks = None
):
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

@app.post("/test/vehicle-exit", status_code=status.HTTP_202_ACCEPTED)
async def test_vehicle_exit(
    license_plate: str,
    user_id: Optional[str] = None
):
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