from fastapi import FastAPI, HTTPException, status, BackgroundTasks
import asyncio
import httpx
import logging
import json
from pydantic import BaseModel
from typing import Optional, Dict, Any, List
from datetime import datetime
import uuid

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class VehicleEntry(BaseModel):
    license_plate: str
    entry_timestamp: datetime = datetime.now()
    user_id: Optional[str] = None
    is_favorite: bool = False

class SagaStep(BaseModel):
    step_id: str
    service_name: str
    operation: str
    status: str = "pending"
    compensation_required: bool = False
    request_data: Dict[str, Any] = {}
    response_data: Optional[Dict[str, Any]] = None
    error: Optional[str] = None
    timestamp: datetime = datetime.now()

class ParkingEntrySaga(BaseModel):
    saga_id: str = str(uuid.uuid4())
    license_plate: str
    user_id: Optional[str] = None
    start_time: datetime = datetime.now()
    end_time: Optional[datetime] = None
    status: str = "in_progress"
    steps: List[SagaStep] = []
    current_step_index: int = 0

class ParkingEntrySagaOrchestrator:
    def __init__(self, base_url: str = "http://localhost"):
        self.license_plate_service_url = f"{base_url}:8002"
        self.user_service_url = f"{base_url}:8003"
        self.parking_mgmt_service_url = f"{base_url}:8004"
        self.notification_service_url = f"{base_url}:8005"
        self.active_sagas: Dict[str, ParkingEntrySaga] = {}

    async def start_saga(self, vehicle_entry: VehicleEntry) -> ParkingEntrySaga:
        saga = ParkingEntrySaga(license_plate=vehicle_entry.license_plate, user_id=vehicle_entry.user_id)
        
        saga.steps = [
            SagaStep(
                step_id="verify_license_plate",
                service_name="license_plate_service",
                operation="verify_license_plate",
                request_data={"license_plate": vehicle_entry.license_plate}
            ),
            SagaStep(
                step_id="check_user_subscription",
                service_name="user_service",
                operation="check_subscription",
                request_data={"user_id": vehicle_entry.user_id} if vehicle_entry.user_id else {}
            ),
            SagaStep(
                step_id="update_parking_occupancy",
                service_name="parking_mgmt_service",
                operation="allocate_parking_space",
                request_data={"license_plate": vehicle_entry.license_plate}
            ),
            SagaStep(
                step_id="create_entry_record",
                service_name="license_plate_service",
                operation="create_entry_record",
                request_data={"license_plate": vehicle_entry.license_plate}
            ),
            SagaStep(
                step_id="send_entry_notification",
                service_name="notification_service",
                operation="send_entry_notification",
                request_data={
                    "user_id": vehicle_entry.user_id if vehicle_entry.user_id else None,
                    "license_plate": vehicle_entry.license_plate,
                    "entry_time": vehicle_entry.entry_timestamp.isoformat()
                }
            ),
            SagaStep(
                step_id="update_favorite_if_needed",
                service_name="user_service",
                operation="update_favorite_parking",
                request_data={
                    "user_id": vehicle_entry.user_id,
                    "license_plate": vehicle_entry.license_plate,
                    "is_favorite": vehicle_entry.is_favorite
                }
            )
        ]
        
        self.active_sagas[saga.saga_id] = saga
        logger.info(f"SAGA başlatıldı: {saga.saga_id} - Plaka: {vehicle_entry.license_plate}")
        
        asyncio.create_task(self._execute_saga(saga.saga_id))
        
        return saga
    
    async def start_vehicle_exit_saga(self, license_plate: str, user_id: Optional[str] = None) -> ParkingEntrySaga:
        saga = ParkingEntrySaga(license_plate=license_plate, user_id=user_id)
        saga.status = "in_progress"
        
        saga.steps = [
            SagaStep(
                step_id="verify_license_plate",
                service_name="license_plate_service",
                operation="verify_license_plate",
                request_data={"license_plate": license_plate}
            ),
            SagaStep(
                step_id="calculate_parking_fee",
                service_name="license_plate_service",
                operation="calculate_parking_fee",
                request_data={"license_plate": license_plate}
            ),
            SagaStep(
                step_id="update_parking_history",
                service_name="user_service", 
                operation="update_parking_history",
                request_data={"user_id": user_id, "license_plate": license_plate} if user_id else {}
            ),
            SagaStep(
                step_id="update_occupancy_status",
                service_name="parking_mgmt_service",
                operation="update_occupancy_status",
                request_data={"license_plate": license_plate}
            ),
            SagaStep(
                step_id="send_exit_notification",
                service_name="notification_service",
                operation="send_exit_notification",
                request_data={
                    "user_id": user_id,
                    "license_plate": license_plate,
                    "exit_time": datetime.now().isoformat()
                }
            )
        ]
        
        self.active_sagas[saga.saga_id] = saga
        logger.info(f"Araç çıkış SAGA başlatıldı: {saga.saga_id} - Plaka: {license_plate}")
        
        asyncio.create_task(self._execute_saga(saga.saga_id))
        
        return saga

    async def _execute_saga(self, saga_id: str) -> None:
        saga = self.active_sagas.get(saga_id)
        if not saga:
            logger.error(f"SAGA bulunamadı: {saga_id}")
            return
        
        try:
            while saga.current_step_index < len(saga.steps):
                current_step = saga.steps[saga.current_step_index]
                logger.info(f"SAGA adımı çalıştırılıyor: {current_step.step_id}")
                
                success, response = await self._execute_step(current_step)
                
                if success:
                    current_step.status = "success"
                    current_step.response_data = response
                    saga.current_step_index += 1
                    logger.info(f"SAGA adımı başarılı: {current_step.step_id}")
                    
                    if current_step.step_id == "calculate_parking_fee" and response.get("fee_amount"):
                        for next_step in saga.steps[saga.current_step_index:]:
                            if next_step.step_id == "send_exit_notification":
                                next_step.request_data["fee_amount"] = response.get("fee_amount")
                                next_step.request_data["duration_hours"] = response.get("duration_hours")
                                break
                else:
                    current_step.status = "failed"
                    current_step.error = str(response)
                    logger.error(f"SAGA adımı başarısız: {current_step.step_id} - Hata: {response}")
                    await self._compensate_saga(saga)
                    saga.status = "failed"
                    saga.end_time = datetime.now()
                    return
            
            saga.status = "completed"
            saga.end_time = datetime.now()
            logger.info(f"SAGA başarıyla tamamlandı: {saga_id}")
            
        except Exception as e:
            logger.exception(f"SAGA çalıştırılırken hata: {e}")
            saga.status = "failed"
            saga.end_time = datetime.now()
            await self._compensate_saga(saga)

    async def _execute_step(self, step: SagaStep) -> tuple[bool, Any]:
        service_url = getattr(self, f"{step.service_name}_url", None)
        if not service_url:
            return False, f"Servis URL'i bulunamadı: {step.service_name}"
        
        endpoint = ""
        method = "POST"
        
        if step.service_name == "license_plate_service":
            if step.operation == "verify_license_plate":
                endpoint = "/recognize/"
                method = "GET"
                endpoint = f"/vehicles/{step.request_data.get('license_plate')}"
            elif step.operation == "create_entry_record":
                endpoint = f"/vehicles/{step.request_data.get('license_plate')}/entry"
            elif step.operation == "calculate_parking_fee":
                endpoint = f"/vehicles/{step.request_data.get('license_plate')}/exit"
                
        elif step.service_name == "user_service":
            if step.operation == "check_subscription":
                user_id = step.request_data.get("user_id")
                if not user_id:
                    return True, {"subscription": None}
                endpoint = f"/users/{user_id}/subscription"
                method = "GET"
            elif step.operation == "update_favorite_parking":
                user_id = step.request_data.get("user_id")
                if not user_id:
                    return True, {}
                endpoint = f"/users/{user_id}/favorites"
            elif step.operation == "update_parking_history":
                user_id = step.request_data.get("user_id")
                if not user_id:
                    return True, {}
                endpoint = f"/users/{user_id}/history"
                
        elif step.service_name == "parking_mgmt_service":
            if step.operation == "allocate_parking_space":
                endpoint = "/parking-spaces/allocate"
            elif step.operation == "update_occupancy_status":
                endpoint = "/parking-spaces/update-occupancy"
                
        elif step.service_name == "notification_service":
            if step.operation == "send_entry_notification":
                endpoint = "/notifications/entry"
            elif step.operation == "send_exit_notification":
                endpoint = "/notifications/exit"
        
        try:
            async with httpx.AsyncClient() as client:
                url = f"{service_url}{endpoint}"
                logger.info(f"İstek gönderiliyor: {method} {url}")
                
                if method == "GET":
                    response = await client.get(url, timeout=10.0)
                else:
                    response = await client.post(url, json=step.request_data, timeout=10.0)
                
                if response.status_code < 200 or response.status_code >= 300:
                    return False, f"HTTP hata kodu: {response.status_code}, Yanıt: {response.text}"
                
                return True, response.json()
                
        except Exception as e:
            logger.exception(f"Adım çalıştırılırken hata: {e}")
            return False, str(e)

    async def _compensate_saga(self, saga: ParkingEntrySaga) -> None:
        logger.info(f"SAGA telafi işlemleri başlatılıyor: {saga.saga_id}")
        
        compensation_steps = [
            step for step in saga.steps[:saga.current_step_index] 
            if step.status == "success" and step.compensation_required
        ]
        
        for step in reversed(compensation_steps):
            logger.info(f"Telafi işlemi: {step.step_id}")
            
            if step.service_name == "license_plate_service" and step.operation == "create_entry_record":
                license_plate = step.request_data.get("license_plate")
                await self._compensate_entry_record(license_plate)
                
            elif step.service_name == "parking_mgmt_service" and step.operation == "allocate_parking_space":
                await self._compensate_parking_allocation(step.response_data)
    
    async def _compensate_entry_record(self, license_plate: str) -> None:
        try:
            async with httpx.AsyncClient() as client:
                url = f"{self.license_plate_service_url}/vehicles/{license_plate}/cancel-entry"
                logger.info(f"Giriş kaydı iptal ediliyor: {license_plate}")
                response = await client.post(url, timeout=10.0)
                logger.info(f"Giriş kaydı iptal yanıtı: {response.status_code}")
        except Exception as e:
            logger.error(f"Giriş kaydı iptal edilirken hata: {e}")
    
    async def _compensate_parking_allocation(self, allocation_data: Dict[str, Any]) -> None:
        try:
            async with httpx.AsyncClient() as client:
                url = f"{self.parking_mgmt_service_url}/parking-spaces/release"
                logger.info(f"Park yeri tahsisi iptal ediliyor")
                response = await client.post(url, json=allocation_data, timeout=10.0)
                logger.info(f"Park yeri tahsisi iptal yanıtı: {response.status_code}")
        except Exception as e:
            logger.error(f"Park yeri tahsisi iptal edilirken hata: {e}")

    def get_saga_status(self, saga_id: str) -> Optional[ParkingEntrySaga]:
        return self.active_sagas.get(saga_id)
    
    def get_all_sagas(self) -> List[ParkingEntrySaga]:
        return list(self.active_sagas.values())
    
    def cleanup_completed_sagas(self, age_hours: int = 24) -> None:
        current_time = datetime.now()
        to_remove = []
        
        for saga_id, saga in self.active_sagas.items():
            if saga.status in ["completed", "failed"] and saga.end_time:
                age = (current_time - saga.end_time).total_seconds() / 3600
                if age > age_hours:
                    to_remove.append(saga_id)
        
        for saga_id in to_remove:
            del self.active_sagas[saga_id]
            logger.info(f"Eski SAGA temizlendi: {saga_id}")

