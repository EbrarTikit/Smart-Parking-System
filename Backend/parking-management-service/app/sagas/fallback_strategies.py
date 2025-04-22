import logging
import asyncio
from typing import Dict, Any, Optional, Callable, Tuple, List
from datetime import datetime, timedelta
import random

logger = logging.getLogger(__name__)

async def retry_with_backoff(
    func: Callable, 
    *args,
    retry_count: int = 3,
    initial_delay: float = 0.5,
    backoff_factor: float = 2,
    max_delay: float = 10.0,
    **kwargs
) -> Tuple[bool, Any]:
    current_delay = initial_delay
    last_exception = None
    
    for attempt in range(retry_count + 1):
        try:
            if attempt > 0:
                logger.info(f"Yeniden deneme {attempt}/{retry_count}, gecikme: {current_delay:.2f}s")
                await asyncio.sleep(current_delay)
                
                current_delay = min(current_delay * backoff_factor, max_delay)
            
            result = await func(*args, **kwargs)
            logger.info(f"İşlem başarıyla tamamlandı (deneme {attempt + 1}/{retry_count + 1})")
            return True, result
            
        except Exception as e:
            last_exception = e
            logger.warning(f"Deneme {attempt + 1}/{retry_count + 1} başarısız: {str(e)}")
    
    logger.error(f"Tüm yeniden denemeler başarısız oldu: {str(last_exception)}")
    return False, str(last_exception)


class CircuitBreaker:
    
    CLOSED = "closed"
    OPEN = "open"
    HALF_OPEN = "half_open"
    
    def __init__(
        self,
        service_name: str,
        failure_threshold: int = 5,
        reset_timeout: float = 30.0,
        half_open_max_calls: int = 2
    ):
        self.service_name = service_name
        self.failure_threshold = failure_threshold
        self.reset_timeout = reset_timeout
        self.half_open_max_calls = half_open_max_calls
        
        self.failure_count = 0
        self.state = self.CLOSED
        self.last_failure_time = None
        self.half_open_calls = 0
        
        self.logger = logging.getLogger(f"circuit_breaker.{service_name}")
        
    async def execute(self, func: Callable, *args, **kwargs) -> Tuple[bool, Any]:
        if self.state == self.OPEN:
            if self.last_failure_time and (datetime.now() - self.last_failure_time) > timedelta(seconds=self.reset_timeout):
                self.logger.info(f"{self.service_name} için devre HALF_OPEN durumuna geçiyor")
                self.state = self.HALF_OPEN
                self.half_open_calls = 0
            else:
                self.logger.warning(f"{self.service_name} için devre OPEN durumunda - istek reddedildi")
                return False, f"Circuit Breaker açık: {self.service_name} servisine erişilemiyor"
        
        if self.state == self.HALF_OPEN and self.half_open_calls >= self.half_open_max_calls:
            self.logger.warning(f"{self.service_name} için maximum yarı-açık çağrı sayısına ulaşıldı")
            return False, f"Circuit Breaker yarı-açık durumda: {self.service_name} çağrı limiti aşıldı"
        
        if self.state == self.HALF_OPEN:
            self.half_open_calls += 1
            
        try:
            result = await func(*args, **kwargs)
            
            if self.state == self.HALF_OPEN:
                self.logger.info(f"{self.service_name} için devre CLOSED durumuna geçiyor (başarılı çağrı)")
                self.state = self.CLOSED
                
            self.failure_count = 0
            
            return True, result
            
        except Exception as e:
            self.failure_count += 1
            self.last_failure_time = datetime.now()
            
            if self.state == self.CLOSED and self.failure_count >= self.failure_threshold:
                self.logger.warning(f"{self.service_name} için devre OPEN durumuna geçiyor ({self.failure_count} başarısız çağrı)")
                self.state = self.OPEN
            
            if self.state == self.HALF_OPEN:
                self.logger.warning(f"{self.service_name} için devre tekrar OPEN durumuna geçiyor")
                self.state = self.OPEN
                
            return False, str(e)

