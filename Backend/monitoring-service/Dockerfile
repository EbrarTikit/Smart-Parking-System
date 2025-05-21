FROM python:3.9-slim

WORKDIR /app

# Gerekli sistem bağımlılıklarını kur
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    gcc \
    && apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Uygulama bağımlılıklarını kopyala ve kur
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Uygulama kodunu kopyala
COPY app /app/app

EXPOSE 8010

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8010"] 