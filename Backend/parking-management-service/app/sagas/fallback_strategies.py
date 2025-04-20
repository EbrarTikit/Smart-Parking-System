"""
Fallback Stratejileri - SAGA Pattern için

Bu modül, SAGA pattern uygulamasında kullanılabilecek çeşitli fallback stratejilerini içerir.
Fallback stratejileri, bir servis çağrısı başarısız olduğunda alternatif işlemler sağlar.
"""

import logging
import asyncio
from typing import Dict, Any, Optional, Callable, Tuple, List
from datetime import datetime, timedelta
import random

logger = logging.getLogger(__name__)

# Yeniden deneme stratejisi için util fonksiyonu
async def retry_with_backoff(
    func: Callable, 
    *args,
    retry_count: int = 3,
    initial_delay: float = 0.5,
    backoff_factor: float = 2,
    max_delay: float = 10.0,
    **kwargs
) -> Tuple[bool, Any]:
    """
    Bir fonksiyonu üssel artışlı yeniden deneme stratejisi ile çağırır.
    
    Parametreler:
        func: Yeniden denenecek async fonksiyon
        *args: Fonksiyona geçirilecek argümanlar
        retry_count: Maksimum yeniden deneme sayısı
        initial_delay: İlk yeniden deneme öncesindeki gecikme süresi (saniye)
        backoff_factor: Her denemede gecikme süresinin kaç kat artacağı
        max_delay: Maksimum gecikme süresi
        **kwargs: Fonksiyona geçirilecek keyword argümanları
        
    Dönüş:
        (başarı durumu, sonuç veya hata mesajı) şeklinde bir tuple
    """
    current_delay = initial_delay
    last_exception = None
    
    for attempt in range(retry_count + 1):  # İlk deneme + retry_count kadar yeniden deneme
        try:
            if attempt > 0:
                logger.info(f"Yeniden deneme {attempt}/{retry_count}, gecikme: {current_delay:.2f}s")
                await asyncio.sleep(current_delay)
                
                # Bir sonraki gecikme süresi (max_delay'den fazla olmayacak şekilde)
                current_delay = min(current_delay * backoff_factor, max_delay)
            
            # Fonksiyonu çağır
            result = await func(*args, **kwargs)
            logger.info(f"İşlem başarıyla tamamlandı (deneme {attempt + 1}/{retry_count + 1})")
            return True, result
            
        except Exception as e:
            last_exception = e
            logger.warning(f"Deneme {attempt + 1}/{retry_count + 1} başarısız: {str(e)}")
    
    # Tüm denemeler başarısız oldu
    logger.error(f"Tüm yeniden denemeler başarısız oldu: {str(last_exception)}")
    return False, str(last_exception)


