# Chatbot Service

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Python](https://img.shields.io/badge/Python-3.9+-blue.svg)](https://www.python.org/downloads/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.95.0-green.svg)](https://fastapi.tiangolo.com/)
[![Redis](https://img.shields.io/badge/Redis-6.0+-red.svg)](https://redis.io/)

Chatbot Service, Smart Parking System için yapay zeka destekli bir sohbet asistanıdır. Google Gemini API'si kullanılarak geliştirilmiş bu servis, kullanıcılara otopark ile ilgili sorularına yanıt verir, otopark durumu hakkında bilgi sağlar ve kullanıcılara rehberlik eder.

![Chatbot Architecture](https://i.imgur.com/PlaceHolder.png)

## İçindekiler

- [Chatbot Service](#chatbot-service)
  - [İçindekiler](#i̇çindekiler)
  - [Mimari Yapı](#mimari-yapı)
  - [Özellikler](#özellikler)
  - [Gereksinimler](#gereksinimler)
  - [Kurulum](#kurulum)
    - [1. Yerel Geliştirme Ortamı](#1-yerel-geliştirme-ortamı)
    - [2. Docker Kullanarak](#2-docker-kullanarak)
  - [Konfigürasyon](#konfigürasyon)
  - [API Dokümantasyonu](#api-dokümantasyonu)
    - [Sohbet Endpoint'leri](#sohbet-endpointleri)
    - [Yönetim Endpoint'leri](#yönetim-endpointleri)
  - [Kullanım Örnekleri](#kullanım-örnekleri)
  - [Test Etme](#test-etme)
  - [Performans ve Sınırlamalar](#performans-ve-sınırlamalar)
  - [Sorun Giderme](#sorun-giderme)
  - [Katkıda Bulunma](#katkıda-bulunma)
  - [Lisans](#lisans)
  - [İletişim](#i̇letişim)

## Mimari Yapı

Chatbot Service, aşağıdaki bileşenlerden oluşur:

1. **FastAPI Web Servisi**: RESTful API sağlayan web servisi
2. **Gemini API Entegrasyonu**: Google'ın Gemini AI modeline bağlantı
3. **Redis Cache**: Sohbet geçmişi ve oturum yönetimi için önbellek sistemi
4. **Konuşma Yönetimi**: Kullanıcı oturumlarını ve mesaj akışını yöneten modül

Bu mimari yapı, ölçeklenebilir, yüksek performanslı ve güvenilir bir chatbot servisi sağlar.

## Özellikler

- **Doğal Dil İşleme**: Gemini AI ile gelişmiş doğal dil anlama
- **Çoklu Oturum Desteği**: Aynı anda birden çok kullanıcı için sohbet oturumları yönetimi
- **Sohbet Geçmişi**: Kullanıcı sohbet geçmişini saklama ve yönetme
- **Bağlam Koruması**: Sohbet sırasında bağlamı koruma ve takip etme
- **Hızlı Yanıt Süresi**: Optimize edilmiş önbellek ile düşük gecikme süresi
- **Otopark Alanına Özel Bilgi**: Otopark işlemleri için özelleştirilmiş yanıtlar
- **Kolay Entegrasyon**: Diğer mikroservislerle kolay entegrasyon için RESTful API
- **Yüksek Test Kapsamı**: %92 test kapsamı ile güvenilir kod tabanı

## Gereksinimler

- Python 3.9 veya üzeri
- Redis 6.0 veya üzeri
- Google Gemini API anahtarı
- Docker (opsiyonel)

## Kurulum

### 1. Yerel Geliştirme Ortamı

```bash
# Repo'yu klonla
git clone https://github.com/username/smart-parking-system.git
cd smart-parking-system/Backend/chatbot-service

# Sanal ortam oluştur ve aktif et
python -m venv venv
# Windows
venv\Scripts\activate
# Linux/macOS
source venv/bin/activate

# Bağımlılıkları yükle
pip install -r requirements.txt

# .env dosyasını oluştur
cp .env.example .env
# .env dosyasını düzenle ve Gemini API anahtarını ekle

# Redis'i başlat (Docker kullanarak)
docker run -d -p 6379:6379 --name redis-for-chatbot redis:6

# Servisi başlat
uvicorn app.main:app --reload --port 8001
```

Servis http://localhost:8001 adresinde çalışacak ve Swagger API dokümantasyonu http://localhost:8001/docs adresinden erişilebilir olacaktır.

### 2. Docker Kullanarak

Tüm bağımlılıkları ve gereksinimleri içeren bir Docker container'ı kullanarak servisi çalıştırabilirsiniz:

```bash
# Ana dizine git
cd smart-parking-system/Backend

# Yalnızca chatbot servisini başlat
docker-compose up -d chatbot-service

# Veya tüm servisleri başlat
docker-compose up -d
```

## Konfigürasyon

Chatbot servisi, aşağıdaki çevre değişkenleri ile yapılandırılabilir (.env dosyası):

| Değişken       | Açıklama                         | Varsayılan Değer |
| -------------- | -------------------------------- | ---------------- |
| REDIS_HOST     | Redis sunucu adresi              | localhost        |
| REDIS_PORT     | Redis sunucu portu               | 6379             |
| REDIS_PASSWORD | Redis şifresi (varsa)            | (boş)            |
| REDIS_DB       | Redis veritabanı numarası        | 0                |
| GEMINI_API_KEY | Google Gemini API anahtarı       | (gerekli)        |
| MAX_HISTORY    | Saklanacak maksimum mesaj sayısı | 20               |
| SESSION_TTL    | Oturum yaşam süresi (saniye)     | 86400 (24 saat)  |
| LOG_LEVEL      | Günlük kayıt seviyesi            | INFO             |

## API Dokümantasyonu

### Sohbet Endpoint'leri

#### POST /api/v1/chat

Kullanıcıdan gelen bir mesajı işler ve yanıt döndürür.

**İstek Gövdesi (JSON):**

```json
{
  "message": "Antalya'da otopark fiyatları ne kadar?",
  "session_id": "optional-session-id" // Opsiyonel, belirtilmezse yeni oturum oluşturulur
}
```

**Başarılı Yanıt (200 OK):**

```json
{
  "response": "Antalya'da otopark fiyatları bölgeye göre değişiklik göstermektedir...",
  "session_id": "unique-session-id"
}
```

#### GET /api/v1/chat/{session_id}/history

Belirli bir oturuma ait sohbet geçmişini getirir.

**Başarılı Yanıt (200 OK):**

```json
[
  {
    "role": "user",
    "content": "Antalya'da otopark fiyatları ne kadar?",
    "timestamp": "2023-05-15T14:30:45.123456"
  },
  {
    "role": "assistant",
    "content": "Antalya'da otopark fiyatları bölgeye göre değişiklik göstermektedir...",
    "timestamp": "2023-05-15T14:30:47.456789"
  }
]
```

#### DELETE /api/v1/chat/{session_id}

Belirli bir oturumun sohbet geçmişini siler.

**Başarılı Yanıt (200 OK):**

```json
{
  "success": true,
  "message": "Sohbet geçmişi başarıyla silindi"
}
```

### Yönetim Endpoint'leri

#### GET /api/v1/sessions

Aktif sohbet oturumlarını listeler.

**Başarılı Yanıt (200 OK):**

```json
{
  "sessions": ["session-id-1", "session-id-2", ...],
  "count": 10
}
```

#### GET /api/v1/health

Servis sağlık durumunu kontrol eder.

**Başarılı Yanıt (200 OK):**

```json
{
  "status": "ok",
  "timestamp": "2023-05-15T15:45:30.123456"
}
```

## Kullanım Örnekleri

### cURL ile API'yi Test Etme

```bash
# Yeni sohbet başlatma
curl -X POST http://localhost:8001/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Merhaba, otopark fiyatları hakkında bilgi alabilir miyim?"}'

# Varolan oturuma mesaj gönderme
curl -X POST http://localhost:8001/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Konyaaltı bölgesindeki otoparklar?", "session_id":"YOUR_SESSION_ID"}'

# Sohbet geçmişini getirme
curl -X GET http://localhost:8001/api/v1/chat/YOUR_SESSION_ID/history
```

### Python ile API'yi Kullanma

```python
import requests

# Yeni sohbet başlatma
response = requests.post(
    "http://localhost:8001/api/v1/chat",
    json={"message": "Merhaba, otopark fiyatları hakkında bilgi alabilir miyim?"}
)
data = response.json()
session_id = data["session_id"]
print(f"Yanıt: {data['response']}")

# Aynı oturumda devam etme
response = requests.post(
    "http://localhost:8001/api/v1/chat",
    json={"message": "Konyaaltı bölgesindeki otoparklar?", "session_id": session_id}
)
print(f"Yanıt: {response.json()['response']}")
```

## Test Etme

Chatbot Service, kapsamlı bir test süiti ile gelir. Tüm test dosyaları ve test yardımcı araçları `tests` klasöründe bulunmaktadır. Test süiti hakkında ayrıntılı bilgi için [tests/README.md](tests/README.md) dosyasına bakın.

Testleri çalıştırmak için:

```bash
# Tüm testleri çalıştırma
python -m pytest

# HTML raporlu kod kapsamı oluşturma
python -m pytest --cov=app --cov-report=html
```

## Performans ve Sınırlamalar

- **Yanıt Süresi**: Ortalama yanıt süresi 1-3 saniyedir (ağ gecikmesi dahil)
- **Eşzamanlı Bağlantılar**: Servis, donanıma bağlı olarak yüzlerce eşzamanlı bağlantıyı destekleyebilir
- **Mesaj Boyutu**: Tek bir mesaj için maksimum boyut 4096 karakterdir
- **Oturum Ömrü**: Varsayılan olarak oturumlar 24 saat sonra otomatik olarak temizlenir
- **API Limitleri**: Gemini API kullanımı, Google'ın kota sınırlamalarına tabidir

## Sorun Giderme

**Sorun**: Servis yanıt vermiyor.
**Çözüm**: Redis bağlantısını ve Gemini API anahtarının doğru olduğunu kontrol edin.

**Sorun**: Redis bağlantı hatası.
**Çözüm**: Redis sunucusunun çalıştığından ve doğru host/port kullanıldığından emin olun.

**Sorun**: Yetersiz yanıtlar.
**Çözüm**: Gemini API anahtarını kontrol edin ve sohbet bağlamının doğru şekilde korunduğundan emin olun.

**Sorun**: Yüksek bellek kullanımı.
**Çözüm**: `MAX_HISTORY` değerini düşürerek sohbet geçmişi boyutunu sınırlandırın.

## Katkıda Bulunma

1. Bu depoyu fork edin
2. Kendi branch'inizi oluşturun (`git checkout -b feature/amazing-feature`)
3. Değişikliklerinizi commit edin (`git commit -m 'Add some amazing feature'`)
4. Branch'inizi push edin (`git push origin feature/amazing-feature`)
5. Pull Request açın

## Lisans

Bu proje MIT Lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

## İletişim

Proje Ekibi - [example@email.com](mailto:example@email.com)

Proje Linki: [https://github.com/username/smart-parking-system](https://github.com/username/smart-parking-system)
