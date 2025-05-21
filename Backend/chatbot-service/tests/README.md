# Chatbot Service - Test Dokümantasyonu

Bu klasör, Chatbot Service için kapsamlı test süitini içermektedir. Testler, servisin tüm bileşenlerini ve fonksiyonlarını kapsayacak şekilde tasarlanmıştır.

## Test Dosyaları ve Açıklamaları

| Test Dosyası              | Açıklama                                                                                                                                       |
| ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| **test_redis_client.py**  | Redis bağlantısı, veri saklama ve alma fonksiyonlarını test eder. Bağlantı hataları, zaman aşımı durumları ve başarılı senaryolar test edilir. |
| **test_chat_history.py**  | Konuşma geçmişi yönetimi fonksiyonlarının testlerini içerir. Mesaj ekleme, konuşma getirme ve silme işlemleri test edilir.                     |
| **test_gemini_api.py**    | Google Gemini AI API entegrasyonunu test eder. API istekleri, yanıt işleme ve hata yönetimi test edilir.                                       |
| **test_routes.py**        | API endpoint'lerinin (sağlık kontrolü, sohbet, geçmiş ve oturum yönetimi) doğru çalıştığını test eder.                                         |
| **test_integration.py**   | Farklı bileşenlerin birlikte çalışmasını test eder. Redis, Gemini API ve FastAPI katmanları arasındaki entegrasyonu kontrol eder.              |
| **test_load.py**          | Servisin yük altındaki performansını ve kararlılığını ölçer. Paralel istekler, uzun sohbetler ve karışık endpoint çağrıları test edilir.       |
| **conftest.py**           | Pytest fixture'ları ve mock objelerini içerir. Test ortamını hazırlar ve izole testler yapılmasını sağlar.                                     |
| **pytest.ini**            | Pytest yapılandırması, test kapsamı ayarları ve log seviyesi gibi yapılandırmaları içerir.                                                     |
| **run_tests.sh**          | Linux/macOS için test çalıştırma script'i. Farklı test senaryolarını kolayca çalıştırmayı sağlar.                                              |
| **run_tests_windows.bat** | Windows için test çalıştırma batch dosyası. Farklı test senaryolarını kolayca çalıştırmayı sağlar.                                             |

## Test Kategorileri

### 1. Birim Testleri (Unit Tests)

Birim testleri, kodun en küçük parçalarını izole şekilde test eder:

- **RedisClient Testleri**: Redis bağlantısı, veri saklama/getirme, hata yönetimi
- **ConversationHistory Testleri**: Mesaj ekleme, konuşma getirme, temizleme ve oturum yönetimi
- **Gemini API Testleri**: API isteği oluşturma, yanıt işleme, hata yönetimi

### 2. Entegrasyon Testleri (Integration Tests)

Entegrasyon testleri, birden fazla bileşenin birlikte çalışmasını test eder:

- **Tam Sohbet İş Akışı**: Kullanıcı mesajından Gemini API yanıtına kadar tam süreç
- **Redis Entegrasyonu**: Verilerin doğru şekilde saklanması ve getirilmesi
- **API Endpoint Entegrasyonu**: HTTP isteklerinden iş mantığına ve veritabanına kadar tam süreç

### 3. API Endpoint Testleri

REST API endpoint'lerinin doğru çalıştığını doğrular:

- **Sağlık Kontrolü**: `/api/v1/health` endpoint'inin düzgün yanıt verdiğini kontrol eder
- **Sohbet İşlemleri**: `/api/v1/chat` endpoint'inin mesajları işlediğini ve doğru yanıt döndürdüğünü kontrol eder
- **Geçmiş İşlemleri**: `/api/v1/chat/{session_id}/history` endpoint'inin konuşma geçmişini doğru getirdiğini kontrol eder
- **Oturum Yönetimi**: Oturum oluşturma, listeleme ve silme işlemlerinin doğru çalıştığını kontrol eder

### 4. Yük ve Performans Testleri (Load Tests)

Servisin yük altındaki davranışını ölçer:

- **Ardışık İstekler**: Arka arkaya gelen isteklerin doğru işlendiğini ve performansın kabul edilebilir seviyede olduğunu kontrol eder
- **Paralel İstekler**: Aynı anda gelen çoklu isteklerin düzgün işlendiğini kontrol eder
- **Karışık Endpoint Yükü**: Farklı endpoint'lere gelen karışık isteklerin hepsinin doğru şekilde işlendiğini kontrol eder
- **Uzun Sohbet Performansı**: Çok sayıda mesaj içeren uzun sohbetlerin performansını ve bellek kullanımını ölçer