# Circuit Breaker Pattern uygulaması
class CircuitBreaker:
    """
    Circuit Breaker Pattern uygulaması.
    
    Çok sayıda servis hatası durumunda devre kesilir ve devre açık duruma geçer.
    Devre açık durumdayken, istekler otomatik olarak reddedilir.
    Belirli bir süre sonra, devre yarı-açık duruma geçer ve kontrollü şekilde istekleri kabul etmeye başlar.
    """
    
    # Devre durumları
    CLOSED = "closed"      # Normal işlem - istekler servis tarafından karşılanır
    OPEN = "open"          # Devre açık - istekler otomatik olarak reddedilir
    HALF_OPEN = "half_open"  # Yarı açık - kontrollü şekilde istekler kabul edilir
    
    def __init__(
        self,
        service_name: str,
        failure_threshold: int = 5,
        reset_timeout: float = 30.0,
        half_open_max_calls: int = 2
    ):
        """
        Circuit Breaker nesnesi oluşturur.
        
        Parametreler:
            service_name: Servis adı (loglama için)
            failure_threshold: Devrenin açılması için gerekli ardışık hata sayısı
            reset_timeout: Devrenin yarı-açık duruma geçmesi için gereken süre (saniye)
            half_open_max_calls: Yarı-açık durumda kabul edilecek maksimum istek sayısı
        """
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
        """
        Circuit Breaker ile korunan bir fonksiyonu çalıştırır.
        
        Parametreler:
            func: Çalıştırılacak async fonksiyon
            *args, **kwargs: Fonksiyona geçirilecek argümanlar
            
        Dönüş:
            (başarı durumu, sonuç veya hata mesajı) şeklinde bir tuple
        """
        # Devrenin durumunu kontrol et
        if self.state == self.OPEN:
            # Devrenin yeniden kapanıp kapanmayacağını kontrol et
            if self.last_failure_time and (datetime.now() - self.last_failure_time) > timedelta(seconds=self.reset_timeout):
                self.logger.info(f"{self.service_name} için devre HALF_OPEN durumuna geçiyor")
                self.state = self.HALF_OPEN
                self.half_open_calls = 0
            else:
                # Devre hala açık, isteği otomatik olarak reddet
                self.logger.warning(f"{self.service_name} için devre OPEN durumunda - istek reddedildi")
                return False, f"Circuit Breaker açık: {self.service_name} servisine erişilemiyor"
        
        # Yarı-açık durumda maksimum çağrı sayısını kontrol et
        if self.state == self.HALF_OPEN and self.half_open_calls >= self.half_open_max_calls:
            self.logger.warning(f"{self.service_name} için maximum yarı-açık çağrı sayısına ulaşıldı")
            return False, f"Circuit Breaker yarı-açık durumda: {self.service_name} çağrı limiti aşıldı"
        
        # Eğer yarı-açık durumdaysa, çağrı sayacını artır
        if self.state == self.HALF_OPEN:
            self.half_open_calls += 1
            
        try:
            # Fonksiyonu çalıştır
            result = await func(*args, **kwargs)
            
            # Başarılı çağrı
            if self.state == self.HALF_OPEN:
                # Yarı-açık durumdan kapalı duruma geç
                self.logger.info(f"{self.service_name} için devre CLOSED durumuna geçiyor (başarılı çağrı)")
                self.state = self.CLOSED
                
            # Hata sayacını sıfırla
            self.failure_count = 0
            
            return True, result
            
        except Exception as e:
            # Hata durumu
            self.failure_count += 1
            self.last_failure_time = datetime.now()
            
            if self.state == self.CLOSED and self.failure_count >= self.failure_threshold:
                # Hata eşiği aşıldı, devreyi aç
                self.logger.warning(f"{self.service_name} için devre OPEN durumuna geçiyor ({self.failure_count} başarısız çağrı)")
                self.state = self.OPEN
            
            if self.state == self.HALF_OPEN:
                # Yarı-açık durumda hata, devreyi tamamen aç
                self.logger.warning(f"{self.service_name} için devre tekrar OPEN durumuna geçiyor")
                self.state = self.OPEN
                
            return False, str(e)

# Alternatif Servis Stratejisi
class AlternativeServiceStrategy:
    """
    Bir servisin çalışmaması durumunda alternatif servisleri kullanan strateji.
    """
    
    def __init__(self, primary_service: str, alternative_services: List[str], timeout: float = 5.0):
        """
        Alternatif Servis Stratejisi oluşturur.
        
        Parametreler:
            primary_service: Birincil servis adı
            alternative_services: Alternatif servis adları listesi 
            timeout: Her servis çağrısı için timeout süresi
        """
        self.primary_service = primary_service
        self.alternative_services = alternative_services
        self.timeout = timeout
        self.logger = logging.getLogger(f"alt_service.{primary_service}")
        
    async def execute(self, 
                    primary_func: Callable, 
                    alternative_funcs: List[Callable], 
                    *args, **kwargs) -> Tuple[bool, Any]:
        """
        Önce birincil servisi dener, başarısız olursa alternatif servisleri dener.
        
        Parametreler:
            primary_func: Birincil servisi çağıran fonksiyon
            alternative_funcs: Alternatif servisleri çağıran fonksiyonlar listesi
            *args, **kwargs: Fonksiyonlara geçirilecek argümanlar
            
        Dönüş:
            (başarı durumu, sonuç veya hata mesajı) şeklinde bir tuple
        """
        # Birincil servisi dene
        try:
            result = await asyncio.wait_for(primary_func(*args, **kwargs), timeout=self.timeout)
            self.logger.info(f"Birincil servis {self.primary_service} başarıyla çalıştı")
            return True, result
        except asyncio.TimeoutError:
            self.logger.warning(f"Birincil servis {self.primary_service} timeout")
        except Exception as e:
            self.logger.warning(f"Birincil servis {self.primary_service} hatası: {str(e)}")
        
        # Alternatif servisleri dene
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
        
        # Hiçbir servis çalışmadı
        self.logger.error(f"Tüm servisler başarısız: {self.primary_service} ve {len(self.alternative_services)} alternatif")
        return False, f"Hiçbir servis çalışmıyor: {self.primary_service} ve alternatifleri"

