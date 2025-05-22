#!/bin/bash

# Test çalıştırma script'i
# Chatbot servisi testlerini çalıştırır ve test kapsamı raporu oluşturur

# Renkli çıktı için
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Chatbot Service Test Runner ===${NC}"
echo -e "${YELLOW}===============================${NC}"

# Gereksinimleri kontrol et
if ! command -v python3 &> /dev/null
then
    echo -e "${RED}Python 3 bulunamadı. Lütfen Python 3 yükleyin.${NC}"
    exit 1
fi

# Ana dizine çık
cd ..

# Sanal ortam kontrolü
if [ ! -d "venv" ]; then
    echo -e "${YELLOW}Sanal ortam bulunamadı. Oluşturuluyor...${NC}"
    python3 -m venv venv
fi

# Sanal ortamı aktifleştir
source venv/bin/activate || source venv/Scripts/activate

# Bağımlılıkları yükle
echo -e "${YELLOW}Bağımlılıklar yükleniyor...${NC}"
pip install -r requirements.txt

# Test türünü belirle
TEST_TYPE="${1:-all}"  # Varsayılan olarak 'all' testleri çalıştır
FORMAT="${2:-terminal}" # Varsayılan olarak terminal formatında rapor oluştur

# Testleri çalıştır
echo -e "${YELLOW}Testler başlatılıyor...${NC}"

# Test türüne göre uygun komutları çalıştır
case "$TEST_TYPE" in
    unit)
        echo -e "${GREEN}Birim testleri çalıştırılıyor...${NC}"
        if [ "$FORMAT" = "html" ]; then
            python -m pytest -v tests/test_redis_client.py tests/test_chat_history.py --cov=app --cov-report=html
        else
            python -m pytest -v tests/test_redis_client.py tests/test_chat_history.py --cov=app
        fi
        ;;
    integration)
        echo -e "${GREEN}Entegrasyon testleri çalıştırılıyor...${NC}"
        if [ "$FORMAT" = "html" ]; then
            python -m pytest -v tests/test_integration.py --cov=app --cov-report=html
        else
            python -m pytest -v tests/test_integration.py --cov=app
        fi
        ;;
    load)
        echo -e "${GREEN}Yük testleri çalıştırılıyor...${NC}"
        if [ "$FORMAT" = "html" ]; then
            python -m pytest -v tests/test_load.py --cov=app --cov-report=html
        else
            python -m pytest -v tests/test_load.py --cov=app
        fi
        ;;
    all)
        echo -e "${GREEN}Tüm testler çalıştırılıyor (yük testleri hariç)...${NC}"
        if [ "$FORMAT" = "html" ]; then
            python -m pytest -v tests/test_redis_client.py tests/test_chat_history.py tests/test_integration.py --cov=app --cov-report=html
        else
            python -m pytest -v tests/test_redis_client.py tests/test_chat_history.py tests/test_integration.py --cov=app
        fi
        ;;
    *)
        echo -e "${RED}Geçersiz test türü: $TEST_TYPE${NC}"
        echo -e "Kullanım: $0 [unit|integration|load|all] [terminal|html]"
        exit 1
        ;;
esac

# Test sonuçlarını göster
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Testler başarıyla tamamlandı!${NC}"
else
    echo -e "${RED}✗ Bazı testler başarısız oldu!${NC}"
fi

# HTML raporu oluşturulduysa, kullanıcıya bilgi ver
if [ "$FORMAT" = "html" ]; then
    echo -e "${YELLOW}HTML test raporu 'htmlcov/index.html' konumunda oluşturuldu.${NC}"
fi

# Virtual environment'ı deaktive et
deactivate
echo -e "${GREEN}Test çalıştırması tamamlandı.${NC}"

# Kullanım bilgisi
if [ "$1" == "" ]; then
    echo -e "\n${YELLOW}KULLANIM:${NC}"
    echo -e "  ./tests/run_tests.sh [test_türü] [rapor_formatı]"
    echo -e "\n${YELLOW}TEST TÜRLERİ:${NC}"
    echo -e "  ${GREEN}unit${NC}        - Sadece birim testleri çalıştırır"
    echo -e "  ${GREEN}integration${NC} - Sadece entegrasyon testleri çalıştırır"
    echo -e "  ${GREEN}load${NC}        - Sadece yük testleri çalıştırır"
    echo -e "  ${GREEN}all${NC}         - Tüm testleri çalıştırır"
    echo -e "\n${YELLOW}RAPOR FORMATLARI:${NC}"
    echo -e "  ${GREEN}html${NC}        - HTML formatında test kapsamı raporu oluşturur"
    echo -e "                 Örnek: ./tests/run_tests.sh unit html"
fi 