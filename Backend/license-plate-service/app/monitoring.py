import logging
import json
import time
import socket
from typing import Dict, Any, Optional
from pythonjsonlogger import jsonlogger
from prometheus_client import Counter, Histogram, Gauge, start_http_server
import os
from datetime import datetime

# Prometheus metriklerini tanımla
REQUEST_COUNT = Counter(
    'license_plate_service_requests_total',
    'Total number of requests',
    ['method', 'endpoint', 'status']
)

REQUEST_LATENCY = Histogram(
    'license_plate_service_request_latency_seconds',
    'Request latency in seconds',
    ['method', 'endpoint']
)

ACTIVE_REQUESTS = Gauge(
    'license_plate_service_active_requests',
    'Number of active requests',
    ['method', 'endpoint']
)

PLATE_RECOGNITION_COUNT = Counter(
    'license_plate_recognition_total',
    'Total number of license plate recognition attempts',
    ['success']
)

PLATE_RECOGNITION_LATENCY = Histogram(
    'license_plate_recognition_latency_seconds',
    'License plate recognition latency in seconds'
)

PARKING_RECORDS_COUNT = Counter(
    'parking_records_total',
    'Total number of parking records',
    ['action']  # entry, exit
)

# JSON formatında log oluşturucu
class CustomJsonFormatter(jsonlogger.JsonFormatter):
    def add_fields(self, log_record, record, message_dict):
        super(CustomJsonFormatter, self).add_fields(log_record, record, message_dict)
        log_record['timestamp'] = datetime.utcnow().isoformat()
        log_record['level'] = record.levelname
        log_record['service'] = 'license-plate-service'
        log_record['hostname'] = socket.gethostname()

# Logger yapılandırması
def setup_logging(log_level=logging.INFO):
    """
    JSON formatında log çıktısı oluşturacak şekilde logger'ı yapılandırır
    """
    logger = logging.getLogger()
    logger.setLevel(log_level)
    
    # Konsol handler
    console_handler = logging.StreamHandler()
    console_handler.setLevel(log_level)
    
    # JSON formatter
    formatter = CustomJsonFormatter('%(timestamp)s %(level)s %(name)s %(message)s')
    console_handler.setFormatter(formatter)
    
    # Önceki handler'ları temizle
    for handler in logger.handlers[:]:
        logger.removeHandler(handler)
    
    logger.addHandler(console_handler)
    
    return logger

# Prometheus metrik sunucusunu başlat
def start_metrics_server(port=8000):
    """
    Prometheus metriklerini sunan HTTP sunucusunu başlatır
    """
    try:
        start_http_server(port)
        logging.info(f"Prometheus metrics server started on port {port}")
    except Exception as e:
        logging.error(f"Failed to start Prometheus metrics server: {str(e)}")

# Request metriklerini ölçmek için middleware
class PrometheusMiddleware:
    def __init__(self, app):
        self.app = app
    
    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            return await self.app(scope, receive, send)
        
        method = scope.get("method", "UNKNOWN")
        path = scope.get("path", "UNKNOWN")
        
        ACTIVE_REQUESTS.labels(method=method, endpoint=path).inc()
        start_time = time.time()
        
        # Orijinal yanıt gönderme fonksiyonunu wrap et
        original_send = send
        status_code = "500"  # Varsayılan olarak hata durumu
        
        async def send_wrapper(message):
            nonlocal status_code
            if message["type"] == "http.response.start":
                status_code = str(message["status"])
            await original_send(message)
        
        try:
            await self.app(scope, receive, send_wrapper)
        except Exception as e:
            logging.exception(f"Error during request processing: {str(e)}")
            status_code = "500"
            raise
        finally:
            REQUEST_COUNT.labels(method=method, endpoint=path, status=status_code).inc()
            REQUEST_LATENCY.labels(method=method, endpoint=path).observe(time.time() - start_time)
            ACTIVE_REQUESTS.labels(method=method, endpoint=path).dec()

# Plaka tanıma metriklerini ölçmek için dekoratör
def track_plate_recognition(func):
    """
    Plaka tanıma işlemlerini izlemek için dekoratör
    """
    def wrapper(*args, **kwargs):
        start_time = time.time()
        try:
            result = func(*args, **kwargs)
            success = "true" if result and not result.get("error") else "false"
            PLATE_RECOGNITION_COUNT.labels(success=success).inc()
            return result
        except Exception as e:
            PLATE_RECOGNITION_COUNT.labels(success="false").inc()
            raise
        finally:
            PLATE_RECOGNITION_LATENCY.observe(time.time() - start_time)
    return wrapper

# Park kaydı metriklerini güncellemek için yardımcı fonksiyonlar
def track_vehicle_entry():
    """
    Araç girişlerini izle
    """
    PARKING_RECORDS_COUNT.labels(action="entry").inc()

def track_vehicle_exit():
    """
    Araç çıkışlarını izle
    """
    PARKING_RECORDS_COUNT.labels(action="exit").inc()

# Log ve metrik yapılandırmasını başlat
def init_monitoring():
    """
    Tüm izleme bileşenlerini başlatır
    """
    log_level = os.getenv("LOG_LEVEL", "INFO").upper()
    log_level_num = getattr(logging, log_level, logging.INFO)
    
    setup_logging(log_level=log_level_num)
    
    # Prometheus metrik sunucusunu başlat
    # FastAPI uygulamasıyla aynı portu kullanıyoruz, çünkü FastAPI
    # /metrics endpoint'ini kullanacak
    # start_metrics_server(8000)
    
    logging.info("Monitoring initialized")

# Uygulama başlangıcında çağrılacak
init_monitoring() 