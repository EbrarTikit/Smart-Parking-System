import asyncio
import logging
import json
from datetime import datetime
from typing import Dict, Any, List, Optional, Tuple
import random
import uuid

from fallback_strategies import (
    retry_with_backoff,
    CircuitBreaker,
    AlternativeServiceStrategy,
    CacheFallbackStrategy,
    LoadBalancingStrategy
)

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class EnhancedSagaStep:
    
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
        
        self.retry_count = 3
        self.use_circuit_breaker = True
        self.use_cache_fallback = False
        self.alternative_services = []
        self.load_balanced_services = {}
        
        self.execution_time_ms = 0
        self.retry_attempts = 0
        self.fallback_mechanism_used = None

class EnhancedSaga:
    
    def __init__(self, saga_id: str, description: str):
        self.saga_id = saga_id
        self.description = description
        self.start_time = datetime.now()
        self.end_time = None
        self.status = "in_progress"
        self.steps: List[EnhancedSagaStep] = []
        self.current_step_index = 0
        self.metadata = {}
        self.telemetry = {
            "total_execution_time_ms": 0,
            "total_retry_attempts": 0,
            "fallback_mechanisms_used": [],
            "compensation_steps_executed": 0
        }
    
    def add_step(self, step: EnhancedSagaStep) -> None:
        self.steps.append(step)

async def license_plate_service(operation: str, data: Dict[str, Any]) -> Dict[str, Any]:
    if random.random() < 0.2:
        raise Exception(f"Plaka Tanıma Servisi hatası: {operation}")
        
    if operation == "verify_license_plate":
        await asyncio.sleep(0.1)
        return {
            "success": True,
            "license_plate": data["license_plate"],
            "verified": True
        }
    elif operation == "create_entry_record":
        await asyncio.sleep(0.2)
        return {
            "record_id": str(uuid.uuid4()),
            "license_plate": data["license_plate"],
            "entry_time": datetime.now().isoformat(),
            "status": "active"
        }
    elif operation == "cancel_entry_record":
        await asyncio.sleep(0.1)
        return {
            "success": True,
            "license_plate": data["license_plate"],
            "message": "Giriş kaydı iptal edildi"
        }
    else:
        raise ValueError(f"Bilinmeyen operasyon: {operation}")

async def alternative_license_plate_service(operation: str, data: Dict[str, Any]) -> Dict[str, Any]:
    await asyncio.sleep(0.5)
    
    if operation == "verify_license_plate":
        return {
            "success": True,
            "license_plate": data["license_plate"],
            "verified": True,
            "source": "alternative_service"
        }
    elif operation == "create_entry_record":
        return {
            "record_id": str(uuid.uuid4()),
            "license_plate": data["license_plate"],
            "entry_time": datetime.now().isoformat(),
            "status": "active",
            "source": "alternative_service"
        }
    else:
        raise ValueError(f"Bilinmeyen operasyon: {operation}")

async def parking_mgmt_service(operation: str, data: Dict[str, Any]) -> Dict[str, Any]:
    if random.random() < 0.3:
        raise Exception(f"Otopark Yönetim Servisi hatası: {operation}")
        
    if operation == "allocate_parking_space":
        await asyncio.sleep(0.3)
        return {
            "allocation_id": str(uuid.uuid4()),
            "license_plate": data["license_plate"],
            "parking_space": f"A-{random.randint(1, 100)}",
            "timestamp": datetime.now().isoformat(),
            "occupancy_rate": f"{random.randint(60, 95)}%"
        }
    elif operation == "release_parking_space":
        await asyncio.sleep(0.2)
        return {
            "success": True,
            "license_plate": data["license_plate"],
            "message": "Park yeri serbest bırakıldı"
        }
    else:
        raise ValueError(f"Bilinmeyen operasyon: {operation}")

