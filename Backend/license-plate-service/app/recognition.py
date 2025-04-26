import cv2
import numpy as np
import os
import tempfile
import pytesseract
from typing import Tuple, Optional, List, Dict
import logging
import re
import sys
import subprocess
import skimage
import time

# Tesseract yolunu platformlar arası ayarla
if os.name == 'nt':  # Windows
    pytesseract.pytesseract.tesseract_cmd = r'C:\Users\Selin\AppData\Local\Programs\Tesseract-OCR\tesseract.exe'
else:  # Linux/Docker
    if os.path.exists('/usr/bin/tesseract'):
        pytesseract.pytesseract.tesseract_cmd = r'/usr/bin/tesseract'

# Logger tanımlanması
logger = logging.getLogger(__name__)

# EasyOCR yerine Tesseract kullanan OCRSingleton sınıfı
class OCRSingleton:
    _instance = None
    
    @classmethod
    def get_reader(cls, languages=['tr', 'en']):
        if cls._instance is None:
            logger.info(f"Tesseract OCR reader başlatılıyor, diller: {languages}")
            try:
                # Tesseract'ı yapılandır
                if os.name == 'nt':
                    tesseract_path = os.getenv('TESSERACT_PATH', r'C:\Users\Selin\AppData\Local\Programs\Tesseract-OCR\tesseract.exe')
                    if os.path.exists(tesseract_path):
                        pytesseract.pytesseract.tesseract_cmd = tesseract_path
                        logger.info(f"Tesseract yolu ayarlandı: {tesseract_path}")
                    else:
                        logger.warning(f"Tesseract yolu bulunamadı: {tesseract_path}")
                
                # Tesseract sürümünü kontrol et
                version = pytesseract.get_tesseract_version()
                logger.info(f"Tesseract sürümü: {version}")
                
                # OCRSingleton örneğini oluştur
                cls._instance = cls()
                logger.info("Tesseract OCR reader başarıyla başlatıldı")
            except Exception as e:
                logger.error(f"Tesseract OCR reader başlatma hatası: {str(e)}")
                raise
        return cls._instance
    
    def readtext(self, image, detail=0, paragraph=False):
        """EasyOCR ile uyumlu arayüz sağla"""
        try:
            # Türkçe ve İngilizce dillerini kullan
            config = '--psm 7 -l tur+eng'
            text = pytesseract.image_to_string(image, config=config)
            
            # EasyOCR benzeri format döndür
            if detail == 0:
                # Sadece metni döndür
                lines = [line.strip() for line in text.split('\n') if line.strip()]
                return lines
            else:
                # EasyOCR formatında sonuç döndür
                return [(None, text.strip(), None)]
        except Exception as e:
            logger.error(f"Tesseract OCR işlemi sırasında hata: {str(e)}")
            return []

# Tesseract yapılandırma sınıfı
class OCRConfig:
    _instance = None

    @classmethod
    def setup(cls):
        if cls._instance is None:
            logger.info("Tesseract OCR yapılandırılıyor")
            try:
                # Windows için Tesseract yolu belirtilmesi
                if os.name == 'nt':
                    # Önce çevre değişkenlerinden yolu al
                    tesseract_path = os.getenv('TESSERACT_PATH')
                    
                    if tesseract_path and os.path.exists(tesseract_path):
                        logger.info(f"Tesseract yolu env değişkeninden bulundu: {tesseract_path}")
                        pytesseract.pytesseract.tesseract_cmd = tesseract_path
                    else:
                        # Kullanıcı için özel yolu kontrol et
                        custom_path = r'C:\Users\Selin\AppData\Local\Programs\Tesseract-OCR\tesseract.exe'
                        if os.path.exists(custom_path):
                            logger.info(f"Tesseract özel yolda bulundu: {custom_path}")
                            pytesseract.pytesseract.tesseract_cmd = custom_path
                        else:
                            # Alternatif yaygın kurulum konumlarını kontrol et
                            alternative_paths = [
                                r'C:\Program Files\Tesseract-OCR\tesseract.exe',
                                r'C:\Program Files (x86)\Tesseract-OCR\tesseract.exe',
                                r'C:\Tesseract-OCR\tesseract.exe'
                            ]
                            
                            found = False
                            for path in alternative_paths:
                                if os.path.exists(path):
                                    logger.info(f"Alternatif Tesseract yolu bulundu: {path}")
                                    pytesseract.pytesseract.tesseract_cmd = path
                                    found = True
                                    break
                            
                            if not found:
                                # Tesseract kurulu mu diye kontrol edelim
                                try:
                                    # Tesseract komutunu doğrudan çalıştırmayı deneyin
                                    result = subprocess.run(['where', 'tesseract'], capture_output=True, text=True)
                                    if result.returncode == 0 and result.stdout.strip():
                                        path = result.stdout.strip().split('\n')[0]
                                        logger.info(f"Tesseract PATH'te bulundu: {path}")
                                        pytesseract.pytesseract.tesseract_cmd = path
                                        found = True
                                except:
                                    pass
                                
                                if not found:
                                    logger.error("Tesseract bulunamadı! Lütfen Tesseract OCR'ı yükleyin ve PATH'e ekleyin.")
                                    print("=" * 60)
                                    print("HATA: Tesseract OCR bulunamadı!")
                                    print("Lütfen Tesseract OCR'ı şu adımları izleyerek yükleyin:")
                                    print("1. https://github.com/UB-Mannheim/tesseract/wiki adresinden indirin")
                                    print("2. Kurulum sırasında Türkçe dil paketini (tur) seçin")
                                    print("3. Kurulumdan sonra PATH'e ekleyin veya .env dosyasında TESSERACT_PATH değişkenini ayarlayın")
                                    print(f"   Mevcut .env dosyasındaki TESSERACT_PATH: {os.getenv('TESSERACT_PATH', 'Ayarlanmamış')}")
                                    print("4. Bu dosya var mı kontrol edin: C:\\Users\\Selin\\AppData\\Local\\Programs\\Tesseract-OCR\\tesseract.exe")
                                    if os.path.exists(r'C:\Users\Selin\AppData\Local\Programs\Tesseract-OCR'):
                                        print("   Tesseract-OCR klasörü mevcut ama tesseract.exe bulunamadı.")
                                        files = os.listdir(r'C:\Users\Selin\AppData\Local\Programs\Tesseract-OCR')
                                        print(f"   Klasördeki dosyalar: {files[:10] if files else 'Boş'}")
                                    print("=" * 60)
                                    raise FileNotFoundError("Tesseract OCR bulunamadı!")
                
                # Test edelim
                version = pytesseract.get_tesseract_version()
                logger.info(f"Tesseract sürümü: {version}")

                # Yapılandırma tamamlandı
                cls._instance = True
                logger.info("Tesseract OCR başarıyla yapılandırıldı")
            except Exception as e:
                logger.error(f"Tesseract OCR yapılandırma hatası: {str(e)}")
                raise
        return cls._instance

