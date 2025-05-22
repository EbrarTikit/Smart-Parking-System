import pytest
import asyncio
from unittest.mock import Mock, patch, AsyncMock
import json
from datetime import datetime
import sys
import os

# Proje kök dizinini sys.path'e ekle
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# WebSocket modülünü içe aktar
from app.websocket import ConnectionManager, RoomType

class TestConnectionManager:
    """ConnectionManager sınıfı için test suite"""
    
    @pytest.fixture
    def manager(self):
        """Her test için yeni bir ConnectionManager örneği döndürür"""
        return ConnectionManager()
    
    @pytest.fixture
    def mock_websocket(self):
        """WebSocket bağlantısı mock'u"""
        mock_ws = AsyncMock()
        mock_ws.send_text = AsyncMock()
        return mock_ws
    
    def test_add_connection(self, manager, mock_websocket):
        """add_connection metodu bağlantıyı doğru şekilde eklemelidir"""
        client_id = "test-client-1"
        manager.add_connection(client_id, mock_websocket, RoomType.ALL)
        
        # Bağlantı genel odaya eklenmiş olmalı
        assert client_id in manager.room_connections[RoomType.ALL]
        
        # client_info kaydedilmiş olmalı
        assert client_id in manager.client_info
        assert manager.client_info[client_id]["room_type"] == RoomType.ALL
        assert manager.client_info[client_id]["client_id"] == client_id
    
    def test_add_connection_to_room(self, manager, mock_websocket):
        """Belirli bir odaya bağlantı eklenebilmelidir"""
        client_id = "test-client-2"
        room_id = "test-room"
        manager.add_connection(client_id, mock_websocket, RoomType.PARKING, room_id)
        
        # Bağlantı hem genel odaya hem de belirtilen odaya eklenmiş olmalı
        assert client_id in manager.room_connections[RoomType.ALL]
        assert client_id in manager.room_connections[f"{RoomType.PARKING}:{room_id}"]
    
    def test_remove_connection(self, manager, mock_websocket):
        """remove_connection metodu bağlantıyı tüm odalardan kaldırmalıdır"""
        client_id = "test-client-3"
        manager.add_connection(client_id, mock_websocket, RoomType.ALL)
        
        # Bağlantıyı kaldır
        manager.remove_connection(client_id)
        
        # Bağlantı odadan kaldırılmış olmalı
        assert client_id not in manager.room_connections[RoomType.ALL]
        
        # client_info silinmiş olmalı
        assert client_id not in manager.client_info
    
    def test_get_connections(self, manager, mock_websocket):
        """get_connections metodu belli bir odadaki tüm bağlantıları döndürmelidir"""
        client_ids = ["client1", "client2", "client3"]
        for client_id in client_ids:
            manager.add_connection(client_id, mock_websocket, RoomType.ADMIN)
        
        # Farklı bir odaya da bir bağlantı ekle
        manager.add_connection("client4", mock_websocket, RoomType.PARKING, "park1")
        
        # Admin odasındaki bağlantıları al
        connections = manager.get_connections(RoomType.ADMIN)
        
        # 3 admin bağlantısı olmalı
        assert len(connections) == 3
        
        # Tüm admin client_id'leri listede olmalı
        for client_id in client_ids:
            assert client_id in connections
    
    def test_count_connections(self, manager, mock_websocket):
        """count_connections metodu bir odadaki bağlantı sayısını doğru döndürmelidir"""
        # Admin odasına 2 bağlantı ekle
        manager.add_connection("admin1", mock_websocket, RoomType.ADMIN)
        manager.add_connection("admin2", mock_websocket, RoomType.ADMIN)
        
        # Otopark odasına 3 bağlantı ekle
        manager.add_connection("park1", mock_websocket, RoomType.PARKING, "park1")
        manager.add_connection("park2", mock_websocket, RoomType.PARKING, "park1")
        manager.add_connection("park3", mock_websocket, RoomType.PARKING, "park2")
        
        # Araç odasına 1 bağlantı ekle
        manager.add_connection("vehicle1", mock_websocket, RoomType.VEHICLE, "34ABC123")
        
        # Oda başına bağlantı sayıları doğru olmalı
        assert manager.count_connections(RoomType.ADMIN) == 2
        assert manager.count_connections(RoomType.PARKING, "park1") == 2
        assert manager.count_connections(RoomType.PARKING, "park2") == 1
        assert manager.count_connections(RoomType.VEHICLE, "34ABC123") == 1
        
        # Tüm bağlantılar
        assert manager.count_connections(RoomType.ALL) == 6
    
    @pytest.mark.asyncio
    async def test_broadcast(self, manager, mock_websocket):
        """broadcast metodu mesajı odadaki tüm bağlantılara göndermelidir"""
        # 3 bağlantı oluştur
        client_ids = ["client1", "client2", "client3"]
        for client_id in client_ids:
            manager.add_connection(client_id, mock_websocket, RoomType.ADMIN)
        
        # Mesaj gönder
        message = {"type": "info", "message": "Test mesajı"}
        await manager.broadcast(message, RoomType.ADMIN)
        
        # Her bağlantı için send_text çağrılmış olmalı
        expected_calls = len(client_ids)
        assert mock_websocket.send_text.call_count == expected_calls
        
        # JSON formatında mesaj gönderilmiş olmalı
        for call in mock_websocket.send_text.call_args_list:
            sent_message = json.loads(call[0][0])
            assert sent_message["type"] == "info"
            assert sent_message["message"] == "Test mesajı"
            assert "timestamp" in sent_message
    
    @pytest.mark.asyncio
    async def test_broadcast_with_exclude(self, manager, mock_websocket):
        """broadcast metodu exclude listesindeki client'lara mesaj GÖNDERMEMELİDİR"""
        # 3 bağlantı oluştur
        client_ids = ["client1", "client2", "client3"]
        for client_id in client_ids:
            manager.add_connection(client_id, mock_websocket, RoomType.ADMIN)
        
        # client2'yi hariç tutarak mesaj gönder
        message = {"type": "info", "message": "Test mesajı"}
        await manager.broadcast(message, RoomType.ADMIN, exclude=["client2"])
        
        # 2 bağlantıya mesaj gönderilmiş olmalı (3 değil)
        assert mock_websocket.send_text.call_count == 2
    
    def test_get_connection_status(self, manager, mock_websocket):
        """get_connection_status metodu bağlantı durumu bilgisini doğru şekilde döndürmelidir"""
        # Öncelikle manager'in room_connections sözlüğünü temizle
        manager.room_connections = {RoomType.ALL: set()}
        
        # İstemci bilgilerini de temizle
        manager.client_info = {}
        
        # active_connections global değişkenini patch'le
        with patch('app.websocket.active_connections', {}) as mock_active_connections:
            # Farklı odalara bağlantılar ekle
            manager.add_connection("admin1", mock_websocket, RoomType.ADMIN)
            manager.add_connection("admin2", mock_websocket, RoomType.ADMIN)
            manager.add_connection("park1", mock_websocket, RoomType.PARKING, "park1")
            
            # Bağlantıların sayısını doğrula
            assert len(mock_active_connections) == 3
            
            # Bağlantı durumunu al
            status = manager.get_connection_status()
            
            # Doğru bağlantı sayıları olmalı
            assert status["total_connections"] == 3
            assert status["room_stats"][RoomType.ADMIN] == 2
            assert status["room_stats"][f"{RoomType.PARKING}:park1"] == 1
            assert status["room_stats"][RoomType.ALL] == 3
            
            # client_info bilgisi de olmalı
            assert len(status["clients"]) == 3
    
    @pytest.mark.asyncio
    async def test_send_vehicle_update(self, manager, mock_websocket):
        """send_vehicle_update mesajı doğru odalara göndermelidir"""
        # Öncelikle manager'in room_connections sözlüğünü temizle
        manager.room_connections = {RoomType.ALL: set()}
        
        # İstemci bilgilerini de temizle
        manager.client_info = {}
        
        # active_connections global değişkenini patch'le
        with patch('app.websocket.active_connections', {}), \
             patch.object(manager, 'broadcast', new_callable=AsyncMock) as mock_broadcast:
            
            # Farklı odalara bağlantılar ekle
            manager.add_connection("admin1", mock_websocket, RoomType.ADMIN)
            manager.add_connection("vehicle1", mock_websocket, RoomType.VEHICLE, "123")
            manager.add_connection("all1", mock_websocket, RoomType.ALL)
            
            # Araç güncellemesi gönder
            vehicle_data = {
                "id": 123,
                "license_plate": "34ABC123",
                "status": "entry",
                "message": "Araç girişi"
            }
            
            await manager.send_vehicle_update(vehicle_data)
            
            # broadcast 3 kez çağrılmış olmalı
            assert mock_broadcast.call_count == 3
            
            # İlk çağrı vehicle odasına
            first_call_args = mock_broadcast.call_args_list[0][0]
            assert len(first_call_args) >= 3  # En az 3 pozisyonel argüman olmalı
            assert first_call_args[1] == RoomType.VEHICLE  # İkinci argüman room_type
            assert first_call_args[2] == "123"  # Üçüncü argüman room_id
            
            # İkinci çağrı admin odasına
            second_call_args = mock_broadcast.call_args_list[1][0]
            assert len(second_call_args) >= 2  # En az 2 pozisyonel argüman olmalı
            assert second_call_args[1] == RoomType.ADMIN  # İkinci argüman room_type
            
            # Üçüncü çağrı tüm bağlantılara (varsayılan olarak ALL)
            third_call_args = mock_broadcast.call_args_list[2][0]
            assert len(third_call_args) >= 1  # En az 1 pozisyonel argüman olmalı
    
    @pytest.mark.asyncio
    async def test_send_parking_record_update(self, manager, mock_websocket):
        """send_parking_record_update mesajı doğru odalara göndermelidir"""
        # Öncelikle manager'in room_connections sözlüğünü temizle
        manager.room_connections = {RoomType.ALL: set()}
        
        # İstemci bilgilerini de temizle
        manager.client_info = {}
        
        # active_connections global değişkenini patch'le
        with patch('app.websocket.active_connections', {}), \
             patch.object(manager, 'broadcast', new_callable=AsyncMock) as mock_broadcast:
            
            # Farklı odalara bağlantılar ekle
            manager.add_connection("admin1", mock_websocket, RoomType.ADMIN)
            manager.add_connection("vehicle1", mock_websocket, RoomType.VEHICLE, "456")
            manager.add_connection("parking1", mock_websocket, RoomType.PARKING, "789")
            
            # Park kaydı güncellemesi gönder
            record_data = {
                "id": 100,
                "vehicle_id": 456,
                "parking_id": 789,
                "action": "exit",
                "entry_time": "2023-01-01T12:00:00",
                "exit_time": "2023-01-01T14:00:00",
                "duration_hours": 2.0,
                "parking_fee": 20.0
            }
            
            await manager.send_parking_record_update(record_data)
            
            # 4 kez broadcast çağrılmış olmalı (araç odası, otopark odası, admin odası, tüm odalar)
            assert mock_broadcast.call_count == 4
            
            # Çağrıların pozisyonel argümanlarını kontrol et
            calls = mock_broadcast.call_args_list
            
            # İlk çağrı araç odasına
            assert calls[0][0][1] == RoomType.VEHICLE  # İkinci argüman room_type
            assert calls[0][0][2] == "456"  # Üçüncü argüman room_id
            
            # İkinci çağrı otopark odasına
            assert calls[1][0][1] == RoomType.PARKING  # İkinci argüman room_type
            assert calls[1][0][2] == "789"  # Üçüncü argüman room_id
            
            # Üçüncü çağrı admin odasına
            assert calls[2][0][1] == RoomType.ADMIN  # İkinci argüman room_type
            
            # Dördüncü çağrı tüm bağlantılara (varsayılan olarak ALL) 