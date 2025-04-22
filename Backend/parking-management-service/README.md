# Otopark Yönetim Servisi

Otopark Yönetim Servisi, akıllı otopark sisteminin merkezi bileşenidir. Bu servis, aracın otoparka girişinden çıkışına kadar olan tüm süreçleri yönetir ve diğer servislerle iletişim kurarak koordinasyonu sağlar.

## Özellikler

- Araç giriş/çıkış yönetimi
- Park yeri tahsis işlemleri
- SAGA pattern ile dağıtık işlem yönetimi
- Fallback stratejileri ile hata toleransı
- Telemetri ve izleme desteği

## SAGA Pattern

SAGA pattern, birden fazla servis arasında dağıtık işlemlerin yönetilmesi için kullanılan bir tasarım desenidir. Her işlem adımı ayrı bir serviste gerçekleştirilir ve bir adım başarısız olursa, daha önce tamamlanan adımlar için telafi işlemleri (compensation) gerçekleştirilir.

### SAGA Yaklaşımları

1. **Koreografi Tabanlı SAGA**: Servisler birbirleriyle doğrudan iletişim kurarak SAGA işlemlerini gerçekleştirir.
2. **Orkestrasyon Tabanlı SAGA**: Merkezi bir orkestratör, tüm işlem adımlarını koordine eder ve gerektiğinde telafi işlemlerini başlatır.

Bu projede Orkestrasyon Tabanlı SAGA yaklaşımını kullanıyoruz.

### Projemizdeki SAGA Akışları

1. **Araç Giriş Akışı**:

   - Plaka doğrulama (Plaka Tanıma Servisi)
   - Park yeri tahsisi (Otopark Yönetim Servisi)
   - Giriş kaydı oluşturma (Plaka Tanıma Servisi)

2. **Araç Çıkış Akışı**:
   - Plaka doğrulama (Plaka Tanıma Servisi)
   - Park yerinin boşaltılması (Otopark Yönetim Servisi)
   - Çıkış kaydı oluşturma (Plaka Tanıma Servisi)

## Fallback Stratejileri

SAGA işlemleri sırasında bir servis çağrısının başarısız olması durumunda kullanılan stratejiler:

### 1. Retry with Backoff (Üstel Gecikmeli Yeniden Deneme)

Bir servis çağrısı başarısız olduğunda, belirli bir gecikme ile tekrar deneme stratejisidir. Her başarısız denemeden sonra gecikme süresi artırılır.

```python
success, result = await retry_with_backoff(
    call_service,
    retry_count=3,
    initial_delay=0.5,
    backoff_factor=2.0
)
```

### 2. Circuit Breaker (Devre Kesici)

Bir servis sürekli hata veriyorsa, belirli bir süre boyunca bu servise yapılan çağrıları bloke ederek servisi korumak için kullanılır.

```python
circuit_breaker = CircuitBreaker("license_plate_service", failure_threshold=5)
success, result = await circuit_breaker.execute(service_func, operation, request_data)
```

### 3. Alternative Service (Alternatif Servis)

Bir servis kullanılamadığında, aynı işlevselliği sağlayan alternatif bir servise yönlendirme yapar.

```python
alt_strategy = AlternativeServiceStrategy("primary_service", ["backup_service1", "backup_service2"])
success, result = await alt_strategy.execute(primary_func, alternative_funcs)
```

### 4. Cache Fallback (Önbellek Dönüşü)

Bir servis kullanılamadığında, daha önce alınmış olan önbellekteki sonuçları kullanır.

```python
cache_strategy = CacheFallbackStrategy(cache_ttl_seconds=300)
success, result = await cache_strategy.execute(call_service, cache_key="unique_key")
```

### 5. Load Balancing (Yük Dengeleme)

Birden fazla servis instance'ı arasında yük dağıtımı yaparak, hizmet sürekliliğini sağlar.

```python
lb_strategy = LoadBalancingStrategy(LoadBalancingStrategy.ROUND_ROBIN)
service = lb_strategy.get_service([service1, service2, service3])
result = await service(operation, data)
```

## Örnek SAGA Uygulaması

Aşağıdaki örnek, fallback stratejileri ile güçlendirilmiş bir SAGA implementasyonunu göstermektedir:

```python
saga = EnhancedSaga(str(uuid.uuid4()), "Araç Giriş İşlemi")

verify_step = EnhancedSagaStep("verify_license_plate", "license_plate_service", "verify_license_plate")
verify_step.request_data = {"license_plate": "34ABC123"}
verify_step.retry_count = 2
verify_step.use_cache_fallback = True
saga.add_step(verify_step)

allocate_step = EnhancedSagaStep("allocate_parking_space", "parking_mgmt_service", "allocate_parking_space")
allocate_step.request_data = {"license_plate": "34ABC123"}
allocate_step.retry_count = 3
allocate_step.use_circuit_breaker = True
saga.add_step(allocate_step)

entry_step = EnhancedSagaStep("create_entry_record", "license_plate_service", "create_entry_record")
entry_step.request_data = {"license_plate": "34ABC123"}
entry_step.use_circuit_breaker = True
entry_step.alternative_services = ["alternative_license_plate_service"]
saga.add_step(entry_step)

orchestrator = EnhancedSagaOrchestrator()
result_saga = await orchestrator.execute_saga(saga)
```

## Telafi İşlemleri (Compensation)

SAGA işlemi sırasında herhangi bir adım başarısız olursa, daha önce başarıyla tamamlanmış adımlar için telafi işlemleri gerçekleştirilir. Bu işlemler, tam olarak ters sırada (sondan başa) uygulanır.

Örnek telafi işlemleri:

1. **Park yeri tahsisi** için telafi: Park yerini serbest bırakma
2. **Giriş kaydı oluşturma** için telafi: Giriş kaydını iptal etme

## Test Etme

Oluşturduğumuz örnek dosyaları çalıştırmak için:

```bash
python -m app.sagas.fallback_strategies

python -m app.sagas.saga_with_fallbacks
```

## Kurulum

```bash
pip install -r requirements.txt

docker build -t parking-management-service .
docker run -p 8000:8000 parking-management-service
```
