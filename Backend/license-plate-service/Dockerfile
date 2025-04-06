FROM python:3.9-slim

WORKDIR /app

# Sistem bağımlılıklarını yükle
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libglib2.0-0 \
    libsm6 \
    libxrender1 \
    libxext6 \
    gcc \
    postgresql-client \
    # EasyOCR için gerekli bağımlılıklar
    libpng-dev \
    libjpeg-dev \
    libtiff-dev \
    libfreetype6-dev \
    && rm -rf /var/lib/apt/lists/*

# Önce requirements.txt'yi kopyala ve bağımlılıkları yükle
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# EasyOCR modellerini önceden indir
RUN mkdir -p /root/.EasyOCR/tr
# TR ve Common modelleri için ön yükleme yapabilirsiniz (isteğe bağlı)
# RUN python -c "import easyocr; reader = easyocr.Reader(['tr'])"

# Sonra uygulama kodunu ve .env dosyasını kopyala
COPY ./app /app/app
COPY .env .

# Debug için
RUN ls -la /app

# Uygulama başlatma komutu
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]