# Cache Fallback Stratejisi
class CacheFallbackStrategy:
    """
    Servis çağrısı başarısız olduğunda önceki başarılı sonucu cache'den döndüren strateji.
    """
    
    def __init__(self, cache_ttl_seconds: int = 300):
        """
        Cache Fallback Stratejisi oluşturur.
        
        Parametreler:
            cache_ttl_seconds: Cache'deki verilerin geçerlilik süresi (saniye)
        """
        self.cache = {}  # Basit bellek içi cache: {key: (value, expiry_time)}
        self.cache_ttl_seconds = cache_ttl_seconds
        self.logger = logging.getLogger("cache_fallback")
        
    def _cache_key(self, func_name: str, args: tuple, kwargs: dict) -> str:
        """Fonksiyon çağrısı için cache anahtarı oluşturur."""
        return f"{func_name}:{hash(str(args))}:{hash(str(kwargs))}"
        
    async def execute(self, func: Callable, cache_key: Optional[str] = None, *args, **kwargs) -> Tuple[bool, Any]:
        """
        Önce servisi çağırır, başarısız olursa cache'den veri döndürür.
        
        Parametreler:
            func: Çağrılacak async fonksiyon
            cache_key: Cache anahtarı (None ise otomatik oluşturulur)
            *args, **kwargs: Fonksiyona geçirilecek argümanlar
            
        Dönüş:
            (başarı durumu, sonuç veya cache'den dönen veri) şeklinde bir tuple
        """
        if cache_key is None:
            cache_key = self._cache_key(func.__name__, args, kwargs)
            
        # Servisi çağırmayı dene
        try:
            result = await func(*args, **kwargs)
            
            # Başarılı sonucu cache'e kaydet
            expiry_time = datetime.now() + timedelta(seconds=self.cache_ttl_seconds)
            self.cache[cache_key] = (result, expiry_time)
            self.logger.debug(f"Sonuç cache'e kaydedildi: {cache_key}")
            
            return True, result
            
        except Exception as e:
            self.logger.warning(f"Servis çağrısı başarısız, cache'e bakılıyor: {str(e)}")
            
            # Cache'den veri döndür
            if cache_key in self.cache:
                cached_value, expiry_time = self.cache[cache_key]
                
                if datetime.now() < expiry_time:
                    self.logger.info(f"Cache'den veri döndürülüyor: {cache_key}")
                    return True, cached_value
                else:
                    # Cache'deki veri artık geçerli değil
                    self.logger.info(f"Cache'deki veri geçersiz: {cache_key}")
                    del self.cache[cache_key]
            
            # Cache'de veri yok veya geçersiz
            return False, str(e)

# Yük Dengeleme Stratejisi (Load Balancing)
class LoadBalancingStrategy:
    """
    Birden fazla servis arasında yük dengelemesi yapan strateji.
    """
    
    # Yük dengeleme algoritmaları
    ROUND_ROBIN = "round_robin"
    RANDOM = "random"
    LEAST_CONNECTIONS = "least_connections"
    
    def __init__(self, 
                services: List[str], 
                service_funcs: List[Callable], 
                algorithm: str = ROUND_ROBIN):
        """
        Yük Dengeleme Stratejisi oluşturur.
        
        Parametreler:
            services: Servis adları listesi
            service_funcs: Servisleri çağıran fonksiyonlar listesi (aynı sırada)
            algorithm: Yük dengeleme algoritması
        """
        self.services = services
        self.service_funcs = service_funcs
        self.algorithm = algorithm
        self.current_index = 0  # Round robin için
        self.connections = [0] * len(services)  # Her servisin aktif bağlantı sayısı
        self.logger = logging.getLogger("load_balancer")
        
    async def execute(self, *args, **kwargs) -> Tuple[bool, Any]:
        """
        Yük dengeleme algoritmasına göre bir servisi seçer ve çağırır.
        
        Parametreler:
            *args, **kwargs: Seçilen servis fonksiyonuna geçirilecek argümanlar
            
        Dönüş:
            (başarı durumu, sonuç veya hata mesajı) şeklinde bir tuple
        """
        # Çağrılacak servis indeksini seç
        if self.algorithm == self.ROUND_ROBIN:
            # Sırayla her servisi seç
            service_index = self.current_index
            self.current_index = (self.current_index + 1) % len(self.services)
            
        elif self.algorithm == self.RANDOM:
            # Rastgele bir servis seç
            service_index = random.randint(0, len(self.services) - 1)
            
        elif self.algorithm == self.LEAST_CONNECTIONS:
            # En az bağlantısı olan servisi seç
            service_index = self.connections.index(min(self.connections))
            
        else:
            raise ValueError(f"Geçersiz yük dengeleme algoritması: {self.algorithm}")
            
        # Seçilen servisi çağır
        service_name = self.services[service_index]
        service_func = self.service_funcs[service_index]
        
        self.logger.info(f"Servis seçildi: {service_name} ({self.algorithm})")
        
        try:
            # Aktif bağlantı sayısını artır
            self.connections[service_index] += 1
            
            # Servisi çağır
            result = await service_func(*args, **kwargs)
            
            # Başarılı çağrı
            self.logger.info(f"Servis çağrısı başarılı: {service_name}")
            return True, result
            
        except Exception as e:
            # Başarısız çağrı
            self.logger.warning(f"Servis çağrısı başarısız: {service_name} - {str(e)}")
            return False, str(e)
            
        finally:
            # Aktif bağlantı sayısını azalt
            self.connections[service_index] -= 1

