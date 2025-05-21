import logging
from datetime import datetime
from typing import Dict, List, Optional, Any, Union
from elasticsearch import Elasticsearch
from elasticsearch.exceptions import ElasticsearchException

from app.config import (
    ELASTICSEARCH_HOST,
    ELASTICSEARCH_PORT,
    ELASTICSEARCH_USER,
    ELASTICSEARCH_PASSWORD,
    ELASTICSEARCH_INDEX_PREFIX
)

logger = logging.getLogger(__name__)

# Elasticsearch bağlantısı
def get_elasticsearch_client() -> Elasticsearch:
    """Elasticsearch bağlantısı oluştur"""
    es_hosts = [f"http://{ELASTICSEARCH_HOST}:{ELASTICSEARCH_PORT}"]
    
    # Kimlik bilgileri varsa ekle
    if ELASTICSEARCH_USER and ELASTICSEARCH_PASSWORD:
        client = Elasticsearch(
            es_hosts,
            http_auth=(ELASTICSEARCH_USER, ELASTICSEARCH_PASSWORD)
        )
    else:
        client = Elasticsearch(es_hosts)
    
    return client

def check_elasticsearch_connection() -> bool:
    """Elasticsearch bağlantısını kontrol et"""
    try:
        client = get_elasticsearch_client()
        return client.ping()
    except ElasticsearchException as e:
        logger.error(f"Elasticsearch bağlantı hatası: {str(e)}")
        return False

def get_logs(
    service: Optional[str] = None,
    level: Optional[str] = None,
    start_time: Optional[str] = None,
    end_time: Optional[str] = None,
    limit: int = 100
) -> List[Dict[str, Any]]:
    """
    Belirtilen kriterlere göre logları getir
    
    Args:
        service: Servis adı filtresi
        level: Log seviyesi filtresi (INFO, WARN, ERROR)
        start_time: Başlangıç zamanı (ISO format)
        end_time: Bitiş zamanı (ISO format)
        limit: Maksimum log sayısı
        
    Returns:
        Log listesi
    """
    try:
        client = get_elasticsearch_client()
        index_pattern = f"{ELASTICSEARCH_INDEX_PREFIX}*"
        
        # Sorgu oluştur
        query = {"bool": {"must": []}}
        
        # Servis filtresi
        if service:
            query["bool"]["must"].append({"match": {"service": service}})
        
        # Log seviyesi filtresi
        if level:
            query["bool"]["must"].append({"match": {"level": level.upper()}})
        
        # Zaman filtresi
        if start_time or end_time:
            time_filter = {"range": {"@timestamp": {}}}
            
            if start_time:
                time_filter["range"]["@timestamp"]["gte"] = start_time
                
            if end_time:
                time_filter["range"]["@timestamp"]["lte"] = end_time
                
            query["bool"]["must"].append(time_filter)
        
        # Sorguyu çalıştır
        response = client.search(
            index=index_pattern,
            body={
                "query": query,
                "sort": [{"@timestamp": {"order": "desc"}}],
                "size": limit
            }
        )
        
        # Sonuçları işle
        logs = []
        for hit in response["hits"]["hits"]:
            log_entry = hit["_source"]
            # Timestamp formatını düzelt
            if "@timestamp" in log_entry:
                log_entry["timestamp"] = log_entry.pop("@timestamp")
            logs.append(log_entry)
            
        return logs
        
    except ElasticsearchException as e:
        logger.error(f"Log sorgulama hatası: {str(e)}")
        return []

def get_error_logs(
    service: Optional[str] = None,
    start_time: Optional[str] = None,
    end_time: Optional[str] = None,
    limit: int = 100
) -> List[Dict[str, Any]]:
    """
    Hata loglarını getir
    
    Args:
        service: Servis adı filtresi
        start_time: Başlangıç zamanı (ISO format)
        end_time: Bitiş zamanı (ISO format)
        limit: Maksimum log sayısı
        
    Returns:
        Hata logları listesi
    """
    return get_logs(service, "ERROR", start_time, end_time, limit)

def get_service_error_count(
    service: str,
    start_time: Optional[str] = None,
    end_time: Optional[str] = None
) -> int:
    """
    Belirli bir servis için hata sayısını getir
    
    Args:
        service: Servis adı
        start_time: Başlangıç zamanı (ISO format)
        end_time: Bitiş zamanı (ISO format)
        
    Returns:
        Hata sayısı
    """
    try:
        client = get_elasticsearch_client()
        index_pattern = f"{ELASTICSEARCH_INDEX_PREFIX}*"
        
        # Sorgu oluştur
        query = {
            "bool": {
                "must": [
                    {"match": {"service": service}},
                    {"match": {"level": "ERROR"}}
                ]
            }
        }
        
        # Zaman filtresi
        if start_time or end_time:
            time_filter = {"range": {"@timestamp": {}}}
            
            if start_time:
                time_filter["range"]["@timestamp"]["gte"] = start_time
                
            if end_time:
                time_filter["range"]["@timestamp"]["lte"] = end_time
                
            query["bool"]["must"].append(time_filter)
        
        # Sorguyu çalıştır
        response = client.count(
            index=index_pattern,
            body={"query": query}
        )
        
        return response["count"]
        
    except ElasticsearchException as e:
        logger.error(f"Hata sayısı sorgulama hatası: {str(e)}")
        return 0

def get_service_last_log(service: str) -> Optional[Dict[str, Any]]:
    """
    Belirli bir servis için en son logu getir
    
    Args:
        service: Servis adı
        
    Returns:
        En son log veya None
    """
    logs = get_logs(service=service, limit=1)
    return logs[0] if logs else None 