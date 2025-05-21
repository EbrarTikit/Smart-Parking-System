# 1. Temel builder image - Tüm builder aşamalarının temelini oluşturur
FROM python:3.9-slim AS base-builder

WORKDIR /build

# Her zaman apt-get update ve install komutlarını birlikte çalıştırın
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    gcc \
    postgresql-client \
    && apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# CPU-ONLY kurulum için çevresel değişkenleri ayarla
ENV PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    FORCE_CUDA=0 \
    CUDA_VISIBLE_DEVICES="" \
    TORCH_CUDA_ARCH_LIST="None" \
    USE_CUDA=0

# Pip'i güncelleyin ve gerekli temel araçları kurun
RUN pip install --no-cache-dir -U pip setuptools wheel

# 2. Temel kütüphaneler aşaması - sadece numpy
FROM base-builder AS builder-numpy
COPY requirements.txt .
RUN pip wheel --no-cache-dir -w /wheels numpy==1.24.2

# 3. Diğer temel kütüphaneler
FROM base-builder AS builder-core
COPY requirements.txt .
RUN pip wheel --no-cache-dir -w /wheels \
    pandas==2.0.1 \
    scipy==1.10.1 \
    filterpy==1.4.5

# 4. Görüntü işleme kütüphaneleri aşaması
FROM base-builder AS builder-cv
COPY requirements.txt .
RUN pip wheel --no-cache-dir -w /wheels \
    opencv-python==4.7.0.72 \
    scikit-image==0.24.0 \
    Pillow>=9.0.0  # PIL kütüphanesi, LANCZOS için

# 5. Web framework kütüphaneleri aşaması
FROM base-builder AS builder-web
COPY requirements.txt .
RUN pip wheel --no-cache-dir -w /wheels \
    fastapi==0.95.0 \
    uvicorn[standard]==0.21.1 \
    sqlalchemy==2.0.9 \
    psycopg2-binary==2.9.6 \
    pydantic==1.10.7 \
    python-multipart==0.0.6 \
    python-dotenv==1.0.0 \
    websockets==11.0.3 \
    wsproto==1.2.0

# 6. PyTorch aşaması (en uzun süren)
FROM base-builder AS builder-pytorch
COPY requirements.txt .

# CPU sürümlerini açıkça belirt ve GPU bağımlılıklarını önle
ENV FORCE_CUDA=0 \
    CUDA_VISIBLE_DEVICES="" \
    TORCH_CUDA_ARCH_LIST="None" \
    USE_CUDA=0

# Torch ve torchvision CPU-only versiyonları için özel indirme URL'i
RUN pip wheel --no-cache-dir -w /wheels \
    torch==2.0.0+cpu --extra-index-url https://download.pytorch.org/whl/cpu \
    torchvision==0.15.0+cpu --extra-index-url https://download.pytorch.org/whl/cpu

# 7. Ultralytics aşaması
FROM base-builder AS builder-ultralytics
COPY requirements.txt .

# GPU kullanımını devre dışı bırak
ENV FORCE_CUDA=0 \
    CUDA_VISIBLE_DEVICES="" \
    TORCH_CUDA_ARCH_LIST="None" \
    USE_CUDA=0

# Ultralytics'i GPU olmadan kur - güncellenmiş sürüm
RUN pip wheel --no-cache-dir -w /wheels ultralytics==8.0.114 --no-deps

# 8. EasyOCR aşaması
FROM base-builder AS builder-easyocr
COPY requirements.txt .

# GPU kullanımını devre dışı bırak
ENV FORCE_CUDA=0 \
    CUDA_VISIBLE_DEVICES="" \
    TORCH_CUDA_ARCH_LIST="None" \
    USE_CUDA=0

# EasyOCR'ı GPU olmadan kur
RUN pip wheel --no-cache-dir -w /wheels easyocr==1.7.0 --no-deps

# 9. Minimal çalışma zamanı image'ı
FROM python:3.9-slim AS final

WORKDIR /app

