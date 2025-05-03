from ultralytics import YOLO
import cv2
import torch
import os
import numpy as np
import time
import logging
from PIL import Image

# Loglama yapılandırması
logger = logging.getLogger(__name__)

# PIL/Pillow ANTIALIAS/LANCZOS sorunu düzeltme
# EasyOCR kütüphanesi eski PIL.Image.ANTIALIAS'ı kullanıyor, bu yeni sürümlerde bulunmuyor
try:
    # Pillow 9.0.0 ve üzeri sürümlerde ANTIALIAS yerine LANCZOS kullanılmalı
    original_antialias = getattr(Image, "ANTIALIAS", None)
    if original_antialias is None:
        # Monkey patch - ANTIALIAS için yerine LANCZOS kullan
        setattr(Image, "ANTIALIAS", Image.LANCZOS)
        logger.info("PIL.Image.ANTIALIAS monkey-patch başarıyla uygulandı (LANCZOS kullanılacak)")
except Exception as e:
    logger.warning(f"PIL.Image.ANTIALIAS monkey-patch uygulanamadı: {str(e)}")

# Pillow sürüm uyumluluğu için genel değişken
try:
    RESIZE_MODE = Image.LANCZOS  # Yeni PIL sürümleri için
    logger.info("RESIZE_MODE: LANCZOS kullanılıyor")
except AttributeError:
    RESIZE_MODE = Image.ANTIALIAS  # Eski PIL sürümleri için
    logger.info("RESIZE_MODE: ANTIALIAS kullanılıyor")

# Şimdi EasyOCR'ı ANTIALIAS düzeltmesinden sonra import et
import easyocr  # Alternatif OCR için

def load_torch_model(model_path, device='cpu'):
    """Güvenli model yükleme fonksiyonu"""
    try:
        return torch.load(model_path, map_location=device)
    except Exception as e:
        logger.error(f"Model yüklenirken hata: {e}")
        return None

# Model ve util dosyalarını import et
from . import util
from app.model.sort.sort import *
from .util import get_car, read_license_plate, write_csv

# Debug klasörü oluştur
debug_dir = "./debug_plates"
os.makedirs(debug_dir, exist_ok=True)

# Basit YOLO çağrısı için sınıf
class SimpleYOLO:
    """Basit YOLO modeli sarmalayıcı sınıfı"""
    def __init__(self, model_path):
        try:
            from ultralytics import YOLO
            logger.info(f"SimpleYOLO model yükleniyor: {model_path}")
            self.model = YOLO(model_path)
            logger.info(f"SimpleYOLO model yüklendi: {model_path}")
            self.error = None
        except Exception as e:
            logger.error(f"SimpleYOLO model yüklenemedi: {e}")
            self.model = None
            self.error = str(e)
    
    def __call__(self, image):
        """Görüntü üzerinde çıkarım yap ve sonuçları döndür"""
        try:
            if self.model is None:
                logger.warning("Model yüklenemedi, test sonuçları kullanılıyor")
                return MockResults()
            logger.info("SimpleYOLO model çalıştırılıyor")
            return self.model(image)
        except Exception as e:
            logger.error(f"Model çalıştırma hatası: {e}")
            return MockResults()

# Gerçek modellerin ve mock modellerin tanımı
class MockDetector:
    """Model yüklenemediğinde kullanılacak mock sınıf"""
    def __init__(self, name=None):
        if name:
            logger.info(f"Test detector '{name}' oluşturuldu")
        else:
            logger.info("Mock detector initialized")
    
    def __call__(self, image):
        if isinstance(image, str):
            logger.info(f"Test detection on image path: {image}")
        else:
            logger.info(f"Test detector çalıştırılıyor")
        # Boş sonuç döndür
        return [MockResults()]

class MockResults:
    def __init__(self):
        self.boxes = MockBoxes()

class MockBoxes:
    def __init__(self):
        self.data = np.zeros((0, 6))  # Boş sonuç veri yapısı

# SORT takip sistemi
mot_tracker = Sort()

