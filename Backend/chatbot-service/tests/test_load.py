import sys
import os
import pytest
import asyncio
import time
import uuid
import concurrent.futures
from fastapi.testclient import TestClient

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.main import app

client = TestClient(app)

@pytest.mark.skipif("SKIP_LOAD_TESTS" in os.environ, reason="Yük testleri atlanıyor")
class TestChatbotLoad:
    """Chatbot servisi yük testleri"""
    
    def test_consecutive_requests(self):
        """Ardışık istek testi - hızlı chat API çağrıları"""
        start_time = time.time()
        request_count = 10
        successful = 0
        
        # Aynı oturum için ardışık istekler
        session_id = str(uuid.uuid4())
        
        for i in range(request_count):
            response = client.post(
                "/api/v1/chat",
                json={
                    "message": f"Test message {i}",
                    "session_id": session_id
                }
            )
            
            if response.status_code == 200:
                successful += 1
        
        end_time = time.time()
        duration = end_time - start_time
        
        # Sonuçları logla
        print(f"\nArdışık {request_count} istek testi:")
        print(f"Toplam süre: {duration:.2f} saniye")
        print(f"İstek başına ortalama süre: {duration/request_count:.4f} saniye")
        print(f"Başarılı istek: {successful}/{request_count}")
        
        # Testlerin en az %90'ı başarılı olmalı
        assert successful >= request_count * 0.9
    
    def test_parallel_requests(self):
        """Paralel istek testi - eşzamanlı chat API çağrıları"""
        request_count = 10
        successful = 0
        
        # Her istek için yeni bir oturum
        def make_request(i):
            try:
                response = client.post(
                    "/api/v1/chat",
                    json={
                        "message": f"Parallel test message {i}"
                    }
                )
                return response.status_code == 200
            except Exception:
                return False
        
        start_time = time.time()
        
        # ThreadPoolExecutor ile paralel istekler
        with concurrent.futures.ThreadPoolExecutor(max_workers=request_count) as executor:
            results = list(executor.map(make_request, range(request_count)))
            successful = sum(results)
        
        end_time = time.time()
        duration = end_time - start_time
        
        # Sonuçları logla
        print(f"\nParalel {request_count} istek testi:")
        print(f"Toplam süre: {duration:.2f} saniye")
        print(f"İstek başına ortalama süre: {duration/request_count:.4f} saniye")
        print(f"Başarılı istek: {successful}/{request_count}")
        
        # Testlerin en az %90'ı başarılı olmalı
        assert successful >= request_count * 0.9
    
    def test_mixed_endpoint_load(self):
        """Karma endpoint yükü testi - farklı API çağrıları"""
        # Farklı API çağrıları yaparak genel servisi test et
        session_id = str(uuid.uuid4())
        endpoints = [
            # (HTTP methodu, endpoint, payload veya None)
            ("POST", "/api/v1/chat", {"message": "Hello"}),
            ("POST", "/api/v1/chat", {"message": "How are you?", "session_id": session_id}),
            ("GET", f"/api/v1/chat/{session_id}/history", None),
            ("GET", "/api/v1/health", None),
            ("GET", "/api/v1/sessions", None)
        ]
        
        request_count = len(endpoints) * 2  # Her endpoint'i 2 kez çağır
        successful = 0
        
        start_time = time.time()
        
        for _ in range(2):  # 2 tur
            for method, endpoint, payload in endpoints:
                try:
                    if method == "GET":
                        response = client.get(endpoint)
                    elif method == "POST":
                        response = client.post(endpoint, json=payload)
                    elif method == "DELETE":
                        response = client.delete(endpoint)
                    
                    if response.status_code < 400:  # 400 altındaki kodlar başarılı
                        successful += 1
                except Exception:
                    pass
        
        end_time = time.time()
        duration = end_time - start_time
        
        # Sonuçları logla
        print(f"\nKarma {request_count} endpoint çağrısı testi:")
        print(f"Toplam süre: {duration:.2f} saniye")
        print(f"İstek başına ortalama süre: {duration/request_count:.4f} saniye")
        print(f"Başarılı istek: {successful}/{request_count}")
        
        # Testlerin en az %90'ı başarılı olmalı
        assert successful >= request_count * 0.9
    
    def test_long_conversation_performance(self):
        """Uzun sohbet performans testi"""
        # Uzun bir sohbet oluşturarak performans gerilimini test et
        session_id = str(uuid.uuid4())
        conversation_length = 20  # 20 tur soru-cevap
        times = []
        
        for i in range(conversation_length):
            start_time = time.time()
            
            response = client.post(
                "/api/v1/chat",
                json={
                    "message": f"Test message in long conversation: turn {i}",
                    "session_id": session_id
                }
            )
            
            end_time = time.time()
            request_time = end_time - start_time
            times.append(request_time)
            
            assert response.status_code == 200
        
        # Performans istatistikleri
        avg_time = sum(times) / len(times)
        max_time = max(times)
        min_time = min(times)
        
        # Son 5 konuşma turu için ortalama süre
        last_5_avg = sum(times[-5:]) / 5
        
        # İlk 5 konuşma turu için ortalama süre
        first_5_avg = sum(times[:5]) / 5
        
        print(f"\nUzun sohbet performans testi ({conversation_length} tur):")
        print(f"Ortalama yanıt süresi: {avg_time:.4f} saniye")
        print(f"Maksimum yanıt süresi: {max_time:.4f} saniye")
        print(f"Minimum yanıt süresi: {min_time:.4f} saniye")
        print(f"İlk 5 tur ortalama: {first_5_avg:.4f} saniye")
        print(f"Son 5 tur ortalama: {last_5_avg:.4f} saniye")
        
        # Son 5 turun ortalaması, ilk 5 turun 3 katından az olmalı
        # Bu, sohbet uzadıkça performansın çok fazla düşmediğini gösterir
        assert last_5_avg < first_5_avg * 3
        
        # Maksimum yanıt süresi 10 saniyeyi geçmemeli
        assert max_time < 10

@pytest.mark.skipif("SKIP_LOAD_TESTS" in os.environ, reason="Yük testleri atlanıyor")
def test_memory_usage():
    """Bellek kullanımı testi"""
    try:
        import psutil
    except ImportError:
        pytest.skip("psutil kütüphanesi yüklü değil")
    
    process = psutil.Process(os.getpid())
    
    # İlk bellek ölçümü
    start_memory = process.memory_info().rss / 1024 / 1024  # MB
    
    # Birçok istek gönder
    session_id = str(uuid.uuid4())
    for i in range(50):
        client.post(
            "/api/v1/chat",
            json={
                "message": f"Memory test message {i}",
                "session_id": session_id
            }
        )
    
    # Son bellek ölçümü
    end_memory = process.memory_info().rss / 1024 / 1024  # MB
    
    memory_increase = end_memory - start_memory
    
    print(f"\nBellek kullanımı testi:")
    print(f"Başlangıç bellek kullanımı: {start_memory:.2f} MB")
    print(f"Son bellek kullanımı: {end_memory:.2f} MB")
    print(f"Bellek artışı: {memory_increase:.2f} MB")
    
    # Bellek artışı, test boyunca 100MB'den az olmalı
    assert memory_increase < 100 