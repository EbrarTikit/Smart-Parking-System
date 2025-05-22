from fastapi import FastAPI, HTTPException, Query, Depends
from fastapi.middleware.cors import CORSMiddleware
from elasticsearch import Elasticsearch
import os
import logging
from datetime import datetime, timedelta
from typing import List, Dict, Any, Optional
import requests
import json
from pydantic import BaseModel

# Logging yapılandırması
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Çevresel değişkenler
ELASTICSEARCH_HOST = os.getenv("ELASTICSEARCH_HOST", "elasticsearch")
ELASTICSEARCH_PORT = os.getenv("ELASTICSEARCH_PORT", "9200")
PROMETHEUS_HOST = os.getenv("PROMETHEUS_HOST", "prometheus")
PROMETHEUS_PORT = os.getenv("PROMETHEUS_PORT", "9090")

# Elasticsearch bağlantısı
es_url = f"http://{ELASTICSEARCH_HOST}:{ELASTICSEARCH_PORT}"
es = Elasticsearch([es_url])

# Prometheus API URL
prometheus_url = f"http://{PROMETHEUS_HOST}:{PROMETHEUS_PORT}"

# Uygulama oluştur
app = FastAPI(
    title="Smart Parking Monitoring Service",
    description="Logging ve monitoring servisi",
    version="1.0.0"
)

# CORS ayarları
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Pydantic modelleri
class LogEntry(BaseModel):
    timestamp: str
    level: str
    service: str
    message: str
    additional_info: Optional[Dict[str, Any]] = None

class ServiceStatus(BaseModel):
    service: str
    status: str
    last_seen: Optional[str] = None
    error_count: Optional[int] = None
    request_count: Optional[int] = None

class MetricValue(BaseModel):
    timestamp: str
    value: float

class MetricSeries(BaseModel):
    metric: str
    values: List[MetricValue]

# Sağlık kontrolü
@app.get("/health")
def health_check():
    """Servisin sağlık durumunu kontrol et"""
    es_status = "healthy"
    prometheus_status = "healthy"
    
    # Elasticsearch bağlantısını kontrol et
    try:
        es_health = es.cluster.health()
        es_status = es_health["status"]
    except Exception as e:
        logger.error(f"Elasticsearch bağlantı hatası: {str(e)}")
        es_status = "unhealthy"
    
    # Prometheus bağlantısını kontrol et
    try:
        prometheus_response = requests.get(f"{prometheus_url}/-/healthy")
        if prometheus_response.status_code != 200:
            prometheus_status = "unhealthy"
    except Exception as e:
        logger.error(f"Prometheus bağlantı hatası: {str(e)}")
        prometheus_status = "unhealthy"
    
    return {
        "status": "healthy" if es_status == "green" and prometheus_status == "healthy" else "degraded",
        "elasticsearch": es_status,
        "prometheus": prometheus_status,
        "timestamp": datetime.now().isoformat()
    }

# Log sorgulama
@app.get("/logs", response_model=List[LogEntry])
def get_logs(
    service: Optional[str] = Query(None, description="Servis adı filtresi"),
    level: Optional[str] = Query(None, description="Log seviyesi filtresi (INFO, WARN, ERROR)"),
    start_time: Optional[str] = Query(None, description="Başlangıç zamanı (ISO format)"),
    end_time: Optional[str] = Query(None, description="Bitiş zamanı (ISO format)"),
    limit: int = Query(100, description="Maksimum log sayısı")
):
    """Elasticsearch'ten log kayıtlarını sorgula"""
    try:
        # Sorgu oluştur
        query = {"bool": {"must": []}}
        
        # Servis filtresi
        if service:
            query["bool"]["must"].append({"match": {"service": service}})
        
        # Log seviyesi filtresi
        if level:
            query["bool"]["must"].append({"match": {"level": level.upper()}})
        
        # Zaman aralığı filtresi
        time_filter = {}
        if start_time:
            time_filter["gte"] = start_time
        if end_time:
            time_filter["lte"] = end_time
        
        if time_filter:
            query["bool"]["must"].append({"range": {"@timestamp": time_filter}})
        
        # Elasticsearch sorgusu
        result = es.search(
            index="smart-parking-logs-*",
            body={
                "query": query,
                "sort": [{"@timestamp": {"order": "desc"}}],
                "size": limit
            }
        )
        
        # Sonuçları dönüştür
        logs = []
        for hit in result["hits"]["hits"]:
            source = hit["_source"]
            log_entry = LogEntry(
                timestamp=source.get("@timestamp", ""),
                level=source.get("level", ""),
                service=source.get("service", ""),
                message=source.get("message", ""),
                additional_info={k: v for k, v in source.items() if k not in ["@timestamp", "level", "service", "message"]}
            )
            logs.append(log_entry)
        
        return logs
    
    except Exception as e:
        logger.error(f"Log sorgulama hatası: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Log sorgulama hatası: {str(e)}")

