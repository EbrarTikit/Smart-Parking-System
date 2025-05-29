#!/usr/bin/env python3
"""
Veritabanı tablolarını oluşturmak için başlangıç script'i.
Docker konteynerinin başlangıcında çalıştırılır.
"""

import os
import sys
import time
import logging
from sqlalchemy import create_engine, inspect, text
from sqlalchemy.exc import OperationalError

# Logging yapılandırması
logging.basicConfig(level=logging.INFO, 
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger("init-db")

# Veritabanı URL'sini al
DATABASE_URL = os.getenv("DATABASE_URL")
if not DATABASE_URL:
    logger.error("DATABASE_URL çevre değişkeni bulunamadı!")
    sys.exit(1)

logger.info(f"Veritabanı bağlantısı: {DATABASE_URL}")

# Veritabanına bağlanmayı dene (birkaç kez)
MAX_RETRIES = 10
RETRY_INTERVAL = 3  # saniye

def wait_for_db():
    """Veritabanı hazır olana kadar bekler."""
    logger.info("Veritabanının hazır olmasını bekliyor...")
    
    for attempt in range(MAX_RETRIES):
        try:
            engine = create_engine(DATABASE_URL)
            with engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            logger.info("Veritabanı bağlantısı başarılı!")
            return engine
        except OperationalError as e:
            logger.warning(f"Veritabanına bağlanılamadı (deneme {attempt+1}/{MAX_RETRIES}): {str(e)}")
            if attempt < MAX_RETRIES - 1:
                logger.info(f"{RETRY_INTERVAL} saniye bekliyor...")
                time.sleep(RETRY_INTERVAL)
            else:
                logger.error("Maksimum deneme sayısına ulaşıldı. Veritabanına bağlanılamadı.")
                sys.exit(1)

# Veritabanı bağlantısını bekle
engine = wait_for_db()

# Modelleri içe aktar
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from app.database import Base
from app.models import Vehicle, ParkingRecord

# Tabloları oluştur
logger.info("Veritabanı tablolarını oluşturuyor...")
Base.metadata.create_all(bind=engine)

# Tabloları kontrol et
inspector = inspect(engine)
all_tables = ["vehicles", "parking_records", "plate_records"]
missing_tables = [table for table in all_tables if not inspector.has_table(table)]

if missing_tables:
    logger.error(f"Bazı tablolar oluşturulamadı: {missing_tables}")
    sys.exit(1)
else:
    logger.info("Tüm tablolar başarıyla oluşturuldu:")
    for table in all_tables:
        logger.info(f"- {table}")

logger.info("Veritabanı başlatma işlemi tamamlandı!") 