def perform_ocr(image, config=None):
    """Geliştirilmiş OCR fonksiyonu - Birden fazla PSM modu dener"""
    # Tesseract yapılandırmasını kontrol et
    OCRConfig.setup()
    
    if config is None:
        # Plaka karakterleri için whitelist (Türk plakaları)
        whitelist = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        
        # Farklı PSM modlarını dene ve en iyi sonucu seç
        results = []
        
        # PSM 7: Tek satır metin (standart plakalar için)
        config_psm7 = f'--psm 7 -l tur+eng -c tessedit_char_whitelist={whitelist}'
        try:
            text_psm7 = pytesseract.image_to_string(image, config=config_psm7).strip()
            if text_psm7:
                results.append((text_psm7, 7))
        except Exception as e:
            logger.error(f"OCR işlemi PSM 7 modunda hata: {str(e)}")
        
        # PSM 6: Tek metin bloğu (çift satırlı plakalar için)
        config_psm6 = f'--psm 6 -l tur+eng -c tessedit_char_whitelist={whitelist}'
        try:
            text_psm6 = pytesseract.image_to_string(image, config=config_psm6).strip()
            if text_psm6:
                results.append((text_psm6, 6))
        except Exception as e:
            logger.error(f"OCR işlemi PSM 6 modunda hata: {str(e)}")
        
        # PSM 8: Tek kelime (bölünmüş veya kısa plakalar için)
        config_psm8 = f'--psm 8 -l tur+eng -c tessedit_char_whitelist={whitelist}'
        try:
            text_psm8 = pytesseract.image_to_string(image, config=config_psm8).strip()
            if text_psm8:
                results.append((text_psm8, 8))
        except Exception as e:
            logger.error(f"OCR işlemi PSM 8 modunda hata: {str(e)}")
        
        # PSM 10: Tek karakter modu (son çare olarak, karakterleri birleştiririz)
        # Not: Bu modda her karakter tek tek analiz edilir
        if len(results) == 0 or max(len(x[0]) for x in results) < 5:  # Diğer modlar yetersizse
            config_psm10 = f'--psm 10 -l tur+eng -c tessedit_char_whitelist={whitelist}'
            try:
                # Görüntüyü gri tonlamaya çevir (eğer değilse)
                if len(image.shape) == 3:
                    gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
                else:
                    gray_image = image.copy()
                
                # Eşikleme uygula
                _, thresh = cv2.threshold(gray_image, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
                
                # Karakterleri bulmak için konturları tespit et
                contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
                
                # Konturları soldan sağa sırala
                char_boxes = []
                for cnt in contours:
                    x, y, w, h = cv2.boundingRect(cnt)
                    # Minimum boyut filtreleme (gürültüyü ele)
                    if w > 5 and h > 10:
                        char_boxes.append((x, y, w, h))
                
                # X koordinatına göre sırala (soldan sağa okuma)
                char_boxes.sort(key=lambda box: box[0])
                
                # Her karakteri ayrı ayrı tanı ve birleştir
                recognized_chars = []
                for x, y, w, h in char_boxes[:15]:  # En fazla 15 karakter al (plaka uzunluğu sınırı)
                    char_img = gray_image[y:y+h, x:x+w]
                    # Pad ekleyerek karakter boyutunu artır
                    pad = 2
                    char_img = cv2.copyMakeBorder(char_img, pad, pad, pad, pad, cv2.BORDER_CONSTANT, value=255)
                    
                    # Her karakteri ayrı ayrı tanı
                    char = pytesseract.image_to_string(char_img, config=config_psm10).strip()
                    
                    # Sadece alfanumerik ve boş olmayan sonuçları al
                    if char and re.match(r'[A-Z0-9]', char, re.IGNORECASE):
                        recognized_chars.append(char[0])  # İlk karakteri al
                
                if recognized_chars:
                    char_text = ''.join(recognized_chars)
                    results.append((char_text, 10))
                    logger.info(f"PSM 10 karakter bazlı tanıma sonucu: {char_text}")
            except Exception as e:
                logger.error(f"OCR işlemi PSM 10 modunda hata: {str(e)}")
        
        # En iyi sonucu seç
        if results:
            # Önce plaka formatı olanlara bakılır
            valid_plates = []
            for text, psm in results:
                clean_text = clean_plate_text(text)
                # Türk plaka formatına uygunsa
                tr_plate_pattern = r'^(\d{2})\s([A-Z]{1,3})\s(\d{2,4})$'
                if re.match(tr_plate_pattern, clean_text):
                    valid_plates.append((clean_text, psm))
            
            # Formata uygun plaka bulunduysa onu kullan
            if valid_plates:
                best_text, best_psm = valid_plates[0]
                logger.info(f"Formata uygun en iyi sonuç PSM {best_psm} ile bulundu: {best_text}")
                return best_text
            
            # Formata uygun plaka bulunamadıysa, alfanumerik karakter sayısına göre sırala
            results.sort(key=lambda x: len(re.findall(r'[A-Z0-9]', x[0])), reverse=True)
            best_text, best_psm = results[0]
            logger.info(f"En iyi sonuç PSM {best_psm} ile bulundu: {best_text} (format uygun değil)")
            return best_text
        
        return None
    else:
        # Kullanıcı tarafından belirtilen yapılandırmayı kullan
        try:
         text = pytesseract.image_to_string(image, config=config)
         return text.strip()
        except Exception as e:
         logger.error(f"OCR işlemi sırasında hata: {str(e)}")
         return None


def preprocess_image(image):
    """Temel görüntü ön işleme işlevleri"""
    # Orijinal görüntüyü kopyala
    original = image.copy()

    # Gri tonlamaya çevir
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Boyutlandırma - Tesseract için optimize edilmiş boyut
    height, width = gray.shape
    target_width = 400
    if width < target_width:
        ratio = target_width / width
        gray = cv2.resize(gray, (target_width, int(height * ratio)), interpolation=cv2.INTER_CUBIC)

    # Gürültü azaltma - temel bulanıklaştırma
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    
    # Basit kenar tespiti
    edges = cv2.Canny(blurred, 50, 150)

    return gray, blurred, edges

def enhanced_preprocessing(image):
    """Türk plaka tespiti için geliştirilmiş görüntü ön işleme"""
    # Gri tonlamaya çevir
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    
    # Boyut normalleştirme - plaka tespiti için ideal boyut
    height, width = gray.shape
    target_width = 400
    if width < target_width:
        ratio = target_width / width
        gray = cv2.resize(gray, (target_width, int(height * ratio)), interpolation=cv2.INTER_CUBIC)
    
    # Gürültü azaltma - bilateral filtreleme (kenarları koruyarak yumuşatma)
    blurred = cv2.bilateralFilter(gray, 11, 17, 17)
    
    # Kontrast artırma - CLAHE algoritması
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    enhanced = clahe.apply(blurred)
    
    # Keskinleştirme
    kernel_sharpen = np.array([[-1, -1, -1], [-1, 9, -1], [-1, -1, -1]])
    sharpened = cv2.filter2D(enhanced, -1, kernel_sharpen)
    
    # Kenar tespiti - parametreler plaka kenarları için optimize edildi
    edges = cv2.Canny(enhanced, 30, 200)
    
    # Morfolojik işlemler
    kernel = np.ones((3, 3), np.uint8)
    dilated = cv2.dilate(edges, kernel, iterations=1)
    
    # Histogram eşitleme (alternatif bir görüntü iyileştirme yöntemi)
    equalized = cv2.equalizeHist(gray)
    
    # Otsu eşikleme
    _, otsu = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    
    # Adaptif eşikleme
    adaptive = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
                                    cv2.THRESH_BINARY, 11, 2)
    
    return {
        'gray': gray,
        'blurred': blurred,
        'enhanced': enhanced,
        'sharpened': sharpened,
        'edges': edges,
        'dilated': dilated,
        'equalized': equalized,
        'otsu': otsu,
        'adaptive': adaptive
    }

