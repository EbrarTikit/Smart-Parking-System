import pytest
import sys
import os
import json
import requests
from unittest.mock import patch, MagicMock


sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.gemini_api import get_gemini_response


MOCK_SUCCESS_RESPONSE = {
    "candidates": [
        {
            "content": {
                "parts": [
                    {
                        "text": "Merhaba! Antalya Otopark hizmetleri hakkında size nasıl yardımcı olabilirim?"
                    }
                ]
            }
        }
    ]
}

SAMPLE_CONVERSATION = [
    {"role": "user", "content": "Merhaba"},
    {"role": "assistant", "content": "Size nasıl yardımcı olabilirim?"}
]

def test_get_gemini_response_success():
    """Başarılı bir API yanıtının doğru şekilde işlendiğini test eder"""
    with patch('requests.post') as mock_post:
       
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = MOCK_SUCCESS_RESPONSE
        mock_post.return_value = mock_response
        
       
        response = get_gemini_response("Otopark fiyatları nedir?", SAMPLE_CONVERSATION)
        
        
        assert response == "Merhaba! Antalya Otopark hizmetleri hakkında size nasıl yardımcı olabilirim?"
        
        mock_post.assert_called_once()
        
        call_args = mock_post.call_args
        payload = call_args[1]["json"]
        assert "Kullanıcı: Merhaba" in payload["contents"][0]["parts"][0]["text"]
        assert "Asistan: Size nasıl yardımcı olabilirim?" in payload["contents"][0]["parts"][0]["text"]

def test_get_gemini_response_empty_history():
    """Boş konuşma geçmişiyle API'nin doğru çalıştığını test eder"""
    with patch('requests.post') as mock_post:

        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = MOCK_SUCCESS_RESPONSE
        mock_post.return_value = mock_response
        
        response = get_gemini_response("Otopark fiyatları nedir?", [])
        
        assert response == "Merhaba! Antalya Otopark hizmetleri hakkında size nasıl yardımcı olabilirim?"
        
        call_args = mock_post.call_args
        payload = call_args[1]["json"]
        assert "Önceki Konuşma" not in payload["contents"][0]["parts"][0]["text"]

def test_get_gemini_response_http_error():
    """HTTP hatası durumunda doğru hata mesajının döndüğünü test eder"""
    with patch('requests.post') as mock_post:
        # HTTP hatası fırlat
        mock_post.side_effect = requests.exceptions.HTTPError("404 Client Error")
        
        # API'yi çağır
        response = get_gemini_response("Otopark fiyatları nedir?", [])
        
        # Hata mesajının doğru şekilde döndüğünü kontrol et
        assert "API Hatası" in response
        assert "404 Client Error" in response

def test_get_gemini_response_connection_error():
    """Bağlantı hatası durumunda doğru hata mesajının döndüğünü test eder"""
    with patch('requests.post') as mock_post:

        mock_post.side_effect = requests.exceptions.ConnectionError("Connection refused")
        
    
        response = get_gemini_response("Otopark fiyatları nedir?", [])
        
       
        assert "API Hatası" in response
        assert "Connection refused" in response

def test_get_gemini_response_missing_candidates():
    """API yanıtında 'candidates' alanı olmadığında doğru mesajın döndüğünü test eder"""
    with patch('requests.post') as mock_post:
        
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {}  
        mock_post.return_value = mock_response
        
        
        response = get_gemini_response("Otopark fiyatları nedir?", [])
        
        
        assert response == "Üzgünüm, şu anda yanıt üretemiyorum."

def test_gemini_api_key_configuration():
    """GEMINI_API_KEY çevresel değişkeninin yapılandırıldığını test eder"""
    with patch('os.getenv') as mock_getenv:
        mock_getenv.return_value = "test_api_key"
        
        
        import importlib
        import app.gemini_api
        importlib.reload(app.gemini_api)
        
       
        assert app.gemini_api.GEMINI_API_KEY == "test_api_key"

def test_get_gemini_response_long_conversation_history():
    """Uzun sohbet geçmişinin doğru şekilde kısaltıldığını test eder"""
   
    long_conversation = []
    for i in range(10):
        long_conversation.append({"role": "user", "content": f"Kullanıcı mesajı {i}"})
        long_conversation.append({"role": "assistant", "content": f"Asistan yanıtı {i}"})
    
    with patch('requests.post') as mock_post:
      
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = MOCK_SUCCESS_RESPONSE
        mock_post.return_value = mock_response
        
        
        response = get_gemini_response("Son soru", long_conversation)
        
        
        assert response == "Merhaba! Antalya Otopark hizmetleri hakkında size nasıl yardımcı olabilirim?"
        
        
        call_args = mock_post.call_args
        payload = call_args[1]["json"]
        prompt_text = payload["contents"][0]["parts"][0]["text"]
        
       
        assert "Kullanıcı mesajı 9" in prompt_text
        assert "Asistan yanıtı 9" in prompt_text
        
        assert "Kullanıcı mesajı 0" not in prompt_text
