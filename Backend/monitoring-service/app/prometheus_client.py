import logging
import requests
from typing import Dict, List, Optional, Any, Union
from datetime import datetime, timedelta
import time

from app.config import (
    PROMETHEUS_HOST,
    PROMETHEUS_PORT,
    DEFAULT_METRICS,
    DEFAULT_TIME_WINDOW,
    DEFAULT_STEP
)

logger = logging.getLogger(__name__)

# Prometheus API URL
PROMETHEUS_API_URL = f"http://{PROMETHEUS_HOST}:{PROMETHEUS_PORT}/api/v1"

def check_prometheus_connection() -> bool:
    """Prometheus bağlantısını kontrol et"""
    try:
        response = requests.get(f"{PROMETHEUS_API_URL}/status/config")
        return response.status_code == 200
    except requests.RequestException as e:
        logger.error(f"Prometheus bağlantı hatası: {str(e)}")
        return False

def parse_time_window(time_window: str) -> int:
    """
    Zaman penceresi ifadesini saniyeye çevir (örn: 24h, 30m, 1d)
    
    Args:
        time_window: Zaman penceresi ifadesi (örn: 24h, 30m, 1d)
        
    Returns:
        Saniye cinsinden zaman
    """
    if not time_window:
        time_window = DEFAULT_TIME_WINDOW
        
    unit = time_window[-1]
    try:
        value = int(time_window[:-1])
    except ValueError:
        logger.warning(f"Geçersiz zaman penceresi formatı: {time_window}, varsayılan kullanılıyor")
        return parse_time_window(DEFAULT_TIME_WINDOW)
    
    if unit == 's':
        return value
    elif unit == 'm':
        return value * 60
    elif unit == 'h':
        return value * 3600
    elif unit == 'd':
        return value * 86400
    else:
        logger.warning(f"Geçersiz zaman birimi: {unit}, varsayılan kullanılıyor")
        return parse_time_window(DEFAULT_TIME_WINDOW)

def get_metric_range(
    metric_name: str,
    start_time: Optional[str] = None,
    end_time: Optional[str] = None,
    step: str = DEFAULT_STEP
) -> Dict[str, Any]:
    """
    Belirli bir metriğin zaman aralığındaki değerlerini getir
    
    Args:
        metric_name: Metrik adı
        start_time: Başlangıç zamanı (ISO format)
        end_time: Bitiş zamanı (ISO format)
        step: Adım aralığı (örn: 1m, 5m, 1h)
        
    Returns:
        Metrik değerleri
    """
    try:
        # Zaman parametrelerini ayarla
        now = datetime.now()
        
        if end_time:
            end_timestamp = datetime.fromisoformat(end_time).timestamp()
        else:
            end_timestamp = now.timestamp()
            
        if start_time:
            start_timestamp = datetime.fromisoformat(start_time).timestamp()
        else:
            # Varsayılan olarak son 24 saat
            time_window = parse_time_window(DEFAULT_TIME_WINDOW)
            start_timestamp = end_timestamp - time_window
        
        # Prometheus API'sine istek gönder
        response = requests.get(
            f"{PROMETHEUS_API_URL}/query_range",
            params={
                "query": metric_name,
                "start": start_timestamp,
                "end": end_timestamp,
                "step": step
            }
        )
        
        if response.status_code != 200:
            logger.error(f"Prometheus API hatası: {response.status_code}, {response.text}")
            return {"metric": metric_name, "values": []}
            
        data = response.json()
        
        if data["status"] != "success":
            logger.error(f"Prometheus API başarısız: {data.get('error', 'Bilinmeyen hata')}")
            return {"metric": metric_name, "values": []}
            
        result = data["data"]["result"]
        
        if not result:
            logger.warning(f"Metrik için veri bulunamadı: {metric_name}")
            return {"metric": metric_name, "values": []}
            
        # Sonuçları işle
        values = []
        for point in result[0]["values"]:
            timestamp = datetime.fromtimestamp(point[0]).isoformat()
            value = float(point[1])
            values.append({"timestamp": timestamp, "value": value})
            
        return {
            "metric": metric_name,
            "values": values
        }
        
    except requests.RequestException as e:
        logger.error(f"Prometheus API isteği hatası: {str(e)}")
        return {"metric": metric_name, "values": []}
    except Exception as e:
        logger.error(f"Metrik verisi alınırken hata: {str(e)}")
        return {"metric": metric_name, "values": []}

def get_metric_instant(metric_name: str) -> float:
    """
    Belirli bir metriğin anlık değerini getir
    
    Args:
        metric_name: Metrik adı
        
    Returns:
        Metrik değeri
    """
    try:
        # Prometheus API'sine istek gönder
        response = requests.get(
            f"{PROMETHEUS_API_URL}/query",
            params={"query": metric_name}
        )
        
        if response.status_code != 200:
            logger.error(f"Prometheus API hatası: {response.status_code}, {response.text}")
            return 0.0
            
        data = response.json()
        
        if data["status"] != "success":
            logger.error(f"Prometheus API başarısız: {data.get('error', 'Bilinmeyen hata')}")
            return 0.0
            
        result = data["data"]["result"]
        
        if not result:
            logger.warning(f"Metrik için veri bulunamadı: {metric_name}")
            return 0.0
            
        # Sonucu işle
        return float(result[0]["value"][1])
        
    except requests.RequestException as e:
        logger.error(f"Prometheus API isteği hatası: {str(e)}")
        return 0.0
    except Exception as e:
        logger.error(f"Metrik verisi alınırken hata: {str(e)}")
        return 0.0

def get_service_request_count(service: str) -> int:
    """
    Belirli bir servis için toplam istek sayısını getir
    
    Args:
        service: Servis adı
        
    Returns:
        İstek sayısı
    """
    metric_name = f'http_requests_total{{service="{service}"}}'
    return int(get_metric_instant(metric_name))

def get_service_avg_response_time(service: str) -> float:
    """
    Belirli bir servis için ortalama yanıt süresini getir
    
    Args:
        service: Servis adı
        
    Returns:
        Ortalama yanıt süresi (ms)
    """
    metric_name = f'http_request_duration_seconds_sum{{service="{service}"}} / http_request_duration_seconds_count{{service="{service}"}}'
    return get_metric_instant(metric_name) * 1000  # saniyeden milisaniyeye çevir

def get_vehicle_entry_count() -> int:
    """Toplam araç girişi sayısını getir"""
    return int(get_metric_instant("vehicle_entries_total"))

def get_vehicle_exit_count() -> int:
    """Toplam araç çıkışı sayısını getir"""
    return int(get_metric_instant("vehicle_exits_total")) 