def find_contours(edged_image):
    """Görüntüdeki konturları bulur"""
    # Konturları bul
    contours, _ = cv2.findContours(edged_image.copy(), cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    
    # Alanlarına göre sırala (büyükten küçüğe)
    contours = sorted(contours, key=cv2.contourArea, reverse=True)[:10]
    
    return contours

def find_license_plate_contour(contours, image_shape):
    """Geliştirilmiş plaka konturu tespit fonksiyonu"""
    height, width = image_shape[:2]
    min_area = (width * height) * 0.01  # Minimum alan (görüntünün %1'i)
    max_area = (width * height) * 0.3   # Maksimum alan (görüntünün %30'u)

    license_plate_contour = None

    for contour in contours:
        area = cv2.contourArea(contour)
        if area < min_area or area > max_area:
            continue

        # Konturun çevresini yaklaşık olarak hesapla
        perimeter = cv2.arcLength(contour, True)
        approx = cv2.approxPolyDP(contour, 0.02 * perimeter, True)

        # Dörtgen kontrolleri
        if len(approx) == 4:
            (x, y, w, h) = cv2.boundingRect(approx)
            aspect_ratio = float(w) / h

            # Türk plakalarının en-boy oranı kontrol (2 ila 6 arası)
            if 2.0 < aspect_ratio < 6.0:
                license_plate_contour = approx
                break

        # 4 köşeli olmayan konturlar için alternatif yöntem
        if len(approx) >= 4 and len(approx) <= 8:
            # Konturun dikdörtgen olup olmadığını kontrol et
            rect = cv2.minAreaRect(contour)
            (_, _), (width, height), angle = rect

            if width > 0 and height > 0:
                aspect_ratio = max(width, height) / min(width, height)
                if 2.0 < aspect_ratio < 6.0:
                    box = cv2.boxPoints(rect)
                    box = np.int0(box)
                    license_plate_contour = box
                    break

    return license_plate_contour

def alternative_plate_detection(image):
    """Hog tabanlı plaka algılama yöntemi"""
    try:
        from skimage.feature import hog
        from skimage import exposure
        import skimage

        # Görüntüyü gri tonlamaya çevir
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        
        # scikit-image sürümünü kontrol et ve uygun parametreyi kullan
        sk_version = [int(i) for i in skimage.__version__.split('.')]
        
        # Sürüm 0.19.0'dan büyükse channel_axis kullan, değilse multichannel
        if sk_version[0] > 0 or (sk_version[0] == 0 and sk_version[1] >= 19):
            fd, hog_image = hog(gray, orientations=9, pixels_per_cell=(8, 8),
                                cells_per_block=(2, 2), visualize=True, channel_axis=None)
        else:
            fd, hog_image = hog(gray, orientations=9, pixels_per_cell=(8, 8),
                                cells_per_block=(2, 2), visualize=True, multichannel=False)

        # HOG görüntüsünü normalize et
        hog_image_rescaled = exposure.rescale_intensity(hog_image, in_range=(0, 10))

        # Eşikleme
        _, binary = cv2.threshold(hog_image_rescaled.astype(np.uint8) * 255, 0, 255,
                                cv2.THRESH_BINARY + cv2.THRESH_OTSU)

        # Açma işlemi ile gürültüyü temizle
        kernel = np.ones((3, 3), np.uint8)
        opening = cv2.morphologyEx(binary, cv2.MORPH_OPEN, kernel)

        # Konturları bul
        contours, _ = cv2.findContours(opening, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        # En büyük konturları seç
        large_contours = sorted(contours, key=cv2.contourArea, reverse=True)[:5]

        return large_contours
    except Exception as e:
        logger.warning(f"HOG tabanlı algılama sırasında hata: {str(e)}")
        return []  # Hata durumunda boş liste döndür

def turkish_plate_detector(image):
    """Türk plakalarına özel algılama algoritması"""
    try:
        # Görüntüyü ön işleme
        preprocessed = enhanced_preprocessing(image)

        # Mavi şerit algılama (Türk plakalarındaki mavi bölge)
        hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)
        lower_blue = np.array([100, 50, 50])
        upper_blue = np.array([130, 255, 255])
        blue_mask = cv2.inRange(hsv, lower_blue, upper_blue)

        # Konturları bul
        contours, _ = cv2.findContours(preprocessed['dilated'], cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)

        # Alanlarına göre sırala (büyükten küçüğe)
        contours = sorted(contours, key=cv2.contourArea, reverse=True)[:10]

        # Her kontur için
        for contour in contours:
            # Dikdörtgensel alan oluştur
            rect = cv2.minAreaRect(contour)
            (_, _), (width, height), angle = rect

            # Türk plakası en-boy oranı kontrolü (4:1 civarında)
            aspect_ratio = max(width, height) / min(width, height)
            if 3.5 <= aspect_ratio <= 6.0:  # Tolerans ile
                box = cv2.boxPoints(rect)
                box = np.int0(box)

                # Mavi şerit kontrolü - bu konturun içinde mavi şerit var mı?
                mask = np.zeros_like(blue_mask)
                cv2.drawContours(mask, [box], 0, 255, -1)
                if cv2.countNonZero(cv2.bitwise_and(blue_mask, mask)) > 0:
                    return box

        return None
    except Exception as e:
        logger.warning(f"Türk plaka algılama algoritmasında hata: {str(e)}")
        return None

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


def preprocess_plate_image(plate_image):
    """Plaka görüntüsünü tesseract için optimize eder"""
    
    # Orijinal görüntüyü kopyala
    processed = plate_image.copy()
    
    # Mavi bölgeyi filtreleme (TR logosu ve mavi zemin)
    hsv = cv2.cvtColor(processed, cv2.COLOR_BGR2HSV)
    lower_blue = np.array([100, 50, 50])
    upper_blue = np.array([130, 255, 255])
    blue_mask = cv2.inRange(hsv, lower_blue, upper_blue)
    
    # Mavi bölgeyi beyaz yap (255)
    processed[blue_mask > 0] = [255, 255, 255]
    
    # Gri tonlamaya çevir
    gray = cv2.cvtColor(processed, cv2.COLOR_BGR2GRAY)
    
    # Kontrast artırma
    alpha = 2.0  # Kontrast katsayısı
    beta = 10    # Parlaklık katsayısı
    gray = cv2.convertScaleAbs(gray, alpha=alpha, beta=beta)
    
    # Adaptif eşikleme
    thresh = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                 cv2.THRESH_BINARY_INV, 19, 9)
    
    # Morfolojik operasyonlar ile gürültü temizleme
    kernel = np.ones((3,3), np.uint8)
    opening = cv2.morphologyEx(thresh, cv2.MORPH_OPEN, kernel, iterations=1)
    closing = cv2.morphologyEx(opening, cv2.MORPH_CLOSE, kernel, iterations=2)
    
    # Sonucu döndür (binary görüntü)
    return cv2.bitwise_not(closing)  # Karakterlerin siyah, arkaplanın beyaz olmasını sağla

