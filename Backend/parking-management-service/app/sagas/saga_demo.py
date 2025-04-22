#!/usr/bin/env python3

import asyncio
import json
import logging
import random
import sys
import uuid
from datetime import datetime
from typing import Dict, Any, List, Optional, Tuple

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger("saga_demo")


class SagaStep:
    def __init__(self, step_id: str, service_name: str, operation: str):
        self.step_id = step_id
        self.service_name = service_name
        self.operation = operation
        self.status = "pending"
        self.compensation_required = True
        self.request_data = {}
        self.response_data = None
        self.error = None
        self.timestamp = datetime.now()
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "step_id": self.step_id,
            "service_name": self.service_name,
            "operation": self.operation,
            "status": self.status,
            "compensation_required": self.compensation_required,
            "request_data": self.request_data,
            "response_data": self.response_data,
            "error": self.error,
            "timestamp": self.timestamp.isoformat()
        }


class Saga:
    def __init__(self, saga_id: str, description: str):
        self.saga_id = saga_id
        self.description = description
        self.start_time = datetime.now()
        self.end_time = None
        self.status = "in_progress"
        self.steps: List[SagaStep] = []
        self.current_step_index = 0
        self.metadata = {}

    def add_step(self, step: SagaStep) -> None:
        self.steps.append(step)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "saga_id": self.saga_id,
            "description": self.description,
            "start_time": self.start_time.isoformat(),
            "end_time": self.end_time.isoformat() if self.end_time else None,
            "status": self.status,
            "current_step_index": self.current_step_index,
            "steps": [step.to_dict() for step in self.steps],
            "metadata": self.metadata
        }


class MockService:
    def __init__(self, name: str, failure_rate: float = 0.2):
        self.name = name
        self.failure_rate = failure_rate
        self.logger = logging.getLogger(f"service.{name}")
    
    async def call(self, operation: str, data: Dict[str, Any]) -> Tuple[bool, Dict[str, Any]]:
        self.logger.info(f"Çağrılıyor: {operation}, veri: {json.dumps(data)}")
        
        if random.random() < self.failure_rate:
            self.logger.error(f"Başarısız operasyon: {operation}")
            return False, {"error": f"{self.name} servisi başarısız oldu: {operation}"}
        
        await asyncio.sleep(random.uniform(0.1, 0.5))
        
        response = self._generate_response(operation, data)
        self.logger.info(f"Başarılı yanıt: {operation}")
        return True, response
    
    def _generate_response(self, operation: str, data: Dict[str, Any]) -> Dict[str, Any]:
        return {"status": "success", "operation": operation}


class LicensePlateService(MockService):
    def __init__(self, failure_rate: float = 0.2):
        super().__init__("license_plate_service", failure_rate)
    
    def _generate_response(self, operation: str, data: Dict[str, Any]) -> Dict[str, Any]:
        if operation == "verify_license_plate":
            return {
                "success": True,
                "license_plate": data.get("license_plate", ""),
                "verified": True
            }
        elif operation == "create_entry_record":
            return {
                "record_id": str(uuid.uuid4()),
                "license_plate": data.get("license_plate", ""),
                "entry_time": datetime.now().isoformat(),
                "status": "active"
            }
        elif operation == "calculate_parking_fee":
            hours = random.randint(1, 5)
            hourly_rate = 20
            fee = hours * hourly_rate
            return {
                "record_id": str(uuid.uuid4()),
                "license_plate": data.get("license_plate", ""),
                "entry_time": (datetime.now().replace(hour=datetime.now().hour - hours)).isoformat(),
                "exit_time": datetime.now().isoformat(),
                "duration_hours": hours,
                "fee_amount": fee,
                "fee_currency": "TRY"
            }
        return super()._generate_response(operation, data)