# Hata logları
@app.get("/errors", response_model=List[LogEntry])
def get_errors(
    service: Optional[str] = Query(None, description="Servis adı filtresi"),
    start_time: Optional[str] = Query(None, description="Başlangıç zamanı (ISO format)"),
    end_time: Optional[str] = Query(None, description="Bitiş zamanı (ISO format)"),
    limit: int = Query(100, description="Maksimum log sayısı")
):
    """Elasticsearch'ten hata loglarını sorgula"""
    try:
        # Sorgu oluştur
        query = {"bool": {"must": [{"match": {"level": "ERROR"}}]}}
        
        # Servis filtresi
        if service:
            query["bool"]["must"].append({"match": {"service": service}})
        
        # Zaman aralığı filtresi
        time_filter = {}
        if start_time:
            time_filter["gte"] = start_time
        if end_time:
            time_filter["lte"] = end_time
        
        if time_filter:
            query["bool"]["must"].append({"range": {"@timestamp": time_filter}})
        
        # Elasticsearch sorgusu
        result = es.search(
            index="smart-parking-errors-*",
            body={
                "query": query,
                "sort": [{"@timestamp": {"order": "desc"}}],
                "size": limit
            }
        )
        
        # Sonuçları dönüştür
        logs = []
        for hit in result["hits"]["hits"]:
            source = hit["_source"]
            log_entry = LogEntry(
                timestamp=source.get("@timestamp", ""),
                level=source.get("level", ""),
                service=source.get("service", ""),
                message=source.get("message", ""),
                additional_info={k: v for k, v in source.items() if k not in ["@timestamp", "level", "service", "message"]}
            )
            logs.append(log_entry)
        
        return logs
    
    except Exception as e:
        logger.error(f"Hata logu sorgulama hatası: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Hata logu sorgulama hatası: {str(e)}")