# Model dosyalarını yüklemeyi dene
current_dir = os.path.dirname(os.path.abspath(__file__))
try:
    logger.info("YOLO modellerini yüklemeye başlıyor...")
    
    # Yolların doğru olduğunu kontrol et
    coco_model_path = os.path.join(current_dir, 'yolov8n.pt')
    license_model_path = os.path.join(current_dir, 'license_plate_detector.pt')
    
    logger.info(f"COCO model dosyası mevcut mu: {os.path.exists(coco_model_path)}")
    logger.info(f"Plaka model dosyası mevcut mu: {os.path.exists(license_model_path)}")
    
    # Modelleri yükle
    coco_model = SimpleYOLO(coco_model_path)
    license_plate_detector = SimpleYOLO(license_model_path)
    
    # Hata kontrolü
    if coco_model.error is None and license_plate_detector.error is None:
        logger.info("YOLO modelleri başarıyla yüklendi")
        USE_REAL_MODEL = True
    else:
        logger.warning("YOLO modelleri yüklenemedi, test modları kullanılacak")
        coco_model = MockDetector("coco")
        license_plate_detector = MockDetector("license_plate")
        logger.info("YOLO modelleri (test modelleri) başarıyla yüklendi")
        USE_REAL_MODEL = False
except Exception as e:
    logger.error(f"YOLO modelleri yüklenirken hata: {str(e)}")
    # Hata durumunda mock modelleri kullan
    coco_model = MockDetector("coco")  
    license_plate_detector = MockDetector("license_plate")
    logger.info("YOLO modelleri (test modelleri) başarıyla yüklendi")
    USE_REAL_MODEL = False

# EasyOCR okuyucusu
try:
    logger.info("EasyOCR yükleniyor...")
    reader = easyocr.Reader(['en', 'tr'])  # Türkçe ve İngilizce
    logger.info("EasyOCR başarıyla yüklendi")
except Exception as e:
    logger.error(f"EasyOCR yüklenemedi: {str(e)}")
    reader = None

