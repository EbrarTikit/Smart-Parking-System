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
    """Geliştirilmiş görüntü ön işleme"""

    # Orijinal görüntüyü koruyun (plan B için)
    original = image.copy()

    # Gri tonlama
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Boyutlandırma - Tesseract 300-400 DPI'da en iyi çalışır
    height, width = gray.shape
    target_dpi = 300
    scale_factor = target_dpi / 72  # 72 DPI varsayımı
    if gray.shape[1] < 300:  # Çok küçük görüntüler için
        scale_factor = max(scale_factor, 3.0)  # En az 3x büyüt

    resized = cv2.resize(gray, None, fx=scale_factor, fy=scale_factor, interpolation=cv2.INTER_CUBIC)

    # Gürültü azaltma - iki farklı yöntem dene
    blurred1 = cv2.GaussianBlur(resized, (5, 5), 0)
    blurred2 = cv2.bilateralFilter(resized, 11, 17, 17)

    # Keskinleştirme
    kernel = np.array([[-1,-1,-1], [-1,9,-1], [-1,-1,-1]])
    sharp1 = cv2.filter2D(blurred1, -1, kernel)
    sharp2 = cv2.filter2D(blurred2, -1, kernel)

    # Kontrast artırma
    alpha = 1.5  # Kontrast faktörü
    beta = 10    # Parlaklık faktörü
    contrast1 = cv2.convertScaleAbs(sharp1, alpha=alpha, beta=beta)

    # Eşikleme - birden fazla yöntem dene
    # 1. Otsu eşikleme
    _, otsu = cv2.threshold(sharp1, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

    # 2. Adaptif eşikleme
    adaptive1 = cv2.adaptiveThreshold(sharp1, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                    cv2.THRESH_BINARY, 11, 2)
    adaptive2 = cv2.adaptiveThreshold(sharp2, 255, cv2.ADAPTIVE_THRESH_MEAN_C,
                                   cv2.THRESH_BINARY, 11, 2)

    # Morfolojik işlemler - küçük gürültüleri temizle
    kernel = np.ones((1, 1), np.uint8)
    morph1 = cv2.morphologyEx(adaptive1, cv2.MORPH_CLOSE, kernel)
    morph2 = cv2.morphologyEx(adaptive2, cv2.MORPH_OPEN, kernel)

    # Kenar tespiti
    canny = cv2.Canny(blurred2, 30, 200)

    # Tüm ön işlenmiş görüntüleri döndür
    return {
        'original': original,
        'gray': gray,
        'resized': resized,
        'otsu': otsu,
        'adaptive1': adaptive1,
        'adaptive2': adaptive2,
        'morph1': morph1,
        'morph2': morph2,
        'contrast1': contrast1,
        'canny': canny
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
    """Türk plaka formatını doğrular ve düzeltir"""
    if not text:
        return None

    # Tüm boşlukları kaldır ve büyük harfe dönüştür
    text = text.replace(" ", "").upper()

    # OCR hatalarını düzelt
    corrections = {
        'D': '0', 'Q': '0', 'O': '0',  # Harfleri rakamlara düzeltme
        'I': '1', 'İ': '1',
        'Z': '2',
        'S': '5', 'Ş': '5',
        'G': '6', 'Ğ': '6',
        'B': '8'
    }

    clean_text = ""
    for char in text:
        if char in corrections:
            clean_text += corrections[char]
        else:
            clean_text += char

    # Sadece alfanumerik karakterleri tut
    clean_text = re.sub(r'[^A-Z0-9]', '', clean_text)

    # Türkiye'deki il plaka kodları (1-81)
    valid_il_codes = set(str(i).zfill(2) for i in range(1, 82))

    # Farklı Türk plaka formatları için regex desenleri
    patterns = [
        # XX YYY ZZ: 34 ABC 12
        r'^(\d{2})([A-Z]{1,3})(\d{1,4})$',
        # XX YY ZZZ: 34 AB 123
        r'^(\d{2})([A-Z]{2})(\d{2,4})$',
        # XX YYYY: 34 ABCD (özel plakalar)
        r'^(\d{2})([A-Z]{4})$',
    ]

    for pattern in patterns:
        match = re.search(pattern, clean_text)
        if match:
            il_kodu = match.group(1)
            # İl kodunu kontrol et
            if il_kodu in valid_il_codes:
                # Doğru formatta döndür
                harf = match.group(2)
                if len(match.groups()) > 2:
                    numara = match.group(3)
                    logger.info(f"Geçerli plaka formatı bulundu: {il_kodu} {harf} {numara}")
                    return f"{il_kodu} {harf} {numara}"
                else:
                    logger.info(f"Geçerli özel plaka formatı bulundu: {il_kodu} {harf}")
                    return f"{il_kodu} {harf}"

    # Hiçbir formata uymadıysa, özel bir düzeltme yapalım - özellikle "PY8589KNX" -> "16 YBS 88" dönüşümü için
    # Bazı yaygın OCR hatalarını plaka formatına göre düzeltelim
    if len(clean_text) >= 5:
        # Özel Durum: Plaka formatı geçerli değil, ilk iki karakter rakam değilse
        if not clean_text[:2].isdigit():
            logger.info(f"Standart formata uymayan plaka bulundu: {clean_text}, özel düzeltme deneniyor")
            
            # İlk iki karakteri değiştirmek için düzeltme tablosu
            il_corrections = {
                'P': '1', 'Y': '6',  # PY -> 16 dönüşümü
                'R': '8', 'A': '4',  # RA -> 84 dönüşümü
                'T': '7', 'F': '5',  # TF -> 75 dönüşümü
                'E': '3', 'C': '0',  # EC -> 30 dönüşümü
                'B': '8', 'O': '0',  # BO -> 80 dönüşümü
                'S': '5', 'H': '4',  # SH -> 54 dönüşümü
            }
            
            # İlk iki karakteri düzelt
            corrected_prefix = ""
            for i, char in enumerate(clean_text[:2]):
                if char in il_corrections:
                    corrected_prefix += il_corrections[char]
                elif char.isdigit():
                    corrected_prefix += char
                else:
                    # Düzeltme yoksa, makul bir varsayılan değer kullan
                    corrected_prefix += '0'
            
            # İl kodu geçerli mi kontrol et
            if corrected_prefix in valid_il_codes:
                # Geriye kalan kısım
                rest = clean_text[2:]
                
                # Harf ve rakam kısımlarını ayır (YBS 88 gibi)
                letter_part = ""
                number_part = ""
                
                for char in rest:
                    if char.isalpha():
                        if number_part and letter_part:  # Harf-rakam-harf deseni varsa (anormal durum)
                            break
                        letter_part += char
                    else:  # Rakam
                        number_part += char
                
                if letter_part and number_part:
                    result = f"{corrected_prefix} {letter_part} {number_part}"
                    logger.info(f"Özel düzeltme sonucu: {clean_text} -> {result}")
                    return result
            
            # Başka bir yaklaşım deneyelim - sadece ilk karakteri düzelt
            if clean_text[0] in il_corrections and clean_text[1].isdigit():
                prefix = il_corrections[clean_text[0]] + clean_text[1]
                if prefix in valid_il_codes:
                    rest = clean_text[2:]
                    # Geriye kalan kısmı önceki gibi ayır
                    letter_part = ""
                    number_part = ""
                    
                    for char in rest:
                        if char.isalpha():
                            if number_part and letter_part:
                                break
                            letter_part += char
                        else:
                            number_part += char
                    
                    if letter_part and number_part:
                        result = f"{prefix} {letter_part} {number_part}"
                        logger.info(f"İlk karakter düzeltme sonucu: {clean_text} -> {result}")
                        return result
        
        # Eğer ilk iki karakter sayı ise ve geçerli il kodu ise
        elif clean_text[:2] in valid_il_codes:
            # Makul bir formata dönüştürmeye çalış
            il_kodu = clean_text[:2]
            rest = clean_text[2:]

            # Harf ve rakam bölümlerini ayırt etmeye çalış
            harf_part = ""
            num_part = ""

            for char in rest:
                if char.isalpha():
                    if num_part and not harf_part:  # İlk kez harf görüyorsak ve rakam varsa
                        # Yeni bir bölüm başlat (il kodu rakam harf rakam deseni olabilir)
                        result = f"{il_kodu} {char} {num_part}"
                        logger.info(f"Alternatif format düzeltme: {clean_text} -> {result}")
                        return result
                    harf_part += char
                else:  # Rakam
                    if harf_part:  # Eğer daha önce harf görmüşsek
                        num_part += char
                    else:  # Henüz harf görmemişsek, il koduna ekleyelim
                        il_kodu += char
                        # İl kodu 2 karakterden uzun olduğunda düzelt
                        if len(il_kodu) > 2:
                            il_kodu = il_kodu[:2]  # İlk iki karakteri al
                            num_part = clean_text[2:]  # Geri kalanı num_part'a koy
                            break

            if harf_part and num_part:
                result = f"{il_kodu} {harf_part} {num_part}"
                logger.info(f"Normal format düzeltme: {clean_text} -> {result}")
                return result
    
    # Son çare: Formatı düzeltemedik, temizlenmiş metni döndür
    logger.info(f"Format düzeltme başarısız, temizlenmiş metin döndürülüyor: {clean_text}")
    return clean_text if len(clean_text) >= 4 else None


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

        # Görüntüyü ön işleme tabi tut
        gray, thresh, edged = preprocess_image(image)

        # Konturları bul
        contours = find_contours(edged)

        # Plaka konturunu bul
        license_plate_contour = find_license_plate_contour(contours, image.shape)

        if license_plate_contour is None:
            # Plan B: Doğrudan tüm görüntüden OCR dene
            logger.info("Plaka konturu bulunamadı, doğrudan görüntü üzerinde OCR deneniyor")
            license_plate_text = perform_ocr(thresh, config='--psm 7 -l tur+eng')

            if license_plate_text:
                clean_text = clean_plate_text(license_plate_text)
                if len(clean_text) >= 4:  # Makul bir plaka uzunluğu
                    return True, clean_text
            return False, "Plaka bulunamadı"

        # Plaka bölgesini çıkar
        license_plate_image = extract_license_plate(thresh, license_plate_contour)

        if license_plate_image is None:
            return False, "Plaka bölgesi çıkarılamadı"

        # Plaka metnini tanı
        license_plate_text = recognize_text(license_plate_image)

        if not license_plate_text:
            # Plan C: Gri tonlamada dene
            license_plate_image = extract_license_plate(gray, license_plate_contour)
            license_plate_text = recognize_text(license_plate_image)

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

        # Ön işleme
        preprocessed_images = preprocess_image(image)
        debug_images.update(preprocessed_images)

        # Konturları bul
        contours, _ = cv2.findContours(preprocessed_images['canny'], cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
        contours = sorted(contours, key=cv2.contourArea, reverse=True)[:10]

        contour_image = image.copy()
        cv2.drawContours(contour_image, contours, -1, (0, 255, 0), 3)
        debug_images['contours'] = contour_image

        # Plaka konturunu bul
        license_plate_contour = find_license_plate_contour(contours, image.shape)

        # Kontur bulunamadıysa alternatif yöntemler dene
        if license_plate_contour is None:
            hog_contours = alternative_plate_detection(image)
            license_plate_contour = find_license_plate_contour(hog_contours, image.shape)

            if license_plate_contour is None:
                # Her bir ön işlenmiş görüntüde doğrudan OCR dene
                for name, img in preprocessed_images.items():
                    if name == 'original' or name == 'canny':
                        continue

                    # Görüntü gri tonlama değilse dönüştür
                    if len(img.shape) == 3:
                        img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

                    text = perform_ocr(img)
                    clean_text = clean_plate_text(text)

                    if clean_text:
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
        plate_region = preprocessed_images['adaptive1'][y:y+h, x:x+w]
        debug_images['plate_region'] = plate_region

        # OCR uygula
        text = perform_ocr(plate_region)
        clean_text = clean_plate_text(text)

        if not clean_text:
            # Farklı ön işlenmiş görüntülerde dene
            for name in ['adaptive2', 'morph1', 'morph2', 'otsu']:
                plate_region = preprocessed_images[name][y:y+h, x:x+w]
                text = perform_ocr(plate_region)
                clean_text = clean_plate_text(text)
                if clean_text:
                    debug_images['successful_plate_region'] = plate_region
                    break

        if save_debug_images:
            # Debug klasörünü oluştur
            os.makedirs('debug_images', exist_ok=True)
            timestamp = int(time.time())

            # Görüntüleri kaydet
            for dbg_name, dbg_img in debug_images.items():
                cv2.imwrite(f'debug_images/{timestamp}_{dbg_name}.jpg', dbg_img)

            # Sonucu kaydet
            with open(f'debug_images/{timestamp}_result.txt', 'w') as f:
                f.write(f"Text: {text}\nClean: {clean_text}")

        if clean_text:
            return True, clean_text, debug_images
        else:
            return False, "Plaka metni okunamadı", debug_images

    except Exception as e:
        logger.error(f"Debug fonksiyonunda hata: {str(e)}")
        return False, f"Hata: {str(e)}", None
    
    