class UserService(MockService):
    def __init__(self, failure_rate: float = 0.2):
        super().__init__("user_service", failure_rate)
    
    def _generate_response(self, operation: str, data: Dict[str, Any]) -> Dict[str, Any]:
        if operation == "check_subscription":
            user_id = data.get("user_id")
            if not user_id:
                return {"subscription": None}
                
            return {
                "user_id": user_id,
                "subscription": {
                    "type": random.choice(["free", "premium", "business"]),
                    "active": True,
                    "expiry_date": (datetime.now().replace(month=12, day=31)).isoformat()
                }
            }
        elif operation == "update_favorite_parking":
            return {
                "user_id": data.get("user_id", ""),
                "favorites_updated": True,
                "favorite_count": random.randint(1, 5)
            }
        elif operation == "update_parking_history":
            return {
                "user_id": data.get("user_id", ""),
                "history_updated": True,
                "parking_count": random.randint(1, 20)
            }
        return super()._generate_response(operation, data)


class ParkingManagementService(MockService):
    def __init__(self, failure_rate: float = 0.2):
        super().__init__("parking_mgmt_service", failure_rate)
    
    def _generate_response(self, operation: str, data: Dict[str, Any]) -> Dict[str, Any]:
        if operation == "allocate_parking_space":
            return {
                "allocation_id": str(uuid.uuid4()),
                "license_plate": data.get("license_plate", ""),
                "parking_space": f"A-{random.randint(1, 100)}",
                "timestamp": datetime.now().isoformat(),
                "occupancy_rate": f"{random.randint(60, 95)}%"
            }
        elif operation == "update_occupancy_status":
            return {
                "parking_id": "parking-001",
                "total_spaces": 100,
                "occupied_spaces": random.randint(50, 95),
                "occupancy_rate": f"{random.randint(50, 95)}%",
                "updated_at": datetime.now().isoformat()
            }
        return super()._generate_response(operation, data)


class NotificationService(MockService):
    def __init__(self, failure_rate: float = 0.2):
        super().__init__("notification_service", failure_rate)
    
    def _generate_response(self, operation: str, data: Dict[str, Any]) -> Dict[str, Any]:
        if operation == "send_entry_notification":
            channels = []
            if data.get("user_id"):
                channels = random.sample(["push", "email", "sms"], k=random.randint(1, 3))
            
            return {
                "notification_id": str(uuid.uuid4()),
                "recipient": data.get("user_id", "anonymous"),
                "license_plate": data.get("license_plate", ""),
                "channels": channels,
                "sent_at": datetime.now().isoformat()
            }
        elif operation == "send_exit_notification":
            channels = []
            if data.get("user_id"):
                channels = random.sample(["push", "email", "sms"], k=random.randint(1, 3))
            
            return {
                "notification_id": str(uuid.uuid4()),
                "recipient": data.get("user_id", "anonymous"),
                "license_plate": data.get("license_plate", ""),
                "fee_amount": data.get("fee_amount", 0),
                "channels": channels,
                "sent_at": datetime.now().isoformat()
            }
        return super()._generate_response(operation, data)