class AlternativeServiceStrategy:
    
    def __init__(self, primary_service: str, alternative_services: List[str], timeout: float = 5.0):
        self.primary_service = primary_service
        self.alternative_services = alternative_services
        self.timeout = timeout
        self.logger = logging.getLogger(f"alt_service.{primary_service}")
        
    async def execute(self, 
                    primary_func: Callable, 
                    alternative_funcs: List[Callable], 
                    *args, **kwargs) -> Tuple[bool, Any]:
        try:
            result = await asyncio.wait_for(primary_func(*args, **kwargs), timeout=self.timeout)
            self.logger.info(f"Birincil servis {self.primary_service} başarıyla çalıştı")
            return True, result
        except asyncio.TimeoutError:
            self.logger.warning(f"Birincil servis {self.primary_service} timeout")
        except Exception as e:
            self.logger.warning(f"Birincil servis {self.primary_service} hatası: {str(e)}")
        
        for i, (alt_service, alt_func) in enumerate(zip(self.alternative_services, alternative_funcs)):
            try:
                self.logger.info(f"Alternatif servis {alt_service} deneniyor")
                result = await asyncio.wait_for(alt_func(*args, **kwargs), timeout=self.timeout)
                self.logger.info(f"Alternatif servis {alt_service} başarıyla çalıştı")
                return True, result
            except asyncio.TimeoutError:
                self.logger.warning(f"Alternatif servis {alt_service} timeout")
            except Exception as e:
                self.logger.warning(f"Alternatif servis {alt_service} hatası: {str(e)}")
        
        self.logger.error(f"Tüm servisler başarısız: {self.primary_service} ve {len(self.alternative_services)} alternatif")
        return False, f"Hiçbir servis çalışmıyor: {self.primary_service} ve alternatifleri"

class CacheFallbackStrategy:
    
    def __init__(self, cache_ttl_seconds: int = 300):
        self.cache = {}
        self.cache_ttl_seconds = cache_ttl_seconds
        self.logger = logging.getLogger("cache_fallback")
        
    def _cache_key(self, func_name: str, args: tuple, kwargs: dict) -> str:
        return f"{func_name}:{hash(str(args))}:{hash(str(kwargs))}"
        
    async def execute(self, func: Callable, cache_key: Optional[str] = None, *args, **kwargs) -> Tuple[bool, Any]:
        if cache_key is None:
            cache_key = self._cache_key(func.__name__, args, kwargs)
            
        try:
            result = await func(*args, **kwargs)
            
            expiry_time = datetime.now() + timedelta(seconds=self.cache_ttl_seconds)
            self.cache[cache_key] = (result, expiry_time)
            self.logger.debug(f"Sonuç cache'e kaydedildi: {cache_key}")
            
            return True, result
            
        except Exception as e:
            self.logger.warning(f"Servis çağrısı başarısız, cache'e bakılıyor: {str(e)}")
            
            if cache_key in self.cache:
                cached_value, expiry_time = self.cache[cache_key]
                
                if datetime.now() < expiry_time:
                    self.logger.info(f"Cache'den veri döndürülüyor: {cache_key}")
                    return True, cached_value
                else:
                    self.logger.info(f"Cache'deki veri geçersiz: {cache_key}")
                    del self.cache[cache_key]
            
            return False, str(e)

