"""
Uygulama konfigürasyon ayarları
"""

import os
from typing import Dict, List, Optional, Any

# DATABASE_URL çevre değişkeni varsa doğrudan kullan, yoksa ayrı bileşenlerden oluştur
if "DATABASE_URL" in os.environ:
    DATABASE_URL = os.environ["DATABASE_URL"]
    print(f"Çevre değişkeni DATABASE_URL kullanılıyor: {DATABASE_URL}")
else:
    # Veritabanı bağlantı bilgileri
    DB_HOST = os.getenv("DB_HOST", "postgres_db")
    DB_PORT = os.getenv("DB_PORT", "5432")
    DB_NAME = os.getenv("DB_NAME", "license_plate_db")  # Doğru veritabanı adı
    DB_USER = os.getenv("DB_USER", "user")  # Docker Compose'daki kullanıcı adı
    DB_PASSWORD = os.getenv("DB_PASSWORD", "password")  # Docker Compose'daki şifre

    # SQLAlchemy bağlantı URL'si
    DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
    print(f"Bileşenlerden oluşturulan DATABASE_URL: {DATABASE_URL}")

# RabbitMQ bağlantı bilgileri
RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "rabbitmq")
RABBITMQ_PORT = os.getenv("RABBITMQ_PORT", "5672")
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "guest")
RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "guest")
RABBITMQ_VHOST = os.getenv("RABBITMQ_VHOST", "/")

# RabbitMQ bağlantı URL'si
RABBITMQ_URL = f"amqp://{RABBITMQ_USER}:{RABBITMQ_PASSWORD}@{RABBITMQ_HOST}:{RABBITMQ_PORT}/{RABBITMQ_VHOST}"

# Redis bağlantı bilgileri
REDIS_HOST = os.getenv("REDIS_HOST", "redis")
REDIS_PORT = os.getenv("REDIS_PORT", "6379")
REDIS_DB = os.getenv("REDIS_DB", "0")
REDIS_PASSWORD = os.getenv("REDIS_PASSWORD", "")

# Redis bağlantı URL'si
REDIS_URL = f"redis://:{REDIS_PASSWORD}@{REDIS_HOST}:{REDIS_PORT}/{REDIS_DB}" if REDIS_PASSWORD else f"redis://{REDIS_HOST}:{REDIS_PORT}/{REDIS_DB}"

# Uygulama ayarları
DEBUG = os.getenv("DEBUG", "False").lower() in ("true", "1", "t")
API_PORT = int(os.getenv("API_PORT", "8000"))
API_HOST = os.getenv("API_HOST", "0.0.0.0")

# Model ayarları
MODEL_DIR = os.getenv("MODEL_DIR", "./app/model")
USE_GPU = os.getenv("USE_GPU", "False").lower() in ("true", "1", "t")
DEVICE = "cuda" if USE_GPU else "cpu"

# Log ayarları
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

# Debug ortamında ek bilgileri göster
if DEBUG:
    print("=== Uygulama Ayarları ===")
    print(f"Veritabanı URL: {DATABASE_URL}")
    print(f"RabbitMQ URL: {RABBITMQ_URL}")
    print(f"Redis URL: {REDIS_URL}")
    print(f"API: {API_HOST}:{API_PORT}")
    print(f"Model Dizini: {MODEL_DIR}")
    print(f"Cihaz: {DEVICE}")
    print(f"Log Seviyesi: {LOG_LEVEL}")
    print("======================") 