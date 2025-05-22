"""
Testler için mock sınıfları ve yardımcı fonksiyonlar.
"""
from unittest.mock import MagicMock


class MockElasticsearch:
    """Elasticsearch için mock sınıfı."""
    
    def __init__(self, *args, **kwargs):
        self.search = MagicMock()
        self.count = MagicMock()
        self.ping = MagicMock(return_value=True)
        self.cluster = MagicMock()
        self.cluster.health = MagicMock(return_value={"status": "green"})
        
        # Varsayılan mock yanıtlar
        self.search.return_value = {
            "hits": {
                "hits": [
                    {
                        "_source": {
                            "@timestamp": "2025-05-22T10:00:00.000Z",
                            "level": "INFO",
                            "service": "test-service",
                            "message": "Test log message"
                        }
                    }
                ]
            }
        }
        
        self.count.return_value = {"count": 5}
    
    def configure_search_response(self, response):
        """Search yanıtını yapılandır."""
        self.search.return_value = response
    
    def configure_count_response(self, count):
        """Count yanıtını yapılandır."""
        self.count.return_value = {"count": count}
    
    def configure_health_response(self, status):
        """Health yanıtını yapılandır."""
        self.cluster.health.return_value = {"status": status}
    
    def configure_ping_response(self, success):
        """Ping yanıtını yapılandır."""
        self.ping.return_value = success


class MockPrometheusResponse:
    """Prometheus API yanıtı için mock sınıfı."""
    
    def __init__(self, status_code=200, data=None):
        self.status_code = status_code
        self._data = data or {
            "status": "success",
            "data": {
                "result": [
                    {
                        "value": [1621500000, "42.5"]
                    }
                ]
            }
        }
    
    def json(self):
        """JSON yanıtını döndür."""
        return self._data


def mock_prometheus_get(*args, **kwargs):
    """Prometheus için mock requests.get fonksiyonu."""
    return MockPrometheusResponse()


def mock_prometheus_get_error(*args, **kwargs):
    """Prometheus için hata durumunda mock requests.get fonksiyonu."""
    return MockPrometheusResponse(status_code=500, data={"status": "error", "error": "Server error"})


def mock_prometheus_get_empty(*args, **kwargs):
    """Prometheus için boş yanıt durumunda mock requests.get fonksiyonu."""
    return MockPrometheusResponse(data={"status": "success", "data": {"result": []}})


# Test verileri
MOCK_LOG_ENTRIES = [
    {
        "timestamp": "2025-05-22T10:00:00.000Z",
        "level": "INFO",
        "service": "test-service",
        "message": "Test log message 1"
    },
    {
        "timestamp": "2025-05-22T10:05:00.000Z",
        "level": "ERROR",
        "service": "test-service",
        "message": "Test error message"
    },
    {
        "timestamp": "2025-05-22T10:10:00.000Z",
        "level": "WARN",
        "service": "test-service",
        "message": "Test warning message"
    }
]

MOCK_METRIC_VALUES = [
    {"timestamp": "2025-05-22T10:00:00.000Z", "value": 10.5},
    {"timestamp": "2025-05-22T10:05:00.000Z", "value": 15.2},
    {"timestamp": "2025-05-22T10:10:00.000Z", "value": 12.8}
] 