class EnhancedSagaOrchestrator:
    
    def __init__(self):
        self.active_sagas = {}
        self.logger = logging.getLogger("enhanced_saga_orchestrator")
        
        self.circuit_breakers = {
            "license_plate_service": CircuitBreaker("license_plate_service", failure_threshold=5),
            "parking_mgmt_service": CircuitBreaker("parking_mgmt_service", failure_threshold=3)
        }
        
        self.cache_strategies = {
            "license_plate_service": CacheFallbackStrategy(cache_ttl_seconds=300),
            "parking_mgmt_service": CacheFallbackStrategy(cache_ttl_seconds=120)
        }
        
        self.alternative_strategies = {
            "license_plate_service": AlternativeServiceStrategy(
                "license_plate_service", 
                ["alternative_license_plate_service"]
            )
        }
        
    async def execute_saga(self, saga: EnhancedSaga) -> EnhancedSaga:
        self.active_sagas[saga.saga_id] = saga
        start_time = datetime.now()
        self.logger.info(f"SAGA başlatıldı: {saga.saga_id} - {saga.description}")
        
        try:
            while saga.current_step_index < len(saga.steps):
                current_step = saga.steps[saga.current_step_index]
                step_start_time = datetime.now()
                self.logger.info(f"SAGA adımı çalıştırılıyor: {current_step.step_id}")
                
                success, response = await self._execute_step_with_fallbacks(current_step)
                
                step_execution_time = (datetime.now() - step_start_time).total_seconds() * 1000
                current_step.execution_time_ms = step_execution_time
                
                if success:
                    current_step.status = "success"
                    current_step.response_data = response
                    saga.current_step_index += 1
                    self.logger.info(f"SAGA adımı başarılı: {current_step.step_id} ({step_execution_time:.2f}ms)")
                else:
                    current_step.status = "failed"
                    current_step.error = str(response)
                    self.logger.error(f"SAGA adımı başarısız: {current_step.step_id} - Hata: {response}")
                    await self._compensate_saga(saga)
                    saga.status = "failed"
                    saga.end_time = datetime.now()
                    saga.telemetry["total_execution_time_ms"] = (saga.end_time - start_time).total_seconds() * 1000
                    return saga
            
            saga.status = "completed"
            saga.end_time = datetime.now()
            saga.telemetry["total_execution_time_ms"] = (saga.end_time - start_time).total_seconds() * 1000
            self.logger.info(f"SAGA başarıyla tamamlandı: {saga.saga_id} ({saga.telemetry['total_execution_time_ms']:.2f}ms)")
            
        except Exception as e:
            self.logger.exception(f"SAGA çalıştırılırken hata: {e}")
            saga.status = "failed"
            saga.end_time = datetime.now()
            saga.telemetry["total_execution_time_ms"] = (saga.end_time - start_time).total_seconds() * 1000
            await self._compensate_saga(saga)
        
        return saga
    
    async def _execute_step_with_fallbacks(self, step: EnhancedSagaStep) -> Tuple[bool, Any]:
        service_func = None
        if step.service_name == "license_plate_service":
            service_func = license_plate_service
        elif step.service_name == "parking_mgmt_service":
            service_func = parking_mgmt_service
        else:
            return False, f"Bilinmeyen servis: {step.service_name}"
        
        async def call_service():
            return await service_func(step.operation, step.request_data)
            
        if step.retry_count > 0:
            success, result = await retry_with_backoff(
                call_service,
                retry_count=step.retry_count,
                initial_delay=0.5,
                backoff_factor=2.0
            )
            
            step.retry_attempts = step.retry_count - (0 if success else 0)
            
            if success:
                step.fallback_mechanism_used = "retry"
                return True, result
                
            self.logger.warning(f"Retry with backoff başarısız: {step.step_id}")
        
        if step.use_circuit_breaker and step.service_name in self.circuit_breakers:
            circuit_breaker = self.circuit_breakers[step.service_name]
            
            try:
                success, result = await circuit_breaker.execute(service_func, step.operation, step.request_data)
                
                if success:
                    step.fallback_mechanism_used = "circuit_breaker"
                    return True, result
                    
                self.logger.warning(f"Circuit breaker başarısız: {step.step_id}")
                
            except Exception as e:
                self.logger.exception(f"Circuit breaker çalıştırılırken hata: {e}")
        
        if step.service_name in self.alternative_strategies:
            alt_strategy = self.alternative_strategies[step.service_name]
            
            alternative_funcs = []
            if step.service_name == "license_plate_service":
                alternative_funcs = [
                    lambda: alternative_license_plate_service(step.operation, step.request_data)
                ]
            
            if alternative_funcs:
                try:
                    success, result = await alt_strategy.execute(
                        call_service,
                        alternative_funcs,
                    )
                    
                    if success:
                        step.fallback_mechanism_used = "alternative_service"
                        return True, result
                        
                    self.logger.warning(f"Alternative service başarısız: {step.step_id}")
                    
                except Exception as e:
                    self.logger.exception(f"Alternative service çalıştırılırken hata: {e}")
        
        if step.use_cache_fallback and step.service_name in self.cache_strategies:
            cache_strategy = self.cache_strategies[step.service_name]
            
            try:
                cache_key = f"{step.service_name}:{step.operation}:{json.dumps(step.request_data)}"
                
                success, result = await cache_strategy.execute(
                    call_service,
                    cache_key=cache_key
                )
                
                if success:
                    step.fallback_mechanism_used = "cache"
                    return True, result
                    
                self.logger.warning(f"Cache fallback başarısız: {step.step_id}")
                
            except Exception as e:
                self.logger.exception(f"Cache fallback çalıştırılırken hata: {e}")
        
        return False, "Tüm fallback mekanizmaları başarısız oldu"
    
    async def _compensate_saga(self, saga: EnhancedSaga) -> None:
        self.logger.info(f"SAGA telafi işlemleri başlatılıyor: {saga.saga_id}")
        
        compensation_steps = 0
        
        for step in reversed(saga.steps[:saga.current_step_index]):
            if step.status == "success" and step.compensation_required:
                self.logger.info(f"Telafi işlemi: {step.step_id}")
                
                if step.service_name == "license_plate_service" and step.operation == "create_entry_record":
                    try:
                        response = await license_plate_service(
                            "cancel_entry_record", 
                            {"license_plate": step.request_data.get("license_plate")}
                        )
                        step.status = "compensated"
                        compensation_steps += 1
                        self.logger.info(f"Telafi işlemi başarılı: {step.step_id}")
                    except Exception as e:
                        self.logger.error(f"Telafi işlemi başarısız: {step.step_id} - {str(e)}")
                
                elif step.service_name == "parking_mgmt_service" and step.operation == "allocate_parking_space":
                    try:
                        response = await parking_mgmt_service(
                            "release_parking_space", 
                            {"license_plate": step.request_data.get("license_plate")}
                        )
                        step.status = "compensated"
                        compensation_steps += 1
                        self.logger.info(f"Telafi işlemi başarılı: {step.step_id}")
                    except Exception as e:
                        self.logger.error(f"Telafi işlemi başarısız: {step.step_id} - {str(e)}")
        
        saga.telemetry["compensation_steps_executed"] = compensation_steps
        
        if compensation_steps > 0 and compensation_steps < saga.current_step_index:
            saga.status = "partially_completed"
            self.logger.info(f"SAGA kısmen tamamlandı (telafi işlemleri sonrası): {saga.saga_id}")

