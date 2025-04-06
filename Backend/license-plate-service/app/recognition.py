import cv2
import numpy as np
import os
import tempfile
from typing import Tuple, Optional, List, Dict
import logging
import easyocr
import re

# Logger tanımlanması
logger = logging.getLogger(__name__)

# EasyOCR reader'ı singleton olarak tanımla
class OCRSingleton:
    _instance = None
    
    @classmethod
    def get_reader(cls, languages=['tr', 'en']):
        if cls._instance is None:
            logger.info(f"EasyOCR reader başlatılıyor, diller: {languages}")
            try:
                cls._instance = easyocr.Reader(
                    languages,
                    gpu=False,  # GPU varsa True yapılabilir
                    model_storage_directory=os.path.join(os.path.expanduser('~'), '.EasyOCR')
                )
                logger.info("EasyOCR reader başarıyla başlatıldı")
            except Exception as e:
                logger.error(f"EasyOCR reader başlatma hatası: {str(e)}")
                raise
        return cls._instance

def preprocess_image(image):
    """Görüntüyü plaka tanıma için ön işleme tabi tutar"""
    # Gri tonlamaya çevir
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    
    # Gürültüyü azalt
    gray = cv2.bilateralFilter(gray, 11, 17, 17)
    
    # Kontrast iyileştirme
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    gray = clahe.apply(gray)
    
    # Kenarları tespit et
    edged = cv2.Canny(gray, 30, 200)
    
    return gray, edged

def find_contours(edged_image):
    """Görüntüdeki konturları bulur"""
    # Konturları bul
    contours, _ = cv2.findContours(edged_image.copy(), cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    
    # Alanlarına göre sırala (büyükten küçüğe)
    contours = sorted(contours, key=cv2.contourArea, reverse=True)[:10]
    
    return contours

def find_license_plate_contour(contours):
    """Plaka olabilecek konturu bulur"""
    license_plate_contour = None
    
    for contour in contours:
        # Konturun çevresini yaklaşık olarak hesapla
        perimeter = cv2.arcLength(contour, True)
        approx = cv2.approxPolyDP(contour, 0.02 * perimeter, True)
        
        # Plakalar genellikle 4 köşeli olur
        if len(approx) == 4:
            # Ek kontroller - en-boy oranı ve alan
            (x, y, w, h) = cv2.boundingRect(approx)
            aspect_ratio = float(w) / h
            area = cv2.contourArea(contour)
            
            # Türk plakalarının en-boy oranı yaklaşık 4.5:1
            # Alan da belirli bir büyüklükte olmalı
            if 2.0 < aspect_ratio < 6.0 and area > 1000:
                license_plate_contour = approx
                break
    
    return license_plate_contour

def extract_license_plate(image, contour):
    """Plaka bölgesini çıkarır"""
    if contour is None:
        return None
    
    # Maskeleme işlemi yerine doğrudan bounding rectangle kullanma
    (x, y, w, h) = cv2.boundingRect(contour)
    license_plate = image[y:y+h, x:x+w]
    
    # Eğer bulunan bölge çok küçükse, muhtemelen yanlış tespit
    if license_plate.shape[0] < 15 or license_plate.shape[1] < 50:
        return None
    
    return license_plate

def clean_plate_text(text: str) -> str:
    """Tanınan metni temizler ve formatlı plaka metnine dönüştürür"""
    # Tüm boşlukları kaldır
    text = text.replace(" ", "").upper()
    
    # Sadece harf ve rakamları tut
    text = re.sub(r'[^A-Z0-9]', '', text)
    
    # Türk plaka formatına uygunluğu kontrol et: 
    # 11-111-1111 veya 11-11-111 veya 11-111-111 formatında
    tr_plate_pattern = r'^(\d{2})([A-Z]{1,3})(\d{2,4})$'
    match = re.search(tr_plate_pattern, text)
    
    if match:
        # Grupları çıkar ve istenen formata dönüştür
        il_kodu = match.group(1)
        harf = match.group(2)
        numara = match.group(3)
        return f"{il_kodu} {harf} {numara}"
    
    return text

def recognize_text(license_plate_image):
    """Plaka üzerindeki metni tanır (OCR)"""
    try:
        if license_plate_image is None:
            return None
            
        # Görüntüyü yeniden boyutlandırma ve iyileştirme
        height, width = license_plate_image.shape[:2]
        
        # Görüntü kalitesini arttır
        license_plate_image = cv2.resize(license_plate_image, (width*2, height*2), interpolation=cv2.INTER_CUBIC)
        
        # OCR için görüntüyü hazırla - ek işlemler
        # Eşikleme (thresholding)
        _, thresh = cv2.threshold(license_plate_image, 120, 255, cv2.THRESH_BINARY)
        
        # OCR işlemi
        reader = OCRSingleton.get_reader()
        results = reader.readtext(thresh, detail=0, paragraph=False)
        
        # Sonuçları birleştir ve temizle
        if results:
            text = ' '.join(results)
            clean_text = clean_plate_text(text)
            return clean_text
        
        return None
        
    except Exception as e:
        logger.error(f"OCR hatası: {str(e)}")
        return None

def recognize_license_plate(image_data) -> Tuple[bool, Optional[str]]:
    """Ana plaka tanıma fonksiyonu"""
    try:
        # Geçici dosya oluştur
        with tempfile.NamedTemporaryFile(delete=False, suffix='.jpg') as temp_file:
            temp_file.write(image_data)
            temp_file_path = temp_file.name
        
        # Görüntüyü oku
        image = cv2.imread(temp_file_path)
        
        # Geçici dosyayı sil
        os.unlink(temp_file_path)
        
        if image is None:
            return False, "Görüntü okunamadı"
        
        # Görüntüyü ön işleme tabi tut
        gray, edged = preprocess_image(image)
        
        # Konturları bul
        contours = find_contours(edged)
        
        # Plaka konturunu bul
        license_plate_contour = find_license_plate_contour(contours)
        
        if license_plate_contour is None:
            # Plan B: Doğrudan tüm görüntüden OCR dene
            logger.info("Plaka konturu bulunamadı, doğrudan görüntü üzerinde OCR deneniyor")
            license_plate_text = recognize_text(gray)
            if license_plate_text:
                return True, license_plate_text
            return False, "Plaka bulunamadı"
        
        # Plaka bölgesini çıkar
        license_plate_image = extract_license_plate(gray, license_plate_contour)
        
        if license_plate_image is None:
            return False, "Plaka bölgesi çıkarılamadı"
        
        # Plaka metnini tanı
        license_plate_text = recognize_text(license_plate_image)
        
        if not license_plate_text:
            return False, "Plaka metni okunamadı"
        
        return True, license_plate_text
        
    except Exception as e:
        logger.error(f"Plaka tanıma hatası: {str(e)}")
        return False, f"Hata: {str(e)}"