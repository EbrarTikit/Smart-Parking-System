import os
from dotenv import load_dotenv

# .env dosyasını yükle (eğer varsa)
load_dotenv()

# Elasticsearch ayarları
ELASTICSEARCH_HOST = os.getenv("ELASTICSEARCH_HOST", "elasticsearch")
ELASTICSEARCH_PORT = int(os.getenv("ELASTICSEARCH_PORT", 9200))
ELASTICSEARCH_USER = os.getenv("ELASTICSEARCH_USER", "")
ELASTICSEARCH_PASSWORD = os.getenv("ELASTICSEARCH_PASSWORD", "")
ELASTICSEARCH_INDEX_PREFIX = os.getenv("ELASTICSEARCH_INDEX_PREFIX", "smart-parking-")

# Prometheus ayarları
PROMETHEUS_HOST = os.getenv("PROMETHEUS_HOST", "prometheus")
PROMETHEUS_PORT = int(os.getenv("PROMETHEUS_PORT", 9090))

# Servis listesi (izlenecek servisler)
MONITORED_SERVICES = [
    "license-plate-service",
    "parking-management-service",
    "user-service",
    "notification-service",
    "chatbot-service",
    "navigation-service"
]

# Metrik listesi (izlenecek metrikler)
DEFAULT_METRICS = [
    "http_requests_total",
    "http_request_duration_seconds",
    "vehicle_entries_total",
    "vehicle_exits_total",
    "plate_recognition_total",
    "plate_recognition_errors_total",
    "parking_fee_calculation_total",
    "parking_fee_calculation_errors_total"
]

# API ayarları
API_TITLE = "Smart Parking Monitoring Service"
API_DESCRIPTION = "Akıllı Otopark Sistemi için izleme ve loglama servisi"
API_VERSION = "1.0.0"

# Loglama ayarları
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
LOG_FORMAT = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"

# Zaman ayarları
DEFAULT_TIME_WINDOW = "24h"  # Son 24 saat
DEFAULT_STEP = "5m"  # 5 dakikalık adımlar 