# Alternatif plaka okuma fonksiyonu
def read_license_plate_enhanced(img):
    """
    Geliştirilmiş plaka okuma fonksiyonu,
    birden fazla görüntü işleme tekniği ve OCR uygular
    """
    if reader is None:
        logger.warning("EasyOCR yüklü değil, plaka okunamıyor")
        return None, 0
    
    try:
        # Orjinal görüntüyü kaydet
        timestamp = int(time.time())
        orig_path = f"{debug_dir}/plate_orig_{timestamp}.jpg"
        cv2.imwrite(orig_path, img)
        
        # Metot 1: Normal OCR (gri tonlama)
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY) if len(img.shape) == 3 else img
        gray_path = f"{debug_dir}/plate_gray_{timestamp}.jpg"
        cv2.imwrite(gray_path, gray)
        
        # Metot 2: Threshold negatif
        _, thresh_neg = cv2.threshold(gray, 64, 255, cv2.THRESH_BINARY_INV)
        thresh_neg_path = f"{debug_dir}/plate_thresh_neg_{timestamp}.jpg"
        cv2.imwrite(thresh_neg_path, thresh_neg)
        
        # Metot 3: Threshold pozitif
        _, thresh_pos = cv2.threshold(gray, 120, 255, cv2.THRESH_BINARY)
        thresh_pos_path = f"{debug_dir}/plate_thresh_pos_{timestamp}.jpg"
        cv2.imwrite(thresh_pos_path, thresh_pos)
        
        # Metot 4: Adaptif threshold
        adaptive = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2)
        adaptive_path = f"{debug_dir}/plate_adaptive_{timestamp}.jpg"
        cv2.imwrite(adaptive_path, adaptive)
        
        # Metot 5: Eşitleme ve blur
        equalized = cv2.equalizeHist(gray)
        blurred = cv2.GaussianBlur(equalized, (5, 5), 0)
        equalized_path = f"{debug_dir}/plate_equalized_{timestamp}.jpg"
        cv2.imwrite(equalized_path, blurred)
        
        # Mevcut OCR ile dene
        text_old, score_old = read_license_plate(thresh_neg)
        logger.info(f"  -> Mevcut OCR sonucu: {text_old}, Güven: {score_old if text_old else 'Okunamadı'}")
        
        # Tüm tespit edilen metinleri topla
        all_detected_texts = []
        
        # EasyOCR ile farklı görüntü işleme yöntemlerini dene
        for method_name, img_proc in [
            ("gray", gray),
            ("thresh_neg", thresh_neg),
            ("thresh_pos", thresh_pos),
            ("adaptive", adaptive),
            ("equalized", equalized)
        ]:
            try:
                # EasyOCR ile oku
                results_ocr = reader.readtext(img_proc)
                
                if results_ocr:
                    for (bbox, text, confidence) in results_ocr:
                        logger.info(f"  -> EasyOCR ({method_name}): {text}, Güven: {confidence:.2f}")
                        
                        # Temizlenmiş metni ekle
                        clean_text = ''.join(c for c in text if c.isalnum()).upper()
                        if clean_text and confidence > 0.3:  # Minimum güven skoru
                            # X koordinatına göre sıralayarak metinlerin pozisyonunu kaydediyoruz
                            left, top = bbox[0]
                            all_detected_texts.append((clean_text, confidence, left))
            except Exception as e:
                logger.error(f"  -> OCR hatası ({method_name}): {str(e)}")
        
        # Tespit edilen metinleri pozisyona göre sırala (soldan sağa)
        all_detected_texts.sort(key=lambda x: x[2])
        
        logger.info(f"  -> Tespit edilen tüm metinler (soldan sağa): {[t[0] for t in all_detected_texts]}")
        
        # Türk plaka formatına uygunluk için regex kontrolleri
        import re
        
        # Olası plaka kombinasyonlarını dene
        possible_plate_formats = []
        
        # Tüm tespit edilen metinleri birleştirerek kombinasyonlar oluştur
        # Pozisyona göre sıralanmış metinleri kullan
        if all_detected_texts:
            # Metinleri birleştir
            combined_text = ''.join([t[0] for t in all_detected_texts])
            combined_confidence = sum([t[1] for t in all_detected_texts]) / len(all_detected_texts)
            possible_plate_formats.append((combined_text, combined_confidence))
            
            # İlk iki karakter il kodu mu kontrol et
            if len(combined_text) >= 2 and combined_text[:2].isdigit() and 1 <= int(combined_text[:2]) <= 81:
                possible_plate_formats.append((combined_text, combined_confidence))
        
        # Boşlukları temizleyen ve formatı kontrol eden fonksiyon
        def is_turkish_plate_format(text):
            # Türk plaka formatı regex deseni:
            # 1-2 rakam (il kodu), 1-3 harf, 2-4 rakam
            # Örnek: 06AKP37, 34AB123, 07A1234
            pattern = r'^(0?[1-9]|[1-7][0-9]|8[0-1])([A-Z]{1,3})([0-9]{2,4})$'
            return bool(re.match(pattern, text))
        
        # Plakanın parçalanmış olabileceğini düşünerek birleştirme denemeleri yap
        for i in range(len(all_detected_texts)):
            base_text = all_detected_texts[i][0]
            base_conf = all_detected_texts[i][1]
            
            # Tek başına olan kısım plaka olabilir mi?
            if is_turkish_plate_format(base_text):
                possible_plate_formats.append((base_text, base_conf))
            
            # İki parçayı birleştir
            for j in range(i+1, len(all_detected_texts)):
                combined = base_text + all_detected_texts[j][0]
                avg_conf = (base_conf + all_detected_texts[j][1]) / 2
                if is_turkish_plate_format(combined):
                    possible_plate_formats.append((combined, avg_conf))
            
            # Üç parçayı birleştir
            if i+1 < len(all_detected_texts) and i+2 < len(all_detected_texts):
                j = i+1
                k = i+2
                three_combined = base_text + all_detected_texts[j][0] + all_detected_texts[k][0]
                avg_conf_3 = (base_conf + all_detected_texts[j][1] + all_detected_texts[k][1]) / 3
                if is_turkish_plate_format(three_combined):
                    possible_plate_formats.append((three_combined, avg_conf_3))
        
        # Türk plaka formatına manuel düzeltmeler uygula
        for text, conf in list(possible_plate_formats):
            # Tüm parçaları alıp, rakam ve harf gruplarını ayırmaya çalış
            digits_prefix = ""
            letters = ""
            digits_suffix = ""
            
            # İlk kısımda il kodunu bul (1-2 rakam)
            match = re.match(r'^(\d{1,2})(.*)$', text)
            if match:
                digits_prefix, rest = match.groups()
                
                # İkinci kısımda harfleri bul (1-3 harf)
                match = re.match(r'^([A-Z]{1,3})(.*)$', rest)
                if match:
                    letters, digits_suffix = match.groups()
                    
                    # Son kısım rakamlardan oluşuyor mu?
                    if digits_suffix.isdigit():
                        formatted_plate = f"{digits_prefix}{letters}{digits_suffix}"
                        possible_plate_formats.append((formatted_plate, conf))
        
        # Plaka formatına uygun sonuçları filtrele
        valid_plates = [(text, conf) for text, conf in possible_plate_formats if is_turkish_plate_format(text)]
        
        logger.info(f"  -> Olası plaka formatları: {valid_plates}")
        
        # En yüksek güven skoruna sahip olanı seç
        if valid_plates:
            valid_plates.sort(key=lambda x: x[1], reverse=True)
            best_text, best_score = valid_plates[0]
            logger.info(f"  -> En iyi plaka sonucu: {best_text}, Güven: {best_score:.2f}")
            return best_text, best_score
        
        # Eğer hiçbir geçerli format bulunamadıysa, en yüksek güvenli sonucu döndür
        if all_detected_texts:
            all_detected_texts.sort(key=lambda x: x[1], reverse=True)
            best_text, best_score, _ = all_detected_texts[0]
            
            # Son bir çözüm olarak, bazı manuel düzeltmeler yap
            if len(best_text) >= 5:  # Minimum plaka uzunluğu
                # 0 ile O, 1 ile I gibi karışabilecek karakterleri düzelt
                corrected_text = best_text.replace('O', '0').replace('I', '1').replace('S', '5').replace('G', '6')
                return corrected_text, best_score
            
            return best_text, best_score
        
        return text_old, score_old  # Mevcut OCR sonucu döndür
    except Exception as e:
        logger.error(f"Plaka okumada beklenmeyen hata: {str(e)}")
        return None, 0