def perform_optimized_ocr(image):
    """Plaka tanımaya özel optimize edilmiş OCR"""
    # Tesseract yapılandırmasını kontrol et
    OCRConfig.setup()
    
    # Türk plakalarında kullanılan karakterler
    tr_plate_chars = "0123456789ABCDEFGHIJKLMNOPRSTUVYZ"
    
    # Farklı PSM modları deneyerek en iyi sonucu bulma
    results = []
    
    # PSM 7: Tek satır
    config_psm7 = f'--psm 7 -l tur+eng -c tessedit_char_whitelist={tr_plate_chars}'
    try:
        text_psm7 = pytesseract.image_to_string(image, config=config_psm7).strip()
        if text_psm7:
            results.append((text_psm7, 7))
            logger.info(f"PSM 7 sonucu: {text_psm7}")
    except Exception as e:
        logger.error(f"PSM 7 hatası: {str(e)}")
    
    # PSM 8: Tek kelime
    config_psm8 = f'--psm 8 -l tur+eng -c tessedit_char_whitelist={tr_plate_chars}'
    try:
        text_psm8 = pytesseract.image_to_string(image, config=config_psm8).strip()
        if text_psm8:
            results.append((text_psm8, 8))
            logger.info(f"PSM 8 sonucu: {text_psm8}")
    except Exception as e:
        logger.error(f"PSM 8 hatası: {str(e)}")
    
    # PSM 11: Sparse text - Dağınık metin (karakterleri tek tek tanıma)
    config_psm11 = f'--psm 11 -l tur+eng -c tessedit_char_whitelist={tr_plate_chars}'
    try:
        text_psm11 = pytesseract.image_to_string(image, config=config_psm11).strip()
        if text_psm11:
            results.append((text_psm11, 11))
            logger.info(f"PSM 11 sonucu: {text_psm11}")
    except Exception as e:
        logger.error(f"PSM 11 hatası: {str(e)}")
    
    # PSM 13: Raw line - Ham satır
    config_psm13 = f'--psm 13 -l tur+eng -c tessedit_char_whitelist={tr_plate_chars}'
    try:
        text_psm13 = pytesseract.image_to_string(image, config=config_psm13).strip()
        if text_psm13:
            results.append((text_psm13, 13))
            logger.info(f"PSM 13 sonucu: {text_psm13}")
    except Exception as e:
        logger.error(f"PSM 13 hatası: {str(e)}")
    
    if not results:
        logger.warning("Hiçbir PSM modu sonuç vermedi")
        return None
    
    # En iyi sonucu döndür (en uzun metin genellikle en iyidir)
    results.sort(key=lambda x: len(x[0]), reverse=True)
    best_text, best_psm = results[0]
    logger.info(f"En iyi sonuç PSM {best_psm} ile bulundu: {best_text}")
    return best_text

def clean_plate_text(text):
    """Tanınan metni temizle ve Türk plaka formatına dönüştür"""
    # Önemli: Bu fonksiyonu validate_plate_format ile değiştiriyoruz
    # Sadece eski kodla uyumluluk için tutuyoruz
    return validate_plate_format(text)

