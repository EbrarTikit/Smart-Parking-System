@echo off
echo ========== Monitoring Service Test Ortami ==========

:: Komut satiri parametrelerini kontrol et
set TEST_TYPE=%1
if "%TEST_TYPE%"=="" set TEST_TYPE=all

set FORMAT=%2
if "%FORMAT%"=="" set FORMAT=terminal

:: Renklendirme için ANSI kodlari
echo [36m^> Test ortami hazirlaniyor...[0m

:: Virtual environment kontrol et ve olustur
if not exist venv (
    echo [33m^> Virtual environment bulunamadi, olusturuluyor...[0m
    python -m venv venv
    call venv\Scripts\activate
    pip install --upgrade pip
    pip install -r requirements-minimal.txt
) else (
    echo [32m^> Virtual environment zaten mevcut, aktive ediliyor...[0m
    call venv\Scripts\activate
)

echo [32m^> Bagimliliklar yuklendi.[0m

:: Test turune gore uygun komutlari calistir
if "%TEST_TYPE%"=="elasticsearch" (
    echo [36m^> Elasticsearch client testleri calistiriliyor...[0m
    if "%FORMAT%"=="html" (
        python -m pytest -v tests\test_elasticsearch_client.py --cov=app --cov-report=html
    ) else (
        python -m pytest -v tests\test_elasticsearch_client.py --cov=app
    )
) else if "%TEST_TYPE%"=="prometheus" (
    echo [36m^> Prometheus client testleri calistiriliyor...[0m
    if "%FORMAT%"=="html" (
        python -m pytest -v tests\test_prometheus_client.py --cov=app --cov-report=html
    ) else (
        python -m pytest -v tests\test_prometheus_client.py --cov=app
    )
) else if "%TEST_TYPE%"=="service" (
    echo [36m^> Service status testleri calistiriliyor...[0m
    if "%FORMAT%"=="html" (
        python -m pytest -v tests\test_service_status.py --cov=app --cov-report=html
    ) else (
        python -m pytest -v tests\test_service_status.py --cov=app
    )
) else if "%TEST_TYPE%"=="dashboard" (
    echo [36m^> Dashboard testleri calistiriliyor...[0m
    if "%FORMAT%"=="html" (
        python -m pytest -v tests\test_dashboard.py --cov=app --cov-report=html
    ) else (
        python -m pytest -v tests\test_dashboard.py --cov=app
    )
) else if "%TEST_TYPE%"=="api" (
    echo [36m^> API testleri calistiriliyor...[0m
    if "%FORMAT%"=="html" (
        python -m pytest -v tests\test_main.py --cov=app --cov-report=html
    ) else (
        python -m pytest -v tests\test_main.py --cov=app
    )
) else if "%TEST_TYPE%"=="all" (
    echo [36m^> Tum testler calistiriliyor...[0m
    if "%FORMAT%"=="html" (
        python -m pytest -v tests\ --cov=app --cov-report=html
    ) else (
        python -m pytest -v tests\ --cov=app
    )
) else (
    echo [31m^> Gecersiz test turu: %TEST_TYPE%[0m
    echo Kullanim: %0 [elasticsearch^|prometheus^|service^|dashboard^|api^|all] [terminal^|html]
    exit /b 1
)

:: Test sonuclarini goster
if %errorlevel% == 0 (
    echo [32m^> ✓ Testler basariyla tamamlandi![0m
) else (
    echo [31m^> ✗ Bazi testler basarisiz oldu![0m
)

:: HTML raporu olusturduysa, kullaniciya bilgi ver
if "%FORMAT%"=="html" (
    echo [33m^> HTML test raporu 'htmlcov\index.html' konumunda olusturuldu.[0m
    echo [33m^> Raporu görüntülemek için 'start htmlcov\index.html' komutunu calistirabilirsiniz.[0m
)

:: Virtual environment'i deaktive et
deactivate
echo [36m^> ========== Test islemi tamamlandi ==========[0m
pause 