# Servis durumları
@app.get("/services/status", response_model=List[ServiceStatus])
def get_service_status():
    """Tüm servislerin durumunu kontrol et"""
    services = [
        "user-service",
        "parking-management-service",
        "notification-service",
        "license-plate-service",
        "chatbot-service",
        "navigation-service"
    ]
    
    results = []
    
    for service in services:
        try:
            # Son 5 dakika içindeki logları kontrol et
            now = datetime.now()
            five_minutes_ago = now - timedelta(minutes=5)
            
            # Elasticsearch sorgusu - son logları kontrol et
            log_result = es.search(
                index="smart-parking-logs-*",
                body={
                    "query": {
                        "bool": {
                            "must": [
                                {"match": {"service": service}},
                                {"range": {"@timestamp": {"gte": five_minutes_ago.isoformat()}}}
                            ]
                        }
                    },
                    "sort": [{"@timestamp": {"order": "desc"}}],
                    "size": 1
                }
            )
            
            # Son hataları kontrol et
            error_result = es.count(
                index="smart-parking-logs-*",
                body={
                    "query": {
                        "bool": {
                            "must": [
                                {"match": {"service": service}},
                                {"match": {"level": "ERROR"}},
                                {"range": {"@timestamp": {"gte": five_minutes_ago.isoformat()}}}
                            ]
                        }
                    }
                }
            )
            
            # Prometheus'tan istek sayısını al
            try:
                prometheus_query = f'sum(rate({service}_requests_total[5m]))'
                prometheus_response = requests.get(
                    f"{prometheus_url}/api/v1/query",
                    params={"query": prometheus_query}
                )
                prometheus_data = prometheus_response.json()
                request_count = 0
                
                if prometheus_data["status"] == "success" and prometheus_data["data"]["result"]:
                    request_count = int(float(prometheus_data["data"]["result"][0]["value"][1]))
            except Exception:
                request_count = None
            
            # Servis durumunu belirle
            last_seen = None
            if log_result["hits"]["hits"]:
                last_seen = log_result["hits"]["hits"][0]["_source"].get("@timestamp")
                status = "up"
            else:
                status = "unknown"
            
            error_count = error_result["count"]
            if error_count > 0:
                status = "degraded"
            
            results.append(ServiceStatus(
                service=service,
                status=status,
                last_seen=last_seen,
                error_count=error_count,
                request_count=request_count
            ))
            
        except Exception as e:
            logger.error(f"Servis durumu sorgulama hatası ({service}): {str(e)}")
            results.append(ServiceStatus(
                service=service,
                status="unknown",
                error_count=None
            ))
    
    return results

# Metrik sorgulama
@app.get("/metrics/{metric_name}", response_model=MetricSeries)
def get_metric(
    metric_name: str,
    start_time: Optional[str] = Query(None, description="Başlangıç zamanı (ISO format)"),
    end_time: Optional[str] = Query(None, description="Bitiş zamanı (ISO format)"),
    step: str = Query("5m", description="Adım aralığı (örn: 1m, 5m, 1h)")
):
    """Prometheus'tan metrik verilerini sorgula"""
    try:
        # Varsayılan zaman aralığı: son 1 saat
        if not end_time:
            end_time = datetime.now().isoformat()
        if not start_time:
            start_time_dt = datetime.fromisoformat(end_time.replace("Z", "+00:00")) - timedelta(hours=1)
            start_time = start_time_dt.isoformat()
        
        # Prometheus range query
        prometheus_response = requests.get(
            f"{prometheus_url}/api/v1/query_range",
            params={
                "query": metric_name,
                "start": start_time,
                "end": end_time,
                "step": step
            }
        )
        
        prometheus_data = prometheus_response.json()
        
        if prometheus_data["status"] != "success":
            raise HTTPException(status_code=400, detail=f"Prometheus sorgu hatası: {prometheus_data.get('error', 'Bilinmeyen hata')}")
        
        # Sonuçları dönüştür
        values = []
        for result in prometheus_data["data"]["result"]:
            for point in result["values"]:
                timestamp = datetime.fromtimestamp(point[0]).isoformat()
                value = float(point[1])
                values.append(MetricValue(timestamp=timestamp, value=value))
        
        return MetricSeries(metric=metric_name, values=values)
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Metrik sorgulama hatası: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Metrik sorgulama hatası: {str(e)}")