def validate_plate_format(text):
    """Türk plaka formatını doğrular ve düzeltir - Geliştirilmiş esnek yaklaşım"""
    if not text:
        return None

    # Başlangıç metni (debug için)
    original_text = text
    
    # ------------------------
    # 1. ADIM: İLK TEMİZLEME
    # ------------------------
    
    # Gereksiz karakterleri kaldır (tire, nokta, virgül, vb.)
    text = re.sub(r'[^A-Za-z0-9\s]', '', text)
    
    # Büyük harfe dönüştür
    text = text.upper()
    
    # Türkçe karakterleri İngilizce karakterlere dönüştür
    tr_en_map = {
        'Ç': 'C', 'Ğ': 'G', 'İ': 'I', 'Ö': 'O', 'Ş': 'S', 'Ü': 'U'
    }
    for tr_char, en_char in tr_en_map.items():
        text = text.replace(tr_char, en_char)
    
    # OCR hatalarını düzelt - Benzer görünen karakterler
    ocr_corrections = {
        'D': '0', 'Q': '0', 'O': '0',  # Harfleri rakamlara düzeltme
        'I': '1', 'İ': '1', 'L': '1',
        'Z': '2',
        'A': '4', 'H': '4',
        'S': '5', 'Ş': '5',
        'G': '6', 'Ğ': '6',
        'T': '7',
        'B': '8',
        '¹': '1', '²': '2', '³': '3'  # Üst simgeler
    }
    
    # Tüm boşlukları kaldır - daha sonra doğru yere ekleyeceğiz
    text = text.replace(" ", "")
    
    # ------------------------
    # 2. ADIM: KARAKTER SINIFLANDIRMA
    # ------------------------
    
    # Karakter bazlı iterasyon ile temizleme
    clean_chars = []
    
    for char in text:
        # Karakteri düzelt
        if char in ocr_corrections:
            char = ocr_corrections[char]
        
        # Alfanümerik kontrolü
        if char.isalnum():
            clean_chars.append(char)
    
    # Temizlenmiş metni oluştur
    clean_text = ''.join(clean_chars)
    
    # Çok kısa metinleri ele
    if len(clean_text) < 4:
        logger.warning(f"Çok kısa plaka metni: '{original_text}' -> '{clean_text}'")
        return clean_text if clean_text else None
    
    # ------------------------
    # 3. ADIM: İL KODU BELİRLEME
    # ------------------------
    
    # Geçerli il kodları (01-81)
    valid_il_codes = set(str(i).zfill(2) for i in range(1, 82))
    
    il_kodu = None
    remaining_text = clean_text
    
    # İlk iki karakter doğrudan il kodu mu?
    if len(clean_text) >= 2 and clean_text[:2].isdigit() and clean_text[:2] in valid_il_codes:
        il_kodu = clean_text[:2]
        remaining_text = clean_text[2:]
    
    # İlk karakter rakam değil ama potansiyel bir OCR hatası olabilir mi?
    elif len(clean_text) >= 2 and not clean_text[0].isdigit():
        # İlk iki karakteri değiştirmek için düzeltme tablosu
        il_corrections = {
            'P': '1', 'Y': '6',  # PY -> 16 dönüşümü
            'R': '8', 'A': '4',  # RA -> 84 dönüşümü
            'T': '7', 'F': '5',  # TF -> 75 dönüşümü
            'E': '3', 'C': '0',  # EC -> 30 dönüşümü
            'B': '8', 'O': '0',  # BO -> 80 dönüşümü
            'S': '5', 'H': '4',  # SH -> 54 dönüşümü
            'J': '3', 'K': '4',  # JK -> 34 dönüşümü
            'M': '1', 'N': '7',  # MN -> 17 dönüşümü
            'V': '7', 'W': '8',  # VW -> 78 dönüşümü
            'X': '8'            # X -> 8 dönüşümü
        }
        
        # İlk karakteri düzelt
        if clean_text[0] in il_corrections:
            first_digit = il_corrections[clean_text[0]]
            
            # İkinci karakter rakam mı?
            if len(clean_text) >= 2 and clean_text[1].isdigit():
                potential_il = first_digit + clean_text[1]
                if potential_il in valid_il_codes:
                    il_kodu = potential_il
                    remaining_text = clean_text[2:]
            
            # İkinci karakter de harf mi (iki OCR hatası)?
            elif len(clean_text) >= 2 and clean_text[1] in il_corrections:
                second_digit = il_corrections[clean_text[1]]
                potential_il = first_digit + second_digit
                if potential_il in valid_il_codes:
                    il_kodu = potential_il
                    remaining_text = clean_text[2:]
    
    # İl kodu bulunamadıysa, yaygın il kodlarını varsayılan olarak dene
    if not il_kodu:
        # En yaygın il kodları
        common_il_codes = ['34', '06', '35', '01', '16', '07']
        
        # Metinde sadece harf ve rakam varsa
        if clean_text.isalnum():
            # Bu durumda metin muhtemelen bir plaka
            # Varsayılan olarak yaygın il kodlarını deneyelim
            for code in common_il_codes:
                # Metin bir harf ve rakam kombinasyonuyla devam ediyorsa
                if len(clean_text) >= 5:  # En az 5 karakter (2 il + 2 harf + 1 rakam)
                    # İl kodunu varsay ve devam et
                    il_kodu = code
                    remaining_text = clean_text  # Tüm metin gerekli olabilir
                    logger.info(f"İl kodu bulunamadı, varsayılan il kodu kullanılıyor: {code}")
                    break
    
    # Eğer hala il kodu bulunamadıysa, plaka formatını tanıyamıyoruz
    if not il_kodu:
        logger.warning(f"Plakada il kodu tespit edilemedi: '{original_text}' -> '{clean_text}'")
        return clean_text  # En azından temizlenmiş metni döndür
    
    # ------------------------
    # 4. ADIM: PLAKA BÖLÜMÜ AYRIŞTIRMA
    # ------------------------
    
    # Kalan metni harf ve rakam bölümlerine ayır
    alpha_part = ""
    numeric_part = ""
    
    # İl kodundan sonra ilk harfleri topla
    for i, char in enumerate(remaining_text):
        if char.isalpha():
            alpha_part += char
        else:  # Rakam
            # İlk rakamdan sonraki kısım numeric_part'a eklenir
            numeric_part = remaining_text[i:]
            break
    
    # Eğer rakam bulunamadıysa, tüm kalan kısım harf olabilir (özel plakalar)
    if not numeric_part and alpha_part:
        # Harf kısmı çok uzunsa (4'ten fazla) potansiyel olarak içinde rakam vardır
        if len(alpha_part) > 4:
            # Harfleri ve rakamları ayırmaya çalış
            potential_alpha = ""
            potential_numeric = ""
            
            for char in alpha_part:
                if char in ocr_corrections and char.isalpha():
                    # OCR hatasını düzelt
                    potential_numeric += ocr_corrections[char]
                elif char.isdigit():
                    potential_numeric += char
                else:
                    potential_alpha += char
            
            if potential_alpha and potential_numeric:
                alpha_part = potential_alpha
                numeric_part = potential_numeric
    
    # Bir plaka formatı kontrolü yap
    if not alpha_part:
        # Sadece rakamlar var, potansiyel olarak ilk 1-3 karakter harf olabilir
        alpha_part = remaining_text[:min(3, len(remaining_text))]
        numeric_part = remaining_text[min(3, len(remaining_text)):]
    
    # Düğümlenmiş (rakam-harf-rakam gibi) formatları çöz
    # Örneğin: "34A12B56" -> il_kodu="34", alpha_part="AB", numeric_part="1256"
    if len(numeric_part) > 1 and any(c.isalpha() for c in numeric_part):
        new_alpha = alpha_part
        new_numeric = ""
        
        for char in numeric_part:
            if char.isalpha():
                new_alpha += char
            else:
                new_numeric += char
        
        alpha_part = new_alpha
        numeric_part = new_numeric
    
    # Çok uzun alfa kısmını kısalt (en fazla 3 harf)
    if len(alpha_part) > 3:
        alpha_part = alpha_part[:3]
    
    # Çok uzun rakam kısmını kısalt (en fazla 5 rakam)
    if len(numeric_part) > 5:
        numeric_part = numeric_part[:5]
    
    # ------------------------
    # 5. ADIM: PLAKA FORMATI OLUŞTURMA
    # ------------------------
    
    # Son kontrol: Plaka formatını oluştur
    if alpha_part and numeric_part:
        result = f"{il_kodu} {alpha_part} {numeric_part}"
        logger.info(f"Plaka formatı oluşturuldu: '{original_text}' -> '{result}'")
        return result
    elif alpha_part:  # Sadece harf kısmı varsa (özel plakalar)
        result = f"{il_kodu} {alpha_part}"
        logger.info(f"Özel plaka formatı oluşturuldu: '{original_text}' -> '{result}'")
        return result
    
    # Son çare: Formatı düzeltemedik, en azından temizlenmiş metni ve il kodunu döndür
    result = f"{il_kodu} {remaining_text}"
    logger.warning(f"Standart format oluşturulamadı, basit format döndürülüyor: '{original_text}' -> '{result}'")
    return result