class SagaOrchestrator:
    def __init__(self):
        self.services = {
            "license_plate_service": LicensePlateService(),
            "user_service": UserService(),
            "parking_mgmt_service": ParkingManagementService(failure_rate=0.4),
            "notification_service": NotificationService()
        }
        self.active_sagas: Dict[str, Saga] = {}
        self.logger = logging.getLogger("saga_orchestrator")
    
    async def execute_saga(self, saga: Saga) -> Saga:
        self.active_sagas[saga.saga_id] = saga
        self.logger.info(f"SAGA başlatıldı: {saga.saga_id} - {saga.description}")
        
        try:
            while saga.current_step_index < len(saga.steps):
                current_step = saga.steps[saga.current_step_index]
                self.logger.info(f"SAGA adımı çalıştırılıyor: {current_step.step_id}")
                
                service = self.services.get(current_step.service_name)
                if not service:
                    current_step.status = "failed"
                    current_step.error = f"Servis bulunamadı: {current_step.service_name}"
                    saga.status = "failed"
                    saga.end_time = datetime.now()
                    self.logger.error(f"SAGA adımı başarısız: {current_step.step_id} - Servis bulunamadı")
                    await self._compensate_saga(saga)
                    return saga
                
                success, response = await service.call(current_step.operation, current_step.request_data)
                
                if success:
                    current_step.status = "success"
                    current_step.response_data = response
                    saga.current_step_index += 1
                    self.logger.info(f"SAGA adımı başarılı: {current_step.step_id}")
                else:
                    current_step.status = "failed"
                    current_step.error = str(response)
                    self.logger.error(f"SAGA adımı başarısız: {current_step.step_id} - Hata: {response}")
                    await self._compensate_saga(saga)
                    saga.status = "failed"
                    saga.end_time = datetime.now()
                    return saga
            
            saga.status = "completed"
            saga.end_time = datetime.now()
            self.logger.info(f"SAGA başarıyla tamamlandı: {saga.saga_id}")
            
        except Exception as e:
            self.logger.exception(f"SAGA çalıştırılırken hata: {e}")
            saga.status = "failed"
            saga.end_time = datetime.now()
            await self._compensate_saga(saga)
        
        return saga
    
    async def _compensate_saga(self, saga: Saga) -> None:
        self.logger.info(f"SAGA telafi işlemleri başlatılıyor: {saga.saga_id}")
        
        compensation_steps = [
            step for step in saga.steps[:saga.current_step_index] 
            if step.status == "success" and step.compensation_required
        ]
        
        for step in reversed(compensation_steps):
            self.logger.info(f"Telafi işlemi: {step.step_id}")
            
            service = self.services.get(step.service_name)
            if not service:
                self.logger.error(f"Telafi işlemi başarısız: {step.step_id} - Servis bulunamadı")
                continue
            
            compensation_op = f"compensate_{step.operation}"
            compensation_data = {
                "original_operation": step.operation,
                "original_request": step.request_data,
                "original_response": step.response_data
            }
            
            try:
                success, response = await service.call(compensation_op, compensation_data)
                if success:
                    self.logger.info(f"Telafi işlemi başarılı: {step.step_id}")
                else:
                    self.logger.error(f"Telafi işlemi başarısız: {step.step_id} - Hata: {response}")
            except Exception as e:
                self.logger.exception(f"Telafi işlemi sırasında hata: {step.step_id} - {str(e)}")


async def run_successful_saga():
    logger.info("=== BAŞARILI SAGA SENARYOSU BAŞLIYOR ===")
    
    orchestrator = SagaOrchestrator()
    
    for service in orchestrator.services.values():
        service.failure_rate = 0.1
    
    saga = Saga(str(uuid.uuid4()), "Başarılı Araç Giriş İşlemi")
    
    saga.add_step(SagaStep("verify_license_plate", "license_plate_service", "verify_license_plate"))
    saga.steps[-1].request_data = {"license_plate": "34ABC123"}
    
    saga.add_step(SagaStep("check_user_subscription", "user_service", "check_subscription"))
    saga.steps[-1].request_data = {"user_id": "user123"}
    
    saga.add_step(SagaStep("allocate_parking_space", "parking_mgmt_service", "allocate_parking_space"))
    saga.steps[-1].request_data = {"license_plate": "34ABC123"}
    
    saga.add_step(SagaStep("create_entry_record", "license_plate_service", "create_entry_record"))
    saga.steps[-1].request_data = {"license_plate": "34ABC123"}
    
    saga.add_step(SagaStep("send_notification", "notification_service", "send_entry_notification"))
    saga.steps[-1].request_data = {
        "user_id": "user123", 
        "license_plate": "34ABC123", 
        "entry_time": datetime.now().isoformat()
    }
    
    result_saga = await orchestrator.execute_saga(saga)
    
    logger.info(f"SAGA sonucu: {result_saga.status}")
    logger.info("=== BAŞARILI SAGA SENARYOSU TAMAMLANDI ===\n")


