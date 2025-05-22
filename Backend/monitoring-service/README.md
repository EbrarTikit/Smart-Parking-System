# Smart Parking Monitoring Service

Bu servis, Smart Parking System'in tüm bileşenlerini izlemek, log kayıtlarını toplamak ve metriklerini görselleştirmek için tasarlanmış bir monitoring servisidir.

## Özellikler

- **Log Yönetimi**: Elasticsearch üzerinden tüm servislerin log kayıtlarını sorgulama
- **Metrik İzleme**: Prometheus üzerinden toplanan metrikleri sorgulama ve analiz etme
- **Servis Durumu**: Tüm servislerin sağlık durumunu izleme
- **Dashboard**: Sistem performansı ve durumu için özet görünüm

## Kurulum

### Gereksinimler

- Python 3.9+
- Elasticsearch 7.x
- Prometheus 2.x
- Docker (opsiyonel)

### Lokal Kurulum

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

3. Uygulamayı çalıştırın:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8010 --reload
```

### Docker ile Kurulum

```bash
docker build -t smart-parking-monitoring-service .
docker run -p 8010:8010 --name monitoring-service smart-parking-monitoring-service
```

## Kullanım

### API Endpointleri

- **GET /health**: Servisin sağlık durumu
- **GET /logs**: Log kayıtlarını sorgulama
- **GET /errors**: Hata kayıtlarını sorgulama
- **GET /services/status**: Tüm servislerin durumu
- **GET /metrics/{metric_name}**: Belirli bir metriğin verilerini getirme
- **GET /dashboard/summary**: Dashboard için özet veriler

### Örnek İstekler

#### Log Kayıtlarını Sorgulama

```bash
curl "http://localhost:8010/logs?service=license-plate-service&level=ERROR&limit=10"
```

#### Servis Durumunu Kontrol Etme

```bash
curl "http://localhost:8010/services/status"
```

#### Metrik Verilerini Getirme

```bash
curl "http://localhost:8010/metrics/http_requests_total?start_time=2023-09-01T00:00:00&end_time=2023-09-02T00:00:00&step=5m"
```

## Entegrasyon

Bu servis, aşağıdaki bileşenlerle entegre çalışır:

- **Elasticsearch**: Log kayıtları için
- **Prometheus**: Metrikler için
- **Grafana**: Görselleştirme için (opsiyonel)

## Yapılandırma

Servis yapılandırması için aşağıdaki çevresel değişkenler kullanılabilir:

- `ELASTICSEARCH_HOST`: Elasticsearch sunucu adresi (varsayılan: elasticsearch)
- `ELASTICSEARCH_PORT`: Elasticsearch port numarası (varsayılan: 9200)
- `PROMETHEUS_HOST`: Prometheus sunucu adresi (varsayılan: prometheus)
- `PROMETHEUS_PORT`: Prometheus port numarası (varsayılan: 9090)
- `LOG_LEVEL`: Log seviyesi (varsayılan: INFO)

## Geliştirme

### Yeni Metrik Ekleme

Yeni bir metrik eklemek için `app/config.py` dosyasındaki `DEFAULT_METRICS` listesine metrik adını ekleyin.

### Yeni Servis Ekleme

Yeni bir servis eklemek için `app/config.py` dosyasındaki `MONITORED_SERVICES` listesine servis adını ekleyin.
