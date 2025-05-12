FROM python:3.9-slim

WORKDIR /app

# Sistem bağımlılıklarını yükle
RUN apt-get update && apt-get install -y \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Önce requirements.txt'yi kopyala ve bağımlılıkları yükle
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Sonra uygulama kodunu ve .env dosyasını kopyala
COPY ./app /app/app
COPY .env .
COPY __init__.py .

# Debug için
RUN ls -la /app

# Redis bağlantısı için ortam değişkenleri
ENV REDIS_HOST=redis_service
ENV REDIS_PORT=6379
ENV REDIS_DB=0
ENV REDIS_PASSWORD=
ENV REDIS_EXPIRY=1800

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001", "--reload"]
