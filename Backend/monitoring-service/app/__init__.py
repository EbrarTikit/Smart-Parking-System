import logging
from app.config import LOG_LEVEL, LOG_FORMAT

# Loglama yapılandırması
logging.basicConfig(
    level=getattr(logging, LOG_LEVEL),
    format=LOG_FORMAT
)

logger = logging.getLogger(__name__)
logger.info("Smart Parking Monitoring Service başlatılıyor...") 