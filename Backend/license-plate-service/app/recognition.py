import cv2
import numpy as np
import os
import tempfile
from typing import Tuple, Optional

def preprocess_image(image):
    """Görüntüyü plaka tanıma için ön işleme tabi tutar"""
    # Gri tonlamaya çevir
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    
    # Gürültüyü azalt
    gray = cv2.bilateralFilter(gray, 11, 17, 17)
    
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
            license_plate_contour = approx
            break
    
    return license_plate_contour

def extract_license_plate(image, contour):
    """Plaka bölgesini çıkarır"""
    if contour is None:
        return None
    
    # Maskeleme işlemi
    mask = np.zeros(image.shape[:2], np.uint8)
    cv2.drawContours(mask, [contour], 0, 255, -1)
    
    # Maskelenmiş görüntüyü al
    masked_image = cv2.bitwise_and(image, image, mask=mask)
    
    # Plaka bölgesini kırp
    (x, y, w, h) = cv2.boundingRect(contour)
    license_plate = masked_image[y:y+h, x:x+w]
    
    return license_plate

def recognize_text(license_plate_image):
    """Plaka üzerindeki metni tanır (OCR)"""
    # Gerçek bir uygulamada burada Tesseract OCR veya başka bir OCR kütüphanesi kullanılabilir
    # Bu örnek için basit bir simülasyon yapıyoruz
    
    # Simüle edilmiş plaka metni
    # Gerçek uygulamada bu kısım OCR ile değiştirilecek
    return "34ABC123"

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
            return False, "Plaka bulunamadı"
        
        # Plaka bölgesini çıkar
        license_plate_image = extract_license_plate(gray, license_plate_contour)
        
        if license_plate_image is None:
            return False, "Plaka bölgesi çıkarılamadı"
        
        # Plaka metnini tanı
        license_plate_text = recognize_text(license_plate_image)
        
        return True, license_plate_text
        
    except Exception as e:
        return False, f"Hata: {str(e)}"