# Örnek Kullanım
async def example_usage():
    """Fallback stratejilerinin örnek kullanımı."""
    
    # Örnek servis fonksiyonları
    async def success_service():
        return {"status": "success", "data": "Örnek veri"}
        
    async def failing_service():
        raise Exception("Servis hatası")
        
    async def timeout_service():
        await asyncio.sleep(10)  # Uzun süren işlem
        return {"status": "timeout_service_completed"}
    
    # Yeniden deneme stratejisi örneği
    logger.info("=== Yeniden Deneme Stratejisi Örneği ===")
    success, result = await retry_with_backoff(success_service)
    logger.info(f"Başarılı servis çağrısı: {success}, Sonuç: {result}")
    
    success, result = await retry_with_backoff(failing_service, retry_count=2)
    logger.info(f"Başarısız servis çağrısı: {success}, Sonuç: {result}")
    
    # Circuit Breaker örneği
    logger.info("\n=== Circuit Breaker Örneği ===")
    circuit_breaker = CircuitBreaker("test_service", failure_threshold=2)
    
    # Başarılı çağrı
    success, result = await circuit_breaker.execute(success_service)
    logger.info(f"Başarılı çağrı: {success}, Sonuç: {result}")
    
    # Başarısız çağrılar
    success, result = await circuit_breaker.execute(failing_service)
    logger.info(f"Başarısız çağrı 1: {success}, Sonuç: {result}")
    
    success, result = await circuit_breaker.execute(failing_service)
    logger.info(f"Başarısız çağrı 2: {success}, Sonuç: {result}")
    
    # Devre açık durumda çağrı
    success, result = await circuit_breaker.execute(success_service)
    logger.info(f"Devre açıkken çağrı: {success}, Sonuç: {result}")
    
    # Alternatif Servis Stratejisi örneği
    logger.info("\n=== Alternatif Servis Stratejisi Örneği ===")
    alt_strategy = AlternativeServiceStrategy("primary", ["alt1", "alt2"])
    
    success, result = await alt_strategy.execute(
        failing_service,  # Birincil servis (başarısız olacak)
        [success_service, failing_service]  # Alternatif servisler
    )
    logger.info(f"Alternatif servis çağrısı: {success}, Sonuç: {result}")
    
    # Cache Fallback Stratejisi örneği
    logger.info("\n=== Cache Fallback Stratejisi Örneği ===")
    cache_strategy = CacheFallbackStrategy(cache_ttl_seconds=60)
    
    # İlk çağrı (başarılı, cache'e kaydedilecek)
    success, result = await cache_strategy.execute(success_service)
    logger.info(f"İlk çağrı: {success}, Sonuç: {result}")
    
    # İkinci çağrı (başarısız, cache'den veri döndürülecek)
    success, result = await cache_strategy.execute(failing_service, cache_key="test_key")
    logger.info(f"İkinci çağrı (cache'den): {success}, Sonuç: {result}")
    
    # Yük Dengeleme Stratejisi örneği
    logger.info("\n=== Yük Dengeleme Stratejisi Örneği ===")
    load_balancer = LoadBalancingStrategy(
        ["service1", "service2", "service3"],
        [success_service, failing_service, success_service],
        algorithm=LoadBalancingStrategy.ROUND_ROBIN
    )
    
    # Birkaç çağrı yap
    for i in range(5):
        success, result = await load_balancer.execute()
        logger.info(f"Çağrı {i+1}: {success}, Sonuç: {result}")

if __name__ == "__main__":
    # Örnek kullanımı çalıştır
    asyncio.run(example_usage()) 