## Testleri Çalıştırma

### Linux / macOS Kullanıcıları İçin

Bash script'i ile testleri çalıştırabilirsiniz:

```bash
# Çalıştırma izni ver
chmod +x tests/run_tests.sh

# Tüm testleri çalıştır (yük testleri hariç)
./tests/run_tests.sh

# Sadece birim testlerini çalıştır
./tests/run_tests.sh unit

# Sadece entegrasyon testlerini çalıştır
./tests/run_tests.sh integration

# Sadece yük testlerini çalıştır
./tests/run_tests.sh load

# Tüm testleri çalıştır (yük testleri dahil)
./tests/run_tests.sh all

# HTML raporla birim testlerini çalıştır
./tests/run_tests.sh unit html
```

### Windows Kullanıcıları İçin

Windows batch dosyası ile testleri çalıştırabilirsiniz:

```cmd
# Tüm testleri çalıştır (yük testleri hariç)
tests\run_tests_windows.bat

# Sadece birim testlerini çalıştır
tests\run_tests_windows.bat unit

# Sadece entegrasyon testlerini çalıştır
tests\run_tests_windows.bat integration

# Sadece yük testlerini çalıştır
tests\run_tests_windows.bat load

# Tüm testleri çalıştır (yük testleri dahil)
tests\run_tests_windows.bat all

# HTML raporla birim testlerini çalıştır
tests\run_tests_windows.bat unit html
```

### Manuel Test Çalıştırma

Pytest komutlarını doğrudan kullanarak:

```bash
# Sanal ortamı aktif et
# Windows: venv\Scripts\activate
# Linux/macOS: source venv/bin/activate

# Tüm testleri çalıştır
python -m pytest

# Belirli bir test dosyasını çalıştır
python -m pytest tests/test_redis_client.py

# Belirli bir test fonksiyonunu çalıştır
python -m pytest tests/test_redis_client.py::TestRedisClient::test_init_with_default_values

# Detaylı çıktı ile çalıştır
python -m pytest -v

# Kod kapsamı raporu oluştur
python -m pytest --cov=app --cov-report=term

# HTML kod kapsamı raporu oluştur
python -m pytest --cov=app --cov-report=html
```

## Test Kapsamı ve Raporlama

Chatbot servisi için mevcut test kapsamı %92'dir. Bu yüksek kapsam oranı, kodun güvenilirliğini ve kalitesini göstermektedir.

Kapsamlı HTML raporu oluşturmak için:

```bash
python -m pytest --cov=app --cov-report=html
```

Raporda her bir modül için satır bazında kapsam bilgisini görebilirsiniz:

| Modül           | Kapsam % | Açıklama                        |
| --------------- | -------- | ------------------------------- |
| redis_client.py | 100%     | Redis bağlantısı ve veri işleme |
| chat_history.py | 91%      | Konuşma geçmişi yönetimi        |
| gemini_api.py   | 100%     | Gemini API entegrasyonu         |
| routes.py       | 84%      | API endpoint'leri               |
| main.py         | 86%      | Ana uygulama                    |

## Test Ortamı ve Mock'lar

Testler izole bir ortamda çalışır ve gerçek dış servislere bağlanmaz:

- **Redis Mock**: Gerçek Redis sunucusu yerine bellek içi mock kullanılır
- **Gemini API Mock**: Gerçek API istekleri göndermek yerine önceden tanımlanmış yanıtlar kullanılır
- **Fixture'lar**: Test verilerinin tutarlı şekilde oluşturulmasını sağlar

## Hata Ayıklama İpuçları

Testlerde hata ayıklamak için:

1. `-v` veya `-vv` bayrağını kullanarak daha detaylı çıktı alın
2. `--pdb` bayrağı ile hata durumunda Python debugger'ı başlatın
3. Belirli bir test sınıfını veya fonksiyonunu izole etmek için tam yolu belirtin
4. `PYTEST_ADDOPTS="-v"` çevre değişkeni ile varsayılan detay seviyesini artırın

```bash
# Debug modunda çalıştır
python -m pytest --pdb

# Test sırasında print ifadelerini göster
python -m pytest -v --capture=no
```

## CI/CD Entegrasyonu

Bu test süiti, CI/CD pipeline'lara kolayca entegre edilebilir. Örneğin GitHub Actions ile:

```yaml
- name: Run Tests
  run: |
    pip install -r requirements.txt
    python -m pytest --cov=app
```

Test sonuçları başarısız olduğunda pipeline'ın da başarısız olması, hatanın erken fark edilmesini sağlar.
