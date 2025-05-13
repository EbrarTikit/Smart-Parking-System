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
                       exclude: Optional[List[str]] = None) -> None:
        """Belirli bir odadaki tüm bağlantılara mesaj gönderir"""
        if exclude is None:
            exclude = []
            
        room_key = self.get_room_key(room_type, room_id)
        clients = self.get_connections(room_type, room_id)
        
        if not clients:
            logger.debug(f"Oda boş, mesaj gönderilemedi: room={room_key}")
            return
            
        logger.debug(f"Mesaj gönderiliyor: room={room_key}, client_count={len(clients)}")
        
        # Timestamp ekle
        if "timestamp" not in message:
            message["timestamp"] = datetime.now().isoformat()
        
        # JSON formatına dönüştür
        message_json = json.dumps(message)
        
        # Asenkron görevleri topla
        tasks = []
        for client_id in clients:
            if client_id in exclude:
                continue
                
            if client_id in active_connections:
                websocket = active_connections[client_id]
                tasks.append(websocket.send_text(message_json))
        
        # Tüm görevleri çalıştır
        if tasks:
            await asyncio.gather(*tasks, return_exceptions=True)
    
    def get_connection_status(self) -> Dict[str, Any]:
        """Bağlantı durumu bilgisini döndürür"""
        room_stats = {}
        for room_key in self.room_connections:
            room_stats[room_key] = len(self.room_connections[room_key])
            
        return {
            "total_connections": len(active_connections),
            "room_stats": room_stats,
            "clients": list(self.client_info.values())
        }
    
    async def send_vehicle_update(self, vehicle_data: Dict[str, Any]) -> None:
        """Araç güncelleme olayını ilgili odalara gönderir"""
        vehicle_id = vehicle_data.get("id")
        message = {
            "type": "vehicle_update",
            "data": vehicle_data
        }
        
        # Araç odasına gönder
        if vehicle_id:
            await self.broadcast(message, RoomType.VEHICLE, str(vehicle_id))
            
        # Admin odasına gönder
        await self.broadcast(message, RoomType.ADMIN)
        
        # Tüm bağlantılara gönder
        await self.broadcast(message)
    
    async def send_parking_update(self, parking_data: Dict[str, Any]) -> None:
        """Otopark güncelleme olayını ilgili odalara gönderir"""
        parking_id = parking_data.get("id")
        message = {
            "type": "parking_update",
            "data": parking_data
        }
        
        # Otopark odasına gönder
        if parking_id:
            await self.broadcast(message, RoomType.PARKING, str(parking_id))
            
        # Admin odasına gönder
        await self.broadcast(message, RoomType.ADMIN)
        
        # Tüm bağlantılara gönder
        await self.broadcast(message)
    
    async def send_parking_record_update(self, record_data: Dict[str, Any]) -> None:
        """Park kaydı güncelleme olayını ilgili odalara gönderir"""
        vehicle_id = record_data.get("vehicle_id")
        parking_id = record_data.get("parking_id")
        message = {
            "type": "parking_record_update",
            "data": record_data
        }
        
        # İlgili araç odasına gönder
        if vehicle_id:
            await self.broadcast(message, RoomType.VEHICLE, str(vehicle_id))
            
        # İlgili otopark odasına gönder
        if parking_id:
            await self.broadcast(message, RoomType.PARKING, str(parking_id))
            
        # Admin odasına gönder
        await self.broadcast(message, RoomType.ADMIN)
        
        # Tüm bağlantılara gönder
        await self.broadcast(message)
        
    async def send_error(self, client_id: str, error_message: str) -> None:
        """Hata mesajı gönderir"""
        if client_id in active_connections:
            websocket = active_connections[client_id]
            message = {
                "type": "error",
                "message": error_message,
                "timestamp": datetime.now().isoformat()
            }
            await websocket.send_text(json.dumps(message))

# Bağlantı yöneticisi örneği
manager = ConnectionManager() 