# Görüntü işleme fonksiyonu (test/örnek amaçlı)
def process_test_image(image_path):
    """Test amaçlı bir görüntüyü işler ve sonuçları döndürür"""
    results = {}
    frame_nmr = 0
    results[frame_nmr] = {}
    
    # Görüntüyü okuma
    logger.info(f"Görüntü dosyası yükleniyor: {image_path}")
    logger.info(f"Görüntü dosyası mevcut mu: {os.path.exists(image_path)}")
    
    try:
        frame = cv2.imread(image_path)
        if frame is None:
            logger.warning(f"UYARI: Görüntü yüklenemedi: {image_path}")
            # Boş bir frame oluştur (örnek için)
            frame = np.zeros((300, 300, 3), dtype=np.uint8)
            logger.info("Boş bir görüntü oluşturuldu")
        else:
            logger.info(f"Görüntü yüklendi, boyut: {frame.shape}")
    except Exception as e:
        logger.error(f"Görüntü yükleme hatası: {str(e)}")
        frame = np.zeros((300, 300, 3), dtype=np.uint8)
        logger.info("Hata nedeniyle boş bir görüntü oluşturuldu")

    # İşlem sonucunu ve görüntüyü döndür
    return frame, results

# Modül doğrudan çalıştırılırsa test işlemi yap
if __name__ == "__main__":
    # Test için varsayılan görüntüyü kullan
    test_image_path = os.path.join(current_dir, 'debug_images', 'sample.jpg')
    if not os.path.exists(test_image_path):
        test_image_path = os.path.join(os.path.dirname(current_dir), 'debug_images', 'sample.jpg')
    
    # Test görüntüsünü işle
    frame, results = process_test_image(test_image_path)
    
    # Sonuçları göster
    logger.info(f"Test işlemi tamamlandı. Sonuçlar: {results}")