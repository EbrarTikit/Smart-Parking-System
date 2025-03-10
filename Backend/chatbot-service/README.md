# Chatbot Service

Bu servis, Gemini API kullanarak otopark kullanıcılarına yardımcı olan bir chatbot servisidir. Kullanıcı sohbet geçmişi Redis veritabanında tutulur.

## Özellikler

- Gemini API ile doğal dil işleme
- Redis ile kullanıcı sohbet geçmişi yönetimi
- FastAPI ile hızlı ve modern API

## Kurulum

### Gereksinimler

- Python 3.9+
- Redis 6.0+

### Yerel Geliştirme

1. Sanal ortam oluşturun ve aktifleştirin:

```bash
python -m venv venv
source venv/bin/activate  # Linux/Mac
venv\Scripts\activate  # Windows
```

2. Bağımlılıkları yükleyin:

```bash
pip install -r requirements.txt
```

3. Redis'i başlatın:

```bash
# Docker ile
docker run --name redis -p 6379:6379 -d redis

# Veya yerel olarak kurulu Redis'i başlatın
redis-server
```

4. .env dosyasını yapılandırın:

```
GEMINI_API_KEY=your_api_key
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DB=0
REDIS_PASSWORD=
REDIS_EXPIRY=1800
```

5. Uygulamayı başlatın:

```bash
uvicorn app.main:app --reload
```

### Docker ile Çalıştırma

1. Docker Compose ile çalıştırma:

```yaml
# docker-compose.yml
version: "3"

services:
  redis:
    image: redis:6
    ports:
      - "6379:6379"

  chatbot:
    build: .
    ports:
      - "8000:8000"
    environment:
      - GEMINI_API_KEY=your_api_key
      - REDIS_HOST=redis
    depends_on:
      - redis
```

2. Başlatın:

```bash
docker-compose up -d
```

## API Kullanımı

### Sohbet Başlatma/Devam Ettirme

```
POST /api/v1/chat
```

İstek:

```json
{
  "message": "Otopark ücretleri nedir?",
  "session_id": "optional-session-id"
}
```

Yanıt:

```json
{
  "response": "Otoparkımızda saatlik ücret 10 TL'dir...",
  "session_id": "generated-or-provided-session-id"
}
```

### Sohbet Geçmişini Görüntüleme

```
GET /api/v1/chat/{session_id}/history
```

### Sohbet Geçmişini Silme

```
DELETE /api/v1/chat/{session_id}
```

### Aktif Oturumları Listeleme

```
GET /api/v1/sessions
```