orchestrator = ParkingEntrySagaOrchestrator()

router = FastAPI()

@router.post("/parking/entry", status_code=status.HTTP_202_ACCEPTED)
async def start_parking_entry_saga(
    entry: VehicleEntry, 
    background_tasks: BackgroundTasks
):
    try:
        saga = await orchestrator.start_saga(entry)
        return {
            "saga_id": saga.saga_id,
            "message": "Araç giriş işlemi başlatıldı",
            "status": saga.status
        }
    except Exception as e:
        logger.exception(f"SAGA başlatılırken hata: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"SAGA başlatılamadı: {str(e)}"
        )

@router.post("/parking/exit", status_code=status.HTTP_202_ACCEPTED)
async def start_parking_exit_saga(
    license_plate: str,
    user_id: Optional[str] = None
):
    try:
        saga = await orchestrator.start_vehicle_exit_saga(license_plate, user_id)
        return {
            "saga_id": saga.saga_id,
            "message": "Araç çıkış işlemi başlatıldı",
            "status": saga.status
        }
    except Exception as e:
        logger.exception(f"Araç çıkış SAGA başlatılırken hata: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"SAGA başlatılamadı: {str(e)}"
        )

@router.get("/parking/entry/{saga_id}")
async def get_saga_status(saga_id: str):
    saga = orchestrator.get_saga_status(saga_id)
    if not saga:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"SAGA bulunamadı: {saga_id}"
        )
    
    step_details = []
    for step in saga.steps:
        step_details.append({
            "step_id": step.step_id,
            "service": step.service_name,
            "operation": step.operation,
            "status": step.status,
            "error": step.error,
        })
    
    return {
        "saga_id": saga.saga_id,
        "license_plate": saga.license_plate,
        "status": saga.status,
        "start_time": saga.start_time,
        "end_time": saga.end_time,
        "steps": step_details,
        "current_step": saga.steps[saga.current_step_index].step_id if saga.current_step_index < len(saga.steps) else None
    }

@router.get("/parking/sagas")
async def list_sagas():
    sagas = orchestrator.get_all_sagas()
    return {
        "sagas": [
            {
                "saga_id": saga.saga_id,
                "license_plate": saga.license_plate,
                "status": saga.status,
                "start_time": saga.start_time
            }
            for saga in sagas
        ],
        "total": len(sagas)
    }

@router.post("/parking/cleanup")
async def cleanup_sagas(age_hours: int = 24):
    orchestrator.cleanup_completed_sagas(age_hours)
    return {"message": f"{age_hours} saatten eski tamamlanmış SAGA'lar temizlendi"} 