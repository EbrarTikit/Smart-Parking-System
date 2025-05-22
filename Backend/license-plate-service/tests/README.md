# License Plate Service Test Suite

Bu klasör, License Plate Service için test modüllerini içermektedir. Kapsamlı test süiti, sistemin her bileşeninin beklendiği gibi çalıştığını doğrulamak için birim testleri, entegrasyon testleri ve API testlerini içerir.

## Test Dosyaları

| Dosya Adı                    | Açıklama                                                                                                                                           |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| `test_fee_calculation.py`    | Park ücreti hesaplama fonksiyonunu test eder. Farklı süreler, oran ve senaryo testlerini içerir.                                                   |
| `test_websocket_manager.py`  | WebSocket bağlantı yöneticisinin işlevselliğini test eder - bağlantı yönetimi, mesaj gönderme ve hata işleme.                                      |
| `test_process_plate_apis.py` | Plaka işleme API'lerini test eder (/process-plate-entry ve /process-plate-exit)                                                                    |
| `test_integration.py`        | Farklı servisler arasındaki entegrasyonu test eder. Özellikle License Plate Service ve Parking Management Service arasındaki etkileşime odaklanır. |
| `test_models.py`             | Veritabanı modelleri ve CRUD operasyonlarını test eder.                                                                                            |
| `test_api.py`                | API endpoint'lerini ve rotaları test eder.                                                                                                         |

## Test Kategorileri

### Birim Testleri

- Bağımsız fonksiyonlar ve sınıflar için testler
- Her bileşenin diğer bileşenlerden izole edilmiş şekilde çalışmasını doğrular
- WebSocket yöneticisi, ücret hesaplama mantığı, veritabanı erişim katmanı gibi.

### API Testleri

- HTTP endpoint'lerini doğrudan test eder
- Girdi doğrulama, hata işleme ve başarılı senaryoları kontrol eder
- TestClient kullanarak REST API'lerini simüle eder

### Entegrasyon Testleri

- Farklı servisler ve bileşenler arasındaki etkileşimleri test eder
- Parking Management Service ile bağlantıyı test eder
- Tam giriş-çıkış akışını end-to-end olarak simüle eder

## Testleri Çalıştırma

### Tüm testleri çalıştırma

```bash
python -m pytest tests/
```

### Belirli bir test dosyasını çalıştırma

```bash
python -m pytest tests/test_fee_calculation.py
```

### Verbose mod ile çalıştırma

```bash
python -m pytest -v tests/
```

### Belirli bir test fonksiyonunu çalıştırma

```bash
python -m pytest tests/test_fee_calculation.py::test_calculate_fee_for_one_hour
```

## Mock Kullanımı

Testler, dış bağımlılıkları simüle etmek için mock nesnelerini kullanır:

- `unittest.mock.patch` - Sınıfları ve fonksiyonları geçici olarak değiştirmek için
- `MagicMock` - Özel davranışlar ve dönüş değerleri için
- API yanıtları, veritabanı işlemleri ve dış servisler için mock'lar

## Test Verileri

- `conftest.py` - Paylaşılan test fixture'larını içerir
- `test_data/` - Test için örnek resimler ve JSON yanıtlar

## Kapsam Raporu Oluşturma

```bash
python -m pytest --cov=app tests/
```

Ayrıntılı bir kapsam raporu HTML formatında oluşturmak için:

```bash
python -m pytest --cov=app --cov-report=html tests/
```

## Hata Ayıklama

Test hataları ile karşılaşıldığında detaylı çıktı için:

```bash
python -m pytest -vvs tests/
```

Burada:

- `-v` veya `-vv`: Çıktı ayrıntı seviyesi
- `-s`: print() çıktılarını göster