def recognize_text(license_plate_image):
    """Plaka üzerindeki metni Tesseract ile tanır"""
    try:
        if license_plate_image is None:
            return None

        # Görüntüyü yeniden boyutlandırma ve iyileştirme
        height, width = license_plate_image.shape[:2]

        # Görüntü kalitesini arttır
        license_plate_image = cv2.resize(license_plate_image, (width*3, height*3), interpolation=cv2.INTER_CUBIC)

        # TR logosu filtreleme ve plaka optimize etme
        optimized_plate = preprocess_plate_image(license_plate_image)
        
        # Eski görüntü işleme adımlarını da saklayalım
        kernel = np.array([[-1,-1,-1], [-1,9,-1], [-1,-1,-1]])
        sharpened = cv2.filter2D(license_plate_image, -1, kernel)

        # OCR için görüntüyü hazırla - farklı ön işlemelerle deneme
        # 1. Temel Eşikleme
        _, thresh1 = cv2.threshold(sharpened, 120, 255, cv2.THRESH_BINARY)

        # 2. Adaptif Eşikleme
        thresh2 = cv2.adaptiveThreshold(sharpened, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                      cv2.THRESH_BINARY, 11, 2)

        # Sonuçları topla
        results = []

        # YENİ: Optimize edilmiş OCR fonksiyonunu kullan
        text_optimized = perform_optimized_ocr(optimized_plate)
        if text_optimized and len(text_optimized) > 3:
            results.append(text_optimized)
            logger.info(f"Optimize edilmiş OCR fonksiyonu ile tanınan plaka: {text_optimized}")
        
        # YENİ: Orijinal plaka görüntüsünde optimize edilmiş OCR
        text_original_optimized = perform_optimized_ocr(license_plate_image)
        if text_original_optimized and len(text_original_optimized) > 3 and text_original_optimized != text_optimized:
            results.append(text_original_optimized)
            logger.info(f"Orijinal görüntüde optimize edilmiş OCR ile tanınan plaka: {text_original_optimized}")
        
        # Eski yöntemlerle de deneyelim
        # İlk deneme - temel eşikleme
        text1 = perform_ocr(thresh1, config='--psm 7 -l tur')
        if text1 and len(text1) > 3:
            results.append(text1)

        # İkinci deneme - adaptif eşikleme
        text2 = perform_ocr(thresh2, config='--psm 7 -l tur')
        if text2 and len(text2) > 3:
            results.append(text2)

        # Orijinal görüntüde deneme
        text3 = perform_ocr(sharpened, config='--psm 7 -l tur')
        if text3 and len(text3) > 3:
            results.append(text3)

        # Sonuçları değerlendir
        if results:
            # Özel temizleme işlevi uygula
            cleaned_results = []
            for text in results:
             clean_text = clean_plate_text(text)
             if clean_text:
                 cleaned_results.append(clean_text)
            
            # En iyi sonucu seç (doğru formatta olanı tercih et)
            for result in cleaned_results:
                # İl kodu + harf + rakam formatını kontrol et
                pattern = r'^\d{2}\s[A-Z]{1,3}\s\d{1,4}$'
                if re.match(pattern, result):
                    logger.info(f"Doğru formatta plaka bulundu: {result}")
                    return result
            
            # Doğru formatta yoksa, en uzun sonucu döndür
            if cleaned_results:
                best_result = max(cleaned_results, key=len)
                logger.info(f"En iyi plaka sonucu: {best_result}")
                return best_result

        return None

    except Exception as e:
        logger.error(f"OCR hatası: {str(e)}")
        return None

