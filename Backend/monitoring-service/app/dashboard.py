import logging
from typing import Dict, List, Optional, Any
from datetime import datetime, timedelta

from app.config import MONITORED_SERVICES
from app.service_status import get_all_services_status, get_system_health
from app.elasticsearch_client import get_logs, get_error_logs
from app.prometheus_client import (
    get_metric_range, 
    get_vehicle_entry_count, 
    get_vehicle_exit_count,
    get_service_request_count,
    get_service_avg_response_time
)

logger = logging.getLogger(__name__)

def get_dashboard_summary() -> Dict[str, Any]:
    """
    Dashboard için özet verileri getir
    
    Returns:
        Dashboard özet verileri
    """
    try:
        # Sistem sağlık durumu
        health = get_system_health()
        
        # Son 24 saatteki toplam istek sayısı
        total_requests = sum(get_service_request_count(service) for service in MONITORED_SERVICES)
        
        # Son 24 saatteki hata sayısı
        now = datetime.now()
        yesterday = now - timedelta(hours=24)
        error_logs = get_error_logs(
            start_time=yesterday.isoformat(),
            end_time=now.isoformat()
        )
        total_errors = len(error_logs)
        
        # Araç giriş ve çıkış sayıları
        vehicle_entries = get_vehicle_entry_count()
        vehicle_exits = get_vehicle_exit_count()
        
        # Servis performans metrikleri
        service_metrics = []
        for service in MONITORED_SERVICES:
            try:
                avg_response_time = get_service_avg_response_time(service)
                request_count = get_service_request_count(service)
                
                service_metrics.append({
                    "service": service,
                    "avg_response_time": round(avg_response_time, 2) if avg_response_time else 0,
                    "request_count": request_count
                })
            except Exception as e:
                logger.error(f"{service} servis metrikleri alınırken hata: {str(e)}")
                service_metrics.append({
                    "service": service,
                    "avg_response_time": 0,
                    "request_count": 0,
                    "error": str(e)
                })
        
        # Son hatalar
        recent_errors = error_logs[:5]  # En son 5 hata
        
        return {
            "health": health,
            "total_requests": total_requests,
            "total_errors": total_errors,
            "vehicle_entries": vehicle_entries,
            "vehicle_exits": vehicle_exits,
            "service_metrics": service_metrics,
            "recent_errors": recent_errors,
            "timestamp": now.isoformat()
        }
        
    except Exception as e:
        logger.error(f"Dashboard özeti alınırken hata: {str(e)}")
        return {
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }

def get_traffic_metrics(
    start_time: Optional[str] = None,
    end_time: Optional[str] = None,
    step: str = "1h"
) -> Dict[str, Any]:
    """
    Trafik metriklerini getir
    
    Args:
        start_time: Başlangıç zamanı (ISO format)
        end_time: Bitiş zamanı (ISO format)
        step: Adım aralığı (örn: 1m, 5m, 1h)
        
    Returns:
        Trafik metrikleri
    """
    try:
        # Araç giriş ve çıkış metrikleri
        entries = get_metric_range("vehicle_entries_total", start_time, end_time, step)
        exits = get_metric_range("vehicle_exits_total", start_time, end_time, step)
        
        return {
            "entries": entries,
            "exits": exits,
            "timestamp": datetime.now().isoformat()
        }
        
    except Exception as e:
        logger.error(f"Trafik metrikleri alınırken hata: {str(e)}")
        return {
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }

def get_performance_metrics(
    service: Optional[str] = None,
    start_time: Optional[str] = None,
    end_time: Optional[str] = None,
    step: str = "5m"
) -> Dict[str, Any]:
    """
    Performans metriklerini getir
    
    Args:
        service: Servis adı (None ise tüm servisler)
        start_time: Başlangıç zamanı (ISO format)
        end_time: Bitiş zamanı (ISO format)
        step: Adım aralığı (örn: 1m, 5m, 1h)
        
    Returns:
        Performans metrikleri
    """
    try:
        result = {}
        
        # Belirli bir servis için
        if service:
            services = [service]
        else:
            services = MONITORED_SERVICES
        
        for svc in services:
            # İstek sayısı
            request_count = get_metric_range(
                f'http_requests_total{{service="{svc}"}}',
                start_time, end_time, step
            )
            
            # Ortalama yanıt süresi
            response_time = get_metric_range(
                f'http_request_duration_seconds_sum{{service="{svc}"}} / http_request_duration_seconds_count{{service="{svc}"}}',
                start_time, end_time, step
            )
            
            result[svc] = {
                "request_count": request_count,
                "response_time": response_time
            }
        
        return {
            "services": result,
            "timestamp": datetime.now().isoformat()
        }
        
    except Exception as e:
        logger.error(f"Performans metrikleri alınırken hata: {str(e)}")
        return {
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }

def get_error_metrics(
    service: Optional[str] = None,
    start_time: Optional[str] = None,
    end_time: Optional[str] = None,
    step: str = "1h"
) -> Dict[str, Any]:
    """
    Hata metriklerini getir
    
    Args:
        service: Servis adı (None ise tüm servisler)
        start_time: Başlangıç zamanı (ISO format)
        end_time: Bitiş zamanı (ISO format)
        step: Adım aralığı (örn: 1m, 5m, 1h)
        
    Returns:
        Hata metrikleri
    """
    try:
        # Hata loglarını getir
        errors = get_error_logs(service, start_time, end_time)
        
        # Hataları zaman damgasına göre grupla
        error_counts = {}
        for error in errors:
            timestamp = error.get("timestamp", "")
            if timestamp:
                # Timestamp'i adım aralığına göre yuvarla
                dt = datetime.fromisoformat(timestamp)
                if step.endswith("m"):
                    minutes = int(step[:-1])
                    rounded = dt.replace(minute=dt.minute - dt.minute % minutes, second=0, microsecond=0)
                elif step.endswith("h"):
                    hours = int(step[:-1])
                    rounded = dt.replace(hour=dt.hour - dt.hour % hours, minute=0, second=0, microsecond=0)
                else:
                    rounded = dt
                
                rounded_str = rounded.isoformat()
                if rounded_str not in error_counts:
                    error_counts[rounded_str] = 0
                error_counts[rounded_str] += 1
        
        # Sonuçları formatlı olarak döndür
        values = []
        for timestamp, count in sorted(error_counts.items()):
            values.append({"timestamp": timestamp, "value": count})
        
        return {
            "metric": "error_count",
            "values": values,
            "timestamp": datetime.now().isoformat()
        }
        
    except Exception as e:
        logger.error(f"Hata metrikleri alınırken hata: {str(e)}")
        return {
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        } 