# Özet dashboard verileri
@app.get("/dashboard/summary")
def get_dashboard_summary():
    """Dashboard için özet verileri getir"""
    try:
        now = datetime.now()
        one_day_ago = now - timedelta(days=1)
        
        # Toplam istek sayısı (son 24 saat)
        try:
            requests_query = 'sum(increase(license_plate_service_requests_total[24h]))'
            requests_response = requests.get(
                f"{prometheus_url}/api/v1/query",
                params={"query": requests_query}
            )
            requests_data = requests_response.json()
            total_requests = 0
            
            if requests_data["status"] == "success" and requests_data["data"]["result"]:
                total_requests = int(float(requests_data["data"]["result"][0]["value"][1]))
        except Exception as e:
            logger.error(f"İstek sayısı sorgulama hatası: {str(e)}")
            total_requests = 0
        
        # Toplam hata sayısı (son 24 saat)
        try:
            error_result = es.count(
                index="smart-parking-logs-*",
                body={
                    "query": {
                        "bool": {
                            "must": [
                                {"match": {"level": "ERROR"}},
                                {"range": {"@timestamp": {"gte": one_day_ago.isoformat()}}}
                            ]
                        }
                    }
                }
            )
            total_errors = error_result["count"]
        except Exception as e:
            logger.error(f"Hata sayısı sorgulama hatası: {str(e)}")
            total_errors = 0
        
        # Toplam araç giriş sayısı (son 24 saat)
        try:
            entries_query = 'sum(increase(parking_records_total{action="entry"}[24h]))'
            entries_response = requests.get(
                f"{prometheus_url}/api/v1/query",
                params={"query": entries_query}
            )
            entries_data = entries_response.json()
            total_entries = 0
            
            if entries_data["status"] == "success" and entries_data["data"]["result"]:
                total_entries = int(float(entries_data["data"]["result"][0]["value"][1]))
        except Exception as e:
            logger.error(f"Araç girişi sayısı sorgulama hatası: {str(e)}")
            total_entries = 0
        
        # Toplam araç çıkış sayısı (son 24 saat)
        try:
            exits_query = 'sum(increase(parking_records_total{action="exit"}[24h]))'
            exits_response = requests.get(
                f"{prometheus_url}/api/v1/query",
                params={"query": exits_query}
            )
            exits_data = exits_response.json()
            total_exits = 0
            
            if exits_data["status"] == "success" and exits_data["data"]["result"]:
                total_exits = int(float(exits_data["data"]["result"][0]["value"][1]))
        except Exception as e:
            logger.error(f"Araç çıkışı sayısı sorgulama hatası: {str(e)}")
            total_exits = 0
        
        # Ortalama yanıt süresi (son 24 saat)
        try:
            latency_query = 'avg(rate(license_plate_service_request_latency_seconds_sum[24h]) / rate(license_plate_service_request_latency_seconds_count[24h]))'
            latency_response = requests.get(
                f"{prometheus_url}/api/v1/query",
                params={"query": latency_query}
            )
            latency_data = latency_response.json()
            avg_latency = 0
            
            if latency_data["status"] == "success" and latency_data["data"]["result"]:
                avg_latency = float(latency_data["data"]["result"][0]["value"][1])
        except Exception as e:
            logger.error(f"Yanıt süresi sorgulama hatası: {str(e)}")
            avg_latency = 0
        
        return {
            "total_requests": total_requests,
            "total_errors": total_errors,
            "total_vehicle_entries": total_entries,
            "total_vehicle_exits": total_exits,
            "average_response_time": round(avg_latency, 3),
            "timestamp": now.isoformat()
        }
    
    except Exception as e:
        logger.error(f"Dashboard özeti sorgulama hatası: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Dashboard özeti sorgulama hatası: {str(e)}")

# Uygulama başlatıldığında
@app.on_event("startup")
def startup_event():
    logger.info("Monitoring servisi başlatılıyor...")
    
    # Elasticsearch bağlantısını kontrol et
    try:
        es_health = es.cluster.health()
        logger.info(f"Elasticsearch bağlantısı başarılı: {es_health['status']}")
    except Exception as e:
        logger.warning(f"Elasticsearch bağlantısı kurulamadı: {str(e)}")
    
    # Prometheus bağlantısını kontrol et
    try:
        prometheus_response = requests.get(f"{prometheus_url}/-/healthy")
        if prometheus_response.status_code == 200:
            logger.info("Prometheus bağlantısı başarılı")
        else:
            logger.warning(f"Prometheus bağlantısı başarısız: {prometheus_response.status_code}")
    except Exception as e:
        logger.warning(f"Prometheus bağlantısı kurulamadı: {str(e)}")
    
    logger.info("Monitoring servisi başlatıldı")

# Uygulamayı doğrudan çalıştırma
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8010) 