# Çalışma zamanı için gereken minimum sistem bağımlılıkları
# apt-get update ve install komutlarını birlikte çalıştırın
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libgl1-mesa-glx \
    libglib2.0-0 \
    libsm6 \
    libxrender1 \
    libxext6 \
    postgresql-client \
    git \
    && apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# CPU-ONLY kurulum için çevresel değişkenleri ayarla ve PATH'i güncelle
ENV PYTHONUNBUFFERED=1 \
    FORCE_CUDA=0 \
    CUDA_VISIBLE_DEVICES="" \
    TORCH_CUDA_ARCH_LIST="None" \
    USE_CUDA=0 \
    ULTRALYTICS_GIT_ROOT="/app" \
    PATH="/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

# Debug klasörleri oluştur
RUN mkdir -p /app/debug_images /app/app/model/debug_plates /app/app/model


# Sort klasörü kopyalama işlemi kaldırıldı

# Temel paketleri önce doğrudan yükle (wheel kullanmadan)
RUN pip install --no-cache-dir \
    numpy==1.24.2 \
    fastapi==0.95.0 \
    uvicorn[standard]==0.21.1 \
    sqlalchemy==2.0.9 \
    psycopg2-binary==2.9.6 \
    pydantic==1.10.7 \
    python-multipart==0.0.6 \
    python-dotenv==1.0.0 \
    filterpy==1.4.5 \
    Pillow>=9.0.0 \
    websockets==11.0.3 \
    wsproto==1.2.0

# Diğer tüm wheelleri kopyala
COPY --from=builder-core /wheels /wheels/
COPY --from=builder-cv /wheels /wheels/
COPY --from=builder-pytorch /wheels /wheels/
COPY --from=builder-ultralytics /wheels /wheels/
COPY --from=builder-easyocr /wheels /wheels/

# Diğer tüm paketleri kur
RUN pip install --no-cache-dir --no-index --find-links=/wheels /wheels/* || true && \
    # CPU-only PyTorch ve torchvision kuruluşunu zorla
    pip install --no-cache-dir torch==2.0.0+cpu torchvision==0.15.0+cpu --extra-index-url https://download.pytorch.org/whl/cpu && \
    # CUDA bağımlılıklarını temizle (varsa)
    pip uninstall -y nvidia-cublas-cu12 nvidia-cuda-runtime-cu12 nvidia-cudnn-cu12 nvidia-cufft-cu12 || true && \
    # Ultralytics ve easyocr'ı bağımlılıklarıyla birlikte kur (güncellenmiş ultralytics sürümü)
    pip install --no-cache-dir ultralytics==8.0.114 easyocr==1.7.0 && \
    rm -rf /wheels

# Ultralytics için git yapılandırmasını oluştur
RUN mkdir -p /app/.git && touch /app/.git/config

# Uvicorn ve WebSocket kitaplıklarının doğru yüklendiğini kontrol et
RUN pip list | grep -e uvicorn -e websockets -e wsproto && \
    python -c "import uvicorn; print(f'Uvicorn {uvicorn.__version__} installed'); import websockets; print(f'Websockets {websockets.__version__} installed'); import wsproto; print(f'WSProto {wsproto.__version__} installed')" || echo "WebSocket kütüphaneleri doğru yüklenmedi!"

# Uygulamayı kopyala - uygulama kodları en son kopyalanmalı çünkü daha sık değişir
COPY app /app/app

# YOLO model dosyaları volume olarak bağlanacak

# Dosya izinlerini düzelt
RUN chmod -R 755 /app

# EasyOCR'ın ANTIALIAS sorunu için uyumluluk için patch uygula
RUN pip show easyocr && \
    if [ -f /usr/local/lib/python3.9/site-packages/easyocr/utils.py ]; then \
    sed -i 's/Image.ANTIALIAS/Image.LANCZOS/g' /usr/local/lib/python3.9/site-packages/easyocr/utils.py && \
    echo "EasyOCR ANTIALIAS patchi uygulandı"; \
    else \
    echo "EasyOCR utils.py dosyası bulunamadı"; \
    fi

# Geliştirme sırasında, kod değişikliklerini otomatik algılamak için reload modunda çalış
# Tam yolu belirt
CMD ["/usr/local/bin/uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--reload", "--log-level", "debug"]
