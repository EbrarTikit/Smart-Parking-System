"""
WebSocket bağlantıları ve ileti gönderimini yöneten modül
"""

import asyncio
import json
import logging
from typing import Dict, List, Any, Optional, Set
from datetime import datetime

logger = logging.getLogger(__name__)

# Aktif WebSocket bağlantılarını saklayan sözlük
# client_id -> WebSocket bağlantısı
active_connections: Dict[str, Any] = {}

# Oda türleri
class RoomType:
    ADMIN = "admin"  # Admin paneli bağlantıları
    PARKING = "parking"  # Belirli bir otopark için bağlantılar
    VEHICLE = "vehicle"  # Belirli bir araç için bağlantılar
    ALL = "all"  # Tüm bağlantılar

# Odaları yönetecek sınıf
class ConnectionManager:
    def __init__(self):
        # Her oda tipi için aktif bağlantıları tutan sözlük
        # room_id -> set(client_id)
        self.room_connections: Dict[str, Set[str]] = {
            RoomType.ALL: set()
        }
        self.client_info: Dict[str, Dict[str, Any]] = {}
        self.last_cleanup = datetime.now()
        self.cleanup_interval = 300  # 5 dakikada bir temizlik yap (saniye)
        logger.info("WebSocket bağlantı yöneticisi başlatıldı")
        
    def get_room_key(self, room_type: str, room_id: Optional[str] = None) -> str:
        """Oda anahtarını oluşturur: room_type:room_id"""
        if room_id:
            return f"{room_type}:{room_id}"
        return room_type
        
    def add_connection(self, client_id: str, websocket: Any, room_type: str, room_id: Optional[str] = None) -> None:
        """Yeni bir WebSocket bağlantısını kaydeder"""
        # WebSocket nesnesini sakla
        active_connections[client_id] = websocket
        
        # Odaya ekle
        room_key = self.get_room_key(room_type, room_id)
        if room_key not in self.room_connections:
            self.room_connections[room_key] = set()
        self.room_connections[room_key].add(client_id)
        
        # Genel "all" odasına da ekle
        self.room_connections[RoomType.ALL].add(client_id)
        
        # Bağlantı bilgilerini kaydet
        self.client_info[client_id] = {
            "room_type": room_type, 
            "room_id": room_id,
            "connected_at": datetime.now().isoformat(),
            "client_id": client_id
        }
        
        logger.info(f"Yeni WebSocket bağlantısı: client_id={client_id}, room={room_key}")
        
        # Periyodik temizlik kontrolü
        self.check_cleanup()
        
    def remove_connection(self, client_id: str) -> None:
        """Bir WebSocket bağlantısını kaldırır"""
        # WebSocket nesnesini kaldır
        if client_id in active_connections:
            del active_connections[client_id]
            
            # Tüm odalardan kaldır
            for room_key in self.room_connections:
                if client_id in self.room_connections[room_key]:
                    self.room_connections[room_key].remove(client_id)
            
            # İstemci bilgilerini kaldır
            if client_id in self.client_info:
                del self.client_info[client_id]
                
            logger.info(f"WebSocket bağlantısı kapatıldı: client_id={client_id}")
        
    def get_connections(self, room_type: str, room_id: Optional[str] = None) -> List[str]:
        """Belirli bir odadaki tüm bağlantıları döndürür"""
        room_key = self.get_room_key(room_type, room_id)
        if room_key in self.room_connections:
            return list(self.room_connections[room_key])
        return []
    
    def count_connections(self, room_type: str = RoomType.ALL, room_id: Optional[str] = None) -> int:
        """Belirli bir odadaki bağlantı sayısını döndürür"""
        room_key = self.get_room_key(room_type, room_id)
        if room_key in self.room_connections:
            return len(self.room_connections[room_key])
        return 0
    
    async def broadcast(self, 
                       message: Dict[str, Any], 
                       room_type: str = RoomType.ALL, 
                       room_id: Optional[str] = None,
                       exclude: Optional[List[str]] = None,
                       fallback_to_admin: bool = True) -> bool:
        """
        Belirli bir odadaki tüm bağlantılara mesaj gönderir
        
        Args:
            message: Gönderilecek mesaj
            room_type: Oda türü
            room_id: Oda ID'si
            exclude: Mesajın gönderilmeyeceği client_id'ler
            fallback_to_admin: Oda boşsa admin odasına gönder
            
        Returns:
            bool: Mesajın en az bir bağlantıya gönderilip gönderilmediği
        """
        if exclude is None:
            exclude = []
            
        room_key = self.get_room_key(room_type, room_id)
        clients = self.get_connections(room_type, room_id)
        
        # Oda boşsa ve fallback_to_admin aktifse, admin odasına gönder
        if not clients and fallback_to_admin and room_type != RoomType.ADMIN:
            logger.debug(f"Oda boş, mesaj admin odasına yönlendiriliyor: room={room_key}")
            admin_clients = self.get_connections(RoomType.ADMIN)
            
            if admin_clients:
                # Admin odasına yönlendirilen mesajı işaretle
                if "forwarded_from" not in message:
                    message["forwarded_from"] = room_key
                
                # Admin odasına gönder
                return await self.broadcast(message, RoomType.ADMIN, exclude=exclude, fallback_to_admin=False)
            else:
                logger.debug("Admin odası da boş, mesaj gönderilemedi")
                return False
        
        if not clients:
            logger.debug(f"Oda boş, mesaj gönderilemedi: room={room_key}")
            return False
            
        logger.debug(f"Mesaj gönderiliyor: room={room_key}, client_count={len(clients)}")
        
        # Timestamp ekle
        if "timestamp" not in message:
            message["timestamp"] = datetime.now().isoformat()
        
        # JSON formatına dönüştür
        message_json = json.dumps(message)
        
        # Asenkron görevleri topla
        tasks = []
        sent_count = 0
        for client_id in clients:
            if client_id in exclude:
                continue
                
            if client_id in active_connections:
                websocket = active_connections[client_id]
                tasks.append(websocket.send_text(message_json))
                sent_count += 1
        
        # Tüm görevleri çalıştır
        if tasks:
            try:
                await asyncio.gather(*tasks, return_exceptions=True)
                return sent_count > 0
            except Exception as e:
                logger.error(f"Mesaj gönderilirken hata: {str(e)}")
                return False
        
        return False
    
    def get_connection_status(self) -> Dict[str, Any]:
        """Bağlantı durumu bilgisini döndürür"""
        room_stats = {}
        for room_key in self.room_connections:
            room_stats[room_key] = len(self.room_connections[room_key])
            
        return {
            "total_connections": len(active_connections),
            "room_stats": room_stats,
            "clients": list(self.client_info.values()),
            "last_cleanup": self.last_cleanup.isoformat()
        }
    
    def check_cleanup(self) -> None:
        """Periyodik temizlik kontrolü yapar"""
        now = datetime.now()
        seconds_since_cleanup = (now - self.last_cleanup).total_seconds()
        
        if seconds_since_cleanup >= self.cleanup_interval:
            self.cleanup_connections()
            self.last_cleanup = now
            
    def cleanup_connections(self) -> None:
        """Geçersiz bağlantıları temizler"""
        logger.info("WebSocket bağlantıları temizleniyor...")
        
        # Aktif olmayan bağlantıları tespit et
        inactive_connections = []
        for client_id, websocket in active_connections.items():
            try:
                # WebSocket bağlantısının durumunu kontrol et
                if hasattr(websocket, "client_state") and websocket.client_state.name == "DISCONNECTED":
                    inactive_connections.append(client_id)
                    logger.debug(f"Bağlantı kapalı tespit edildi: {client_id}")
                elif hasattr(websocket, "closed") and websocket.closed:
                    inactive_connections.append(client_id)
                    logger.debug(f"Kapalı bağlantı tespit edildi: {client_id}")
            except Exception as e:
                logger.warning(f"Bağlantı durumu kontrol edilirken hata: {str(e)}")
                inactive_connections.append(client_id)
        
        # Geçersiz bağlantıları kaldır
        for client_id in inactive_connections:
            self.remove_connection(client_id)
            
        # Boş odaları temizle
        empty_rooms = []
        for room_key, clients in self.room_connections.items():
            if room_key != RoomType.ALL and len(clients) == 0:
                empty_rooms.append(room_key)
                
        for room_key in empty_rooms:
            del self.room_connections[room_key]
            logger.debug(f"Boş oda temizlendi: {room_key}")
            
        logger.info(f"WebSocket temizliği tamamlandı: {len(inactive_connections)} bağlantı ve {len(empty_rooms)} oda temizlendi")
    
    async def send_vehicle_update(self, vehicle_data: Dict[str, Any]) -> bool:
        """Araç güncelleme olayını ilgili odalara gönderir"""
        vehicle_id = vehicle_data.get("id")
        license_plate = vehicle_data.get("license_plate")
        message = {
            "type": "vehicle_update",
            "data": vehicle_data
        }
        
        sent = False
        
        # Araç odasına gönder
        if vehicle_id:
            sent = await self.broadcast(message, RoomType.VEHICLE, str(vehicle_id)) or sent
            
        # Plaka ile de araç odasına gönder
        if license_plate:
            sent = await self.broadcast(message, RoomType.VEHICLE, license_plate) or sent
            
        # Admin odasına gönder
        sent = await self.broadcast(message, RoomType.ADMIN) or sent
        
        # Tüm bağlantılara gönder
        sent = await self.broadcast(message) or sent
        
        return sent
    
    async def send_parking_update(self, parking_data: Dict[str, Any]) -> bool:
        """Otopark güncelleme olayını ilgili odalara gönderir"""
        parking_id = parking_data.get("id")
        message = {
            "type": "parking_update",
            "data": parking_data
        }
        
        sent = False
        
        # Otopark odasına gönder
        if parking_id:
            sent = await self.broadcast(message, RoomType.PARKING, str(parking_id)) or sent
            
        # Admin odasına gönder
        sent = await self.broadcast(message, RoomType.ADMIN) or sent
        
        # Tüm bağlantılara gönder
        sent = await self.broadcast(message) or sent
        
        return sent
    
    async def send_parking_record_update(self, record_data: Dict[str, Any]) -> bool:
        """Park kaydı güncelleme olayını ilgili odalara gönderir"""
        vehicle_id = record_data.get("vehicle_id")
        license_plate = record_data.get("license_plate")
        parking_id = record_data.get("parking_id")
        message = {
            "type": "parking_record_update",
            "data": record_data
        }
        
        sent = False
        
        # İlgili araç odasına gönder
        if vehicle_id:
            sent = await self.broadcast(message, RoomType.VEHICLE, str(vehicle_id)) or sent
            
        # Plaka ile de araç odasına gönder
        if license_plate:
            sent = await self.broadcast(message, RoomType.VEHICLE, license_plate) or sent
            
        # İlgili otopark odasına gönder
        if parking_id:
            sent = await self.broadcast(message, RoomType.PARKING, str(parking_id)) or sent
            
        # Admin odasına gönder
        sent = await self.broadcast(message, RoomType.ADMIN) or sent
        
        # Tüm bağlantılara gönder
        sent = await self.broadcast(message) or sent
        
        return sent
        
    async def send_error(self, client_id: str, error_message: str) -> bool:
        """Hata mesajı gönderir"""
        if client_id in active_connections:
            try:
                websocket = active_connections[client_id]
                message = {
                    "type": "error",
                    "message": error_message,
                    "timestamp": datetime.now().isoformat()
                }
                await websocket.send_text(json.dumps(message))
                return True
            except Exception as e:
                logger.error(f"Hata mesajı gönderilirken hata: {str(e)}")
                return False
        return False

# Bağlantı yöneticisi örneği
manager = ConnectionManager() 