async def run_failing_saga():
    logger.info("=== BAŞARISIZ SAGA SENARYOSU BAŞLIYOR ===")
    
    orchestrator = SagaOrchestrator()
    
    orchestrator.services["parking_mgmt_service"].failure_rate = 0.9
    
    saga = Saga(str(uuid.uuid4()), "Başarısız Araç Giriş İşlemi")
    
    saga.add_step(SagaStep("verify_license_plate", "license_plate_service", "verify_license_plate"))
    saga.steps[-1].request_data = {"license_plate": "34XYZ789"}
    
    saga.add_step(SagaStep("check_user_subscription", "user_service", "check_subscription"))
    saga.steps[-1].request_data = {"user_id": "user456"}
    
    saga.add_step(SagaStep("allocate_parking_space", "parking_mgmt_service", "allocate_parking_space"))
    saga.steps[-1].request_data = {"license_plate": "34XYZ789"}
    
    saga.add_step(SagaStep("create_entry_record", "license_plate_service", "create_entry_record"))
    saga.steps[-1].request_data = {"license_plate": "34XYZ789"}
    
    saga.add_step(SagaStep("send_notification", "notification_service", "send_entry_notification"))
    saga.steps[-1].request_data = {
        "user_id": "user456", 
        "license_plate": "34XYZ789", 
        "entry_time": datetime.now().isoformat()
    }
    
    result_saga = await orchestrator.execute_saga(saga)
    
    logger.info(f"SAGA sonucu: {result_saga.status}")
    logger.info(f"Hata nedeni: {[s.error for s in result_saga.steps if s.status == 'failed']}")
    logger.info("=== BAŞARISIZ SAGA SENARYOSU TAMAMLANDI ===\n")


async def run_vehicle_exit_saga():
    logger.info("=== ARAÇ ÇIKIŞ SAGA SENARYOSU BAŞLIYOR ===")
    
    orchestrator = SagaOrchestrator()
    
    for service in orchestrator.services.values():
        service.failure_rate = 0.1
    
    saga = Saga(str(uuid.uuid4()), "Araç Çıkış İşlemi")
    
    saga.add_step(SagaStep("verify_license_plate", "license_plate_service", "verify_license_plate"))
    saga.steps[-1].request_data = {"license_plate": "34DEF456"}
    
    saga.add_step(SagaStep("calculate_parking_fee", "license_plate_service", "calculate_parking_fee"))
    saga.steps[-1].request_data = {"license_plate": "34DEF456"}
    
    saga.add_step(SagaStep("update_parking_history", "user_service", "update_parking_history"))
    saga.steps[-1].request_data = {"user_id": "user789", "license_plate": "34DEF456"}
    
    saga.add_step(SagaStep("update_occupancy_status", "parking_mgmt_service", "update_occupancy_status"))
    saga.steps[-1].request_data = {"parking_id": "parking-001"}
    
    saga.add_step(SagaStep("send_exit_notification", "notification_service", "send_exit_notification"))
    saga.steps[-1].request_data = {
        "user_id": "user789", 
        "license_plate": "34DEF456", 
        "exit_time": datetime.now().isoformat(),
        "fee_amount": 100
    }
    
    result_saga = await orchestrator.execute_saga(saga)
    
    logger.info(f"SAGA sonucu: {result_saga.status}")
    for step in result_saga.steps:
        logger.info(f"Adım {step.step_id}: {step.status}")
    logger.info("=== ARAÇ ÇIKIŞ SAGA SENARYOSU TAMAMLANDI ===\n")


async def main():
    logger.info("SAGA Pattern Demo Başlıyor...")
    
    await run_successful_saga()
    
    await run_failing_saga()
    
    await run_vehicle_exit_saga()
    
    logger.info("SAGA Pattern Demo Tamamlandı.")


if __name__ == "__main__":
    asyncio.run(main()) 