class LoadBalancingStrategy:
    
    ROUND_ROBIN = "round_robin"
    RANDOM = "random"
    LEAST_CONNECTIONS = "least_connections"
    
    def __init__(self, 
                services: List[str], 
                service_funcs: List[Callable], 
                algorithm: str = ROUND_ROBIN):
        self.services = services
        self.service_funcs = service_funcs
        self.algorithm = algorithm
        self.current_index = 0
        self.connections = [0] * len(services)
        self.logger = logging.getLogger("load_balancer")
        
    async def execute(self, *args, **kwargs) -> Tuple[bool, Any]:
        if self.algorithm == self.ROUND_ROBIN:
            service_index = self.current_index
            self.current_index = (self.current_index + 1) % len(self.services)
            
        elif self.algorithm == self.RANDOM:
            service_index = random.randint(0, len(self.services) - 1)
            
        elif self.algorithm == self.LEAST_CONNECTIONS:
            service_index = self.connections.index(min(self.connections))
            
        else:
            raise ValueError(f"Geçersiz yük dengeleme algoritması: {self.algorithm}")
            
        service_name = self.services[service_index]
        service_func = self.service_funcs[service_index]
        
        self.logger.info(f"Servis seçildi: {service_name} ({self.algorithm})")
        
        try:
            self.connections[service_index] += 1
            
            result = await service_func(*args, **kwargs)
            
            self.logger.info(f"Servis çağrısı başarılı: {service_name}")
            return True, result
            
        except Exception as e:
            self.logger.warning(f"Servis çağrısı başarısız: {service_name} - {str(e)}")
            return False, str(e)
            
        finally:
            self.connections[service_index] -= 1

async def example_usage():
    
    async def success_service():
        return {"status": "success", "data": "Örnek veri"}
        
    async def failing_service():
        raise Exception("Servis hatası")
        
    async def timeout_service():
        await asyncio.sleep(10)
        return {"status": "timeout_service_completed"}
    
    logger.info("=== Yeniden Deneme Stratejisi Örneği ===")
    success, result = await retry_with_backoff(success_service)
    logger.info(f"Başarılı servis çağrısı: {success}, Sonuç: {result}")
    
    success, result = await retry_with_backoff(failing_service, retry_count=2)
    logger.info(f"Başarısız servis çağrısı: {success}, Sonuç: {result}")
    
    logger.info("\n=== Circuit Breaker Örneği ===")
    circuit_breaker = CircuitBreaker("test_service", failure_threshold=2)
    
    success, result = await circuit_breaker.execute(success_service)
    logger.info(f"Başarılı çağrı: {success}, Sonuç: {result}")
    
    success, result = await circuit_breaker.execute(failing_service)
    logger.info(f"Başarısız çağrı 1: {success}, Sonuç: {result}")
    
    success, result = await circuit_breaker.execute(failing_service)
    logger.info(f"Başarısız çağrı 2: {success}, Sonuç: {result}")
    
    success, result = await circuit_breaker.execute(success_service)
    logger.info(f"Devre açıkken çağrı: {success}, Sonuç: {result}")
    
    logger.info("\n=== Alternatif Servis Stratejisi Örneği ===")
    alt_strategy = AlternativeServiceStrategy("primary", ["alt1", "alt2"])
    
    success, result = await alt_strategy.execute(
        failing_service,
        [success_service, failing_service]
    )
    logger.info(f"Alternatif servis çağrısı: {success}, Sonuç: {result}")
    
    logger.info("\n=== Cache Fallback Stratejisi Örneği ===")
    cache_strategy = CacheFallbackStrategy(cache_ttl_seconds=60)
    
    success, result = await cache_strategy.execute(success_service)
    logger.info(f"İlk çağrı: {success}, Sonuç: {result}")
    
    success, result = await cache_strategy.execute(failing_service, cache_key="test_key")
    logger.info(f"İkinci çağrı (cache'den): {success}, Sonuç: {result}")
    
    logger.info("\n=== Yük Dengeleme Stratejisi Örneği ===")
    load_balancer = LoadBalancingStrategy(
        ["service1", "service2", "service3"],
        [success_service, failing_service, success_service],
        algorithm=LoadBalancingStrategy.ROUND_ROBIN
    )
    
    for i in range(5):
        success, result = await load_balancer.execute()
        logger.info(f"Çağrı {i+1}: {success}, Sonuç: {result}")

if __name__ == "__main__":
    asyncio.run(example_usage()) 