def recognize_license_plate(image_data) -> Tuple[bool, Optional[str]]:
    """Ana plaka tanıma fonksiyonu - Tesseract OCR ile"""
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

        # Görüntüyü ön işleme tabi tut (temel işlemler)
        gray, blurred, edges = preprocess_image(image)

        # 1. Yöntem: Standart kontur tabanlı plaka algılama
        contours = find_contours(edges)
        license_plate_contour = find_license_plate_contour(contours, image.shape)

        # 2. Yöntem: Türk plakalarına özel algılama
        if license_plate_contour is None:
            logger.info("Standart yöntemle plaka bulunamadı, Türk plaka algılama deneniyor")
            # Gelişmiş ön işleme - Türk plaka algılama için optimize edilmiş
            turkish_plate_box = turkish_plate_detector(image)
            
            if turkish_plate_box is not None:
                license_plate_contour = turkish_plate_box
                logger.info("Türk plaka algılama algoritması ile plaka bulundu")

        # 3. Yöntem: HOG tabanlı algılama
        if license_plate_contour is None:
            logger.info("Türk plaka algılama ile plaka bulunamadı, HOG tabanlı algılama deneniyor")
            hog_contours = alternative_plate_detection(image)
            license_plate_contour = find_license_plate_contour(hog_contours, image.shape)

        if license_plate_contour is None:
            # Son çare: Gelişmiş ön işleme ve doğrudan OCR dene
            logger.info("Hiçbir yöntemle plaka konturu bulunamadı, gelişmiş görüntü işleme ve doğrudan OCR deneniyor")
            enhanced_results = enhanced_preprocessing(image)
            
            # Farklı ön işleme sonuçlarını dene
            for name, processed_img in enhanced_results.items():
                if name in ['gray', 'blurred', 'enhanced', 'sharpened', 'equalized', 'adaptive', 'otsu']:
                    license_plate_text = perform_ocr(processed_img, config='--psm 7 --oem 3 -l tur+eng')
                    if license_plate_text:
                        clean_text = clean_plate_text(license_plate_text)
                        if len(clean_text) >= 4:  # Makul bir plaka uzunluğu
                            logger.info(f"Doğrudan OCR başarılı oldu (yöntem: {name}): {clean_text}")
                            return True, clean_text
            
            return False, "Plaka bulunamadı"

        # Plaka bölgesini çıkar - önce normal gri görüntüde dene
        license_plate_image = extract_license_plate(gray, license_plate_contour)

        if license_plate_image is None:
            return False, "Plaka bölgesi çıkarılamadı"

        # Plaka metnini tanı
        license_plate_text = recognize_text(license_plate_image)

        if not license_plate_text:
            # Plan B: Blurred görüntüde dene
            license_plate_image = extract_license_plate(blurred, license_plate_contour)
            license_plate_text = recognize_text(license_plate_image)

            if not license_plate_text:
                # Plan C: Gelişmiş ön işleme ile tekrar dene
                enhanced_imgs = enhanced_preprocessing(image)
                
                # Sırayla farklı görüntü işleme sonuçlarını dene
                for name in ['enhanced', 'sharpened', 'equalized', 'adaptive']:
                    if license_plate_text:
                        break
                        
                    img = enhanced_imgs[name]
                    license_plate_image = extract_license_plate(img, license_plate_contour)
                    if license_plate_image is not None:
                        license_plate_text = recognize_text(license_plate_image)
                        if license_plate_text:
                            logger.info(f"Plaka metni tanıma başarılı oldu (yöntem: {name}): {license_plate_text}")
                
                if not license_plate_text:
                    return False, "Plaka metni okunamadı"

        return True, license_plate_text

    except Exception as e:
        logger.error(f"Plaka tanıma hatası: {str(e)}")
        return False, f"Hata: {str(e)}"