async def run_example():
    logger.info("=== Fallback Mekanizmaları ile SAGA Örneği ===")
    
    orchestrator = EnhancedSagaOrchestrator()
    
    saga = EnhancedSaga(str(uuid.uuid4()), "Araç Giriş İşlemi")
    
    verify_step = EnhancedSagaStep("verify_license_plate", "license_plate_service", "verify_license_plate")
    verify_step.request_data = {"license_plate": "34ABC123"}
    verify_step.retry_count = 2
    verify_step.use_cache_fallback = True
    saga.add_step(verify_step)
    
    allocate_step = EnhancedSagaStep("allocate_parking_space", "parking_mgmt_service", "allocate_parking_space")
    allocate_step.request_data = {"license_plate": "34ABC123"}
    allocate_step.retry_count = 3
    allocate_step.use_circuit_breaker = True
    saga.add_step(allocate_step)
    
    entry_step = EnhancedSagaStep("create_entry_record", "license_plate_service", "create_entry_record")
    entry_step.request_data = {"license_plate": "34ABC123"}
    entry_step.use_circuit_breaker = True
    entry_step.alternative_services = ["alternative_license_plate_service"]
    saga.add_step(entry_step)
    
    result_saga = await orchestrator.execute_saga(saga)
    
    logger.info(f"SAGA sonucu: {result_saga.status}")
    logger.info(f"Toplam çalışma süresi: {result_saga.telemetry['total_execution_time_ms']:.2f}ms")
    
    for i, step in enumerate(result_saga.steps):
        logger.info(f"Adım {i+1}: {step.step_id}")
        logger.info(f"  Durum: {step.status}")
        logger.info(f"  Çalışma süresi: {step.execution_time_ms:.2f}ms")
        logger.info(f"  Yeniden deneme sayısı: {step.retry_attempts}")
        logger.info(f"  Kullanılan fallback: {step.fallback_mechanism_used or 'Yok'}")
        if step.error:
            logger.info(f"  Hata: {step.error}")
    
    logger.info(f"Telafi işlemleri: {result_saga.telemetry['compensation_steps_executed']}")
    
    logger.info("=== Örnek tamamlandı ===")

if __name__ == "__main__":
    asyncio.run(run_example()) 