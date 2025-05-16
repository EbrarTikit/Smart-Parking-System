#!/usr/bin/env python
"""
WebSocket bağlantısını test etmek için Python script'i.

Gerekli paketler:
    pip install websocket-client
"""

import json
import time
import websocket
import threading
import ssl
import argparse

def on_message(ws, message):
    """WebSocket'ten mesaj alındığında çağrılır"""
    try:
        data = json.loads(message)
        print(f"\n[ALINAN] {json.dumps(data, indent=2, ensure_ascii=False)}")
    except json.JSONDecodeError:
        print(f"\n[ALINAN] {message}")

def on_error(ws, error):
    """WebSocket hatası oluştuğunda çağrılır"""
    print(f"\n[HATA] {error}")

def on_close(ws, close_status_code, close_msg):
    """WebSocket bağlantısı kapandığında çağrılır"""
    print(f"\n[KAPANDI] Bağlantı kapatıldı: {close_status_code} - {close_msg}")

def on_open(ws):
    """WebSocket bağlantısı açıldığında çağrılır"""
    print("\n[BAĞLANTI] WebSocket bağlantısı başarıyla açıldı!")
    
    def run(*args):
        try:
            while True:
                print("\nSeçenekler:")
                print("1. Ping mesajı gönder")
                print("2. Durum sorgula")
                print("3. Çıkış yap")
                choice = input("Seçiminiz (1-3): ")
                
                if choice == '1':
                    message = json.dumps({"type": "ping"})
                    ws.send(message)
                    print(f"[GÖNDERİLDİ] {message}")
                elif choice == '2':
                    message = json.dumps({"type": "status"})
                    ws.send(message)
                    print(f"[GÖNDERİLDİ] {message}")
                elif choice == '3':
                    ws.close()
                    print("[ÇIKIŞ] Bağlantı kapatılıyor...")
                    break
                else:
                    print("Geçersiz seçenek!")
                
                time.sleep(1)
        except Exception as e:
            print(f"[HATA] {e}")
            ws.close()
            
    # Yeni bir thread'de arayüzü başlat
    threading.Thread(target=run).start()

def main():
    """Ana fonksiyon"""
    parser = argparse.ArgumentParser(description='WebSocket Test Client')
    parser.add_argument('--url', default='ws://localhost:8005/ws', help='WebSocket URL')
    parser.add_argument('--admin', action='store_true', help='Admin endpoint kullan (/ws/admin)')
    parser.add_argument('--parking', type=int, help='Otopark ID (/ws/parking/{id})')
    parser.add_argument('--vehicle', type=str, help='Araç plakası (/ws/vehicle/{plate})')
    args = parser.parse_args()
    
    # URL oluştur
    websocket_url = args.url
    if args.admin:
        websocket_url = 'ws://localhost:8005/ws/admin'
    elif args.parking:
        websocket_url = f'ws://localhost:8005/ws/parking/{args.parking}'
    elif args.vehicle:
        websocket_url = f'ws://localhost:8005/ws/vehicle/{args.vehicle}'
    
    print(f"WebSocket bağlantısı açılıyor: {websocket_url}")
    
    # WebSocket istemcisini oluştur
    ws = websocket.WebSocketApp(
        websocket_url,
        on_open=on_open,
        on_message=on_message,
        on_error=on_error,
        on_close=on_close
    )
    
    # WebSocket bağlantısını başlat
    ws.run_forever(sslopt={"cert_reqs": ssl.CERT_NONE})

if __name__ == "__main__":
    main() 