def debug_plate_recognition(image_data, save_debug_images=True):
    """Plaka tanıma sürecini görselleştirip debug eden fonksiyon"""
    try:
        # Görüntüyü yükle
        with tempfile.NamedTemporaryFile(delete=False, suffix='.jpg') as temp_file:
            temp_file.write(image_data)
            temp_file_path = temp_file.name

        image = cv2.imread(temp_file_path)
        os.unlink(temp_file_path)

        if image is None:
            return False, "Görüntü okunamadı", None

        debug_images = {}
        debug_images['original'] = image.copy()

        # Temel ön işleme
        gray, blurred, edges = preprocess_image(image)
        debug_images['gray'] = gray
        debug_images['blurred'] = blurred
        debug_images['edges'] = edges
        
        # Gelişmiş ön işleme - Türk plaka algılama için
        enhanced_images = enhanced_preprocessing(image)
        # Tüm sonuçları debug_images sözlüğüne ekle
        for name, img in enhanced_images.items():
            debug_images[f'enhanced_{name}'] = img

        # 1. Yöntem: Standart kontur tabanlı plaka algılama
        contours, _ = cv2.findContours(edges, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
        contours = sorted(contours, key=cv2.contourArea, reverse=True)[:10]

        contour_image = image.copy()
        cv2.drawContours(contour_image, contours, -1, (0, 255, 0), 3)
        debug_images['contours'] = contour_image

        # Plaka konturunu bul
        license_plate_contour = find_license_plate_contour(contours, image.shape)

        # 2. Yöntem: Türk plakalarına özel algılama
        if license_plate_contour is None:
            logger.info("Standart yöntemle plaka bulunamadı, Türk plaka algılama deneniyor")
            turkish_plate_box = turkish_plate_detector(image)
            
            if turkish_plate_box is not None:
                license_plate_contour = turkish_plate_box
                
                # Debug için Türk plaka algılama sonucunu göster
                turkish_plate_img = image.copy()
                cv2.drawContours(turkish_plate_img, [turkish_plate_box], -1, (0, 0, 255), 3)
                debug_images['turkish_plate_detection'] = turkish_plate_img
                
                logger.info("Türk plaka algılama algoritması ile plaka bulundu")

        # 3. Yöntem: HOG tabanlı algılama 
        if license_plate_contour is None:
            logger.info("Türk plaka algılama ile plaka bulunamadı, HOG tabanlı algılama deneniyor")
            hog_contours = alternative_plate_detection(image)
            license_plate_contour = find_license_plate_contour(hog_contours, image.shape)
            
            if license_plate_contour is not None:
                # Debug için HOG sonucunu göster
                hog_plate_img = image.copy()
                cv2.drawContours(hog_plate_img, [license_plate_contour], -1, (255, 0, 0), 3)
                debug_images['hog_plate_detection'] = hog_plate_img

        # Hiçbir kontur bulunamadıysa doğrudan OCR dene
        if license_plate_contour is None:
            ocr_results = []
            
            # Farklı ön işleme sonuçlarında OCR dene
            for name, img in debug_images.items():
                if name == 'original' or 'contours' in name or 'plate_detection' in name:
                    continue

                # Görüntü gri tonlama değilse dönüştür
                if len(img.shape) == 3:
                    img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

                text = perform_ocr(img)
                clean_text = clean_plate_text(text)

                if clean_text:
                    ocr_results.append((name, text, clean_text))
            
            # En iyi OCR sonucunu seç
            if ocr_results:
                # Doğru formatta olan sonucu seç (ilk öncelikli)
                valid_format_results = []
                for name, text, clean_text in ocr_results:
                    if re.match(r'^\d{2}\s[A-Z]{1,3}\s\d{1,4}$', clean_text):
                        valid_format_results.append((name, text, clean_text))
                
                # Doğru formatta sonuç varsa onu kullan, yoksa en uzun sonucu kullan
                selected_result = valid_format_results[0] if valid_format_results else max(ocr_results, key=lambda x: len(x[2]))
                
                name, text, clean_text = selected_result
                
                if save_debug_images:
                    # Debug klasörünü oluştur
                    os.makedirs('debug_images', exist_ok=True)
                    timestamp = int(time.time())

                    # Görüntüleri kaydet
                    for dbg_name, dbg_img in debug_images.items():
                        cv2.imwrite(f'debug_images/{timestamp}_{dbg_name}.jpg', dbg_img)

                    # Bulunan metni kaydet
                    with open(f'debug_images/{timestamp}_result.txt', 'w') as f:
                        f.write(f"Method: direct OCR on {name}\nText: {text}\nClean: {clean_text}")
                        f.write("\n\nTüm OCR sonuçları:\n")
                        for r_name, r_text, r_clean_text in ocr_results:
                            f.write(f"{r_name}: {r_text} -> {r_clean_text}\n")

                return True, clean_text, debug_images

            if save_debug_images:
                # Debug klasörünü oluştur
                os.makedirs('debug_images', exist_ok=True)
                timestamp = int(time.time())

                # Görüntüleri kaydet
                for dbg_name, dbg_img in debug_images.items():
                    cv2.imwrite(f'debug_images/{timestamp}_{dbg_name}.jpg', dbg_img)

                # Sonucu kaydet
                with open(f'debug_images/{timestamp}_result.txt', 'w') as f:
                    f.write("No plate detected with any method")

            return False, "Plaka bulunamadı", debug_images

        # Plaka konturunu göster
        plate_contour_image = image.copy()
        cv2.drawContours(plate_contour_image, [license_plate_contour], -1, (0, 0, 255), 3)
        debug_images['plate_contour'] = plate_contour_image

        # Plaka bölgesini çıkar
        x, y, w, h = cv2.boundingRect(license_plate_contour)
        plate_region = gray[y:y+h, x:x+w]
        debug_images['plate_region_gray'] = plate_region
        
        # Farklı işlenmiş versiyonlardaki plaka bölgelerini de ekle
        plate_regions = {}
        for name in ['enhanced', 'sharpened', 'adaptive', 'equalized', 'otsu']:
            if name in enhanced_images:
                plate_region = enhanced_images[name][y:y+h, x:x+w]
                plate_regions[name] = plate_region
                debug_images[f'plate_region_{name}'] = plate_region

        # OCR sonuçlarını topla
        ocr_results = []
        
        # Gri tonlamada OCR
        text_gray = perform_ocr(plate_region)
        clean_text_gray = clean_plate_text(text_gray)
        if clean_text_gray:
            ocr_results.append(("gray", text_gray, clean_text_gray))
        
        # Diğer işlenmiş görüntülerde OCR
        for name, region in plate_regions.items():
            text = perform_ocr(region)
            clean_text = clean_plate_text(text)
            if clean_text:
                ocr_results.append((name, text, clean_text))
        
        # En iyi OCR sonucunu seç
        if ocr_results:
            # Doğru formatta olan sonucu seç (ilk öncelikli)
            valid_format_results = []
            for name, text, clean_text in ocr_results:
                if re.match(r'^\d{2}\s[A-Z]{1,3}\s\d{1,4}$', clean_text):
                    valid_format_results.append((name, text, clean_text))
            
            # Doğru formatta sonuç varsa onu kullan, yoksa en uzun sonucu kullan
            selected_result = valid_format_results[0] if valid_format_results else max(ocr_results, key=lambda x: len(x[2]))
            
            name, text, clean_text = selected_result
            debug_images['successful_region'] = debug_images[f'plate_region_{name}']
            debug_images['successful_method'] = name
            
            if save_debug_images:
                # Debug klasörünü oluştur
                os.makedirs('debug_images', exist_ok=True)
                timestamp = int(time.time())

                # Görüntüleri kaydet
                for dbg_name, dbg_img in debug_images.items():
                    if isinstance(dbg_img, str):  # Eğer değer bir string ise (yöntem adı gibi)
                        continue
                    cv2.imwrite(f'debug_images/{timestamp}_{dbg_name}.jpg', dbg_img)

                # Sonucu kaydet
                with open(f'debug_images/{timestamp}_result.txt', 'w') as f:
                    f.write(f"Selected Method: {name}\nText: {text}\nClean: {clean_text}\n")
                    f.write("\nAll OCR results:\n")
                    for r_name, r_text, r_clean_text in ocr_results:
                        f.write(f"{r_name}: {r_text} -> {r_clean_text}\n")

            return True, clean_text, debug_images
        else:
            if save_debug_images:
                # Debug klasörünü oluştur
                os.makedirs('debug_images', exist_ok=True)
                timestamp = int(time.time())

                # Görüntüleri kaydet
                for dbg_name, dbg_img in debug_images.items():
                    if isinstance(dbg_img, str):  # Eğer değer bir string ise (yöntem adı gibi)
                        continue
                    cv2.imwrite(f'debug_images/{timestamp}_{dbg_name}.jpg', dbg_img)

                # Sonucu kaydet
                with open(f'debug_images/{timestamp}_result.txt', 'w') as f:
                    f.write("Plaka metni okunamadı\n")
                    f.write("Hiçbir OCR yöntemi plaka metni çıkaramadı")

            return False, "Plaka metni okunamadı", debug_images

    except Exception as e:
        logger.error(f"Debug fonksiyonunda hata: {str(e)}")
        return False, f"Hata: {str(e)}", None
    
    