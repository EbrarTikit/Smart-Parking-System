@echo off
echo ========== Chatbot Service Test Ortami ==========

:: Ana dizine çık
cd ..

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
    pip install -r requirements.txt
) else (
    echo [32m^> Virtual environment zaten mevcut, aktive ediliyor...[0m
    call venv\Scripts\activate
)

echo [32m^> Bagimliliklar yuklendi.[0m

:: Test turune gore uygun komutlari calistir
if "%TEST_TYPE%"=="unit" (
    echo [36m^> Birim testleri calistiriliyor...[0m
    if "%FORMAT%"=="html" (
        python -m pytest -v tests\test_redis_client.py tests\test_chat_history.py --cov=app --cov-report=html
    ) else (
        python -m pytest -v tests\test_redis_client.py tests\test_chat_history.py --cov=app
    )
) else if "%TEST_TYPE%"=="integration" (
    echo [36m^> Entegrasyon testleri calistiriliyor...[0m
    if "%FORMAT%"=="html" (
        python -m pytest -v tests\test_integration.py --cov=app --cov-report=html
    ) else (
        python -m pytest -v tests\test_integration.py --cov=app
    )
) else if "%TEST_TYPE%"=="load" (
    echo [36m^> Yuk testleri calistiriliyor...[0m
    if "%FORMAT%"=="html" (
        python -m pytest -v tests\test_load.py --cov=app --cov-report=html
    ) else (
        python -m pytest -v tests\test_load.py --cov=app
    )
) else if "%TEST_TYPE%"=="all" (
    echo [36m^> Tum testler calistiriliyor (yuk testleri haric)...[0m
    if "%FORMAT%"=="html" (
        python -m pytest -v tests\test_redis_client.py tests\test_chat_history.py tests\test_integration.py --cov=app --cov-report=html
    ) else (
        python -m pytest -v tests\test_redis_client.py tests\test_chat_history.py tests\test_integration.py --cov=app
    )
) else (
    echo [31m^> Gecersiz test turu: %TEST_TYPE%[0m
    echo Kullanim: %0 [unit^|integration^|load^|all] [terminal^|html]
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