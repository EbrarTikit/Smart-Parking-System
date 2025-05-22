import requests
import os
from dotenv import load_dotenv
import json
import logging
from typing import List, Dict

logger = logging.getLogger(__name__)

load_dotenv()

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")

def get_gemini_response(user_message: str, conversation_history: List[Dict]) -> str:
 
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key={GEMINI_API_KEY}"    
    # Geçmiş mesajları formatlama
    conversation_context = ""
    if conversation_history:
        conversation_context = "\n".join([
            f"{'Kullanıcı' if msg['role'] == 'user' else 'Asistan'}: {msg['content']}"
            for msg in conversation_history[-6:]  # Son 3 konuşmayı al
        ])
    
    # İç içe f-string kullanımı yerine daha güvenli bir yaklaşım
    context_section = ""
    if conversation_context:
        context_section = "Önceki Konuşma:\n" + conversation_context + "\n"
    
    prompt = f"""Sen Smart Parking System uygulamamızın chatbotusun. Kullanıcılara Türkiye özelinde otopark ile ilgili konularda yardımcı oluyorsun. Doğru cevap vermek için internetten alman gereken bilgi olursa internetten yararlanabilirsin. 

{context_section}
Kullanıcı: {user_message}

Lütfen önceki konuşmayı dikkate alarak yanıt ver."""

    payload = {
        "contents": [{
            "parts": [{
                "text": prompt
            }]
        }],
        "generationConfig": {
            "temperature": 0.7,
            "topK": 40,
            "topP": 0.95,
            "maxOutputTokens": 1000,
        }
    }

    headers = {"Content-Type": "application/json"}

    try:
        response = requests.post(url, json=payload, headers=headers)
        logger.info(f"API Response Status: {response.status_code}")
        
        response.raise_for_status()
        
        if response.status_code == 200:
            result = response.json()
            if "candidates" in result:
                return result["candidates"][0]["content"]["parts"][0]["text"]
            
        return "Üzgünüm, şu anda yanıt üretemiyorum."
                
    except Exception as e:
        logger.error(f"API Hatası: {str(e)}")
        return f"API Hatası: {str(e)}"
