FROM python:3.9-slim

WORKDIR /app

# Sistem güncellemesi ve bağımlılıkları
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libglib2.0-0 \
    libsm6 \
    libxrender1 \
    libxext6 \
    gcc \
    postgresql-client \
    # Tesseract OCR için gerekli bağımlılıklar
    tesseract-ocr \
    tesseract-ocr-tur \
    tesseract-ocr-eng \
    libpng-dev \
    libjpeg-dev \
    libtiff-dev \
    libfreetype6-dev \
    && rm -rf /var/lib/apt/lists/*

# Tesseract test
RUN tesseract --version && tesseract --list-langs

# Debug klasörü oluştur
RUN mkdir -p /app/debug_images && chmod 777 /app/debug_images

# Önce requirements.txt'yi kopyala ve bağımlılıkları yükle
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Tesseract sürümünü ve kurulumunu kontrol et
RUN python -c "import pytesseract; print(f'Tesseract Python Sürümü: {pytesseract.__version__}'); \
    print(f'Tesseract Sürümü: {pytesseract.get_tesseract_version()}')" || echo "Tesseract kurulum hatası"

# Kod dizini ve .env dosyası docker-compose volumes tarafından yönetilecek,
# Bu nedenle burada kopyalamıyoruz

# Geliştirme sırasında, kod değişikliklerini otomatik algılamak için reload modunda çalış
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--reload", "--log-level", "debug"]
