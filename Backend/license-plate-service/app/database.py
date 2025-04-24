from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import os
from dotenv import load_dotenv

# .env dosyasını yükle (Docker ortamında zaten çevre değişkenleri ayarlanmış olacak)
load_dotenv()

# Veritabanı URL'sini al
DATABASE_URL = os.getenv("DATABASE_URL")

# Veritabanı URL'si yoksa ve PostgreSQL çalışmıyorsa SQLite kullan
if not DATABASE_URL:
    DATABASE_URL = "sqlite:///./license_plate.db"
    print(f"UYARI: DATABASE_URL bulunamadı, varsayılan SQLite veritabanı kullanılıyor: {DATABASE_URL}")
else:
    print(f"Veritabanı bağlantısı: {DATABASE_URL}")

# Engine oluştur
engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()