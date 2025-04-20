FROM python:3.9-slim

WORKDIR /app

# Sistem bağımlılıklarını kur
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Python bağımlılıklarını kopyala ve kur
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Uygulama kodunu kopyala
COPY ./app /app/app

# Port aç
EXPOSE 8000

# Uygulama başlat
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"] 