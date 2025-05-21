import logging
from typing import Dict, List, Optional, Any
from datetime import datetime, timedelta

from app.config import MONITORED_SERVICES
from app.elasticsearch_client import get_service_error_count, get_service_last_log
from app.prometheus_client import get_service_request_count, get_service_avg_response_time

logger = logging.getLogger(__name__)

def get_service_status(service: str) -> Dict[str, Any]:
    """
    Belirli bir servisin durumunu getir
    
    Args:
        service: Servis adı
        
    Returns:
        Servis durumu
    """
    try:
        # Son log kaydını al
        last_log = get_service_last_log(service)
        
        # Son 24 saatteki hata sayısını al
        now = datetime.now()
        yesterday = now - timedelta(hours=24)
        error_count = get_service_error_count(
            service, 
            start_time=yesterday.isoformat(),
            end_time=now.isoformat()
        )
        
        # İstek sayısını al
        request_count = get_service_request_count(service)
        
        # Ortalama yanıt süresini al
        avg_response_time = get_service_avg_response_time(service)
        
        # Servis durumunu belirle
        status = "healthy"  # Varsayılan durum
        
        # Son log kaydı 5 dakikadan eskiyse veya yoksa, servis durumu "inactive"
        if not last_log:
            status = "inactive"
        else:
            last_log_time = datetime.fromisoformat(last_log.get("timestamp", now.isoformat()))
            if (now - last_log_time) > timedelta(minutes=5):
                status = "inactive"
        
        # Hata sayısı yüksekse, servis durumu "warning" veya "error"
        if error_count > 0:
            if error_count > 10:
                status = "error"
            else:
                status = "warning"
                
        return {
            "service": service,
            "status": status,
            "last_seen": last_log.get("timestamp") if last_log else None,
            "error_count": error_count,
            "request_count": request_count,
            "avg_response_time": round(avg_response_time, 2) if avg_response_time else None
        }
        
    except Exception as e:
        logger.error(f"Servis durumu alınırken hata: {str(e)}")
        return {
            "service": service,
            "status": "unknown",
            "error": str(e)
        }

def get_all_services_status() -> List[Dict[str, Any]]:
    """
    Tüm servislerin durumunu getir
    
    Returns:
        Servis durumları listesi
    """
    result = []
    for service in MONITORED_SERVICES:
        result.append(get_service_status(service))
    return result

def get_system_health() -> Dict[str, Any]:
    """
    Sistemin genel sağlık durumunu getir
    
    Returns:
        Sistem sağlık durumu
    """
    services_status = get_all_services_status()
    
    # Servislerin durumlarını say
    total = len(services_status)
    healthy = sum(1 for s in services_status if s["status"] == "healthy")
    warning = sum(1 for s in services_status if s["status"] == "warning")
    error = sum(1 for s in services_status if s["status"] == "error")
    inactive = sum(1 for s in services_status if s["status"] == "inactive")
    unknown = sum(1 for s in services_status if s["status"] == "unknown")
    
    # Genel durum
    if error > 0:
        overall_status = "error"
    elif warning > 0 or inactive > 0:
        overall_status = "warning"
    elif unknown > 0:
        overall_status = "unknown"
    else:
        overall_status = "healthy"
    
    return {
        "overall_status": overall_status,
        "services": {
            "total": total,
            "healthy": healthy,
            "warning": warning,
            "error": error,
            "inactive": inactive,
            "unknown": unknown
        },
        "timestamp": datetime.now().isoformat()
    } 