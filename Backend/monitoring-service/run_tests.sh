#!/bin/bash

# ANSI renk kodları
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${BLUE}========== Monitoring Service Test Ortami ==========${NC}"

# Komut satiri parametrelerini kontrol et
TEST_TYPE=${1:-all}
FORMAT=${2:-terminal}

echo -e "${BLUE}> Test ortami hazirlaniyor...${NC}"

# Virtual environment kontrol et ve olustur
if [ ! -d "venv" ]; then
    echo -e "${YELLOW}> Virtual environment bulunamadi, olusturuluyor...${NC}"
    python3 -m venv venv
    source venv/bin/activate
    pip install --upgrade pip
    pip install -r requirements-minimal.txt
else
    echo -e "${GREEN}> Virtual environment zaten mevcut, aktive ediliyor...${NC}"
    source venv/bin/activate
fi

echo -e "${GREEN}> Bagimliliklar yuklendi.${NC}"

# Test turune gore uygun komutlari calistir
case $TEST_TYPE in
    elasticsearch)
        echo -e "${BLUE}> Elasticsearch client testleri calistiriliyor...${NC}"
        if [ "$FORMAT" == "html" ]; then
            python -m pytest -v tests/test_elasticsearch_client.py --cov=app --cov-report=html
        else
            python -m pytest -v tests/test_elasticsearch_client.py --cov=app
        fi
        ;;
    prometheus)
        echo -e "${BLUE}> Prometheus client testleri calistiriliyor...${NC}"
        if [ "$FORMAT" == "html" ]; then
            python -m pytest -v tests/test_prometheus_client.py --cov=app --cov-report=html
        else
            python -m pytest -v tests/test_prometheus_client.py --cov=app
        fi
        ;;
    service)
        echo -e "${BLUE}> Service status testleri calistiriliyor...${NC}"
        if [ "$FORMAT" == "html" ]; then
            python -m pytest -v tests/test_service_status.py --cov=app --cov-report=html
        else
            python -m pytest -v tests/test_service_status.py --cov=app
        fi
        ;;
    dashboard)
        echo -e "${BLUE}> Dashboard testleri calistiriliyor...${NC}"
        if [ "$FORMAT" == "html" ]; then
            python -m pytest -v tests/test_dashboard.py --cov=app --cov-report=html
        else
            python -m pytest -v tests/test_dashboard.py --cov=app
        fi
        ;;
    api)
        echo -e "${BLUE}> API testleri calistiriliyor...${NC}"
        if [ "$FORMAT" == "html" ]; then
            python -m pytest -v tests/test_main.py --cov=app --cov-report=html
        else
            python -m pytest -v tests/test_main.py --cov=app
        fi
        ;;
    all)
        echo -e "${BLUE}> Tum testler calistiriliyor...${NC}"
        if [ "$FORMAT" == "html" ]; then
            python -m pytest -v tests/ --cov=app --cov-report=html
        else
            python -m pytest -v tests/ --cov=app
        fi
        ;;
    *)
        echo -e "${RED}> Gecersiz test turu: $TEST_TYPE${NC}"
        echo "Kullanim: $0 [elasticsearch|prometheus|service|dashboard|api|all] [terminal|html]"
        exit 1
        ;;
esac

# Test sonuclarini goster
if [ $? -eq 0 ]; then
    echo -e "${GREEN}> ✓ Testler basariyla tamamlandi!${NC}"
else
    echo -e "${RED}> ✗ Bazi testler basarisiz oldu!${NC}"
fi

# HTML raporu olusturduysa, kullaniciya bilgi ver
if [ "$FORMAT" == "html" ]; then
    echo -e "${YELLOW}> HTML test raporu 'htmlcov/index.html' konumunda olusturuldu.${NC}"
    echo -e "${YELLOW}> Raporu görüntülemek için 'open htmlcov/index.html' komutunu calistirabilirsiniz.${NC}"
fi

# Virtual environment'i deaktive et
deactivate
echo -e "${BLUE}> ========== Test islemi tamamlandi ==========${NC}" 