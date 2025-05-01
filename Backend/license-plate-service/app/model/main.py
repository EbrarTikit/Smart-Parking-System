from ultralytics import YOLO
import cv2
import torch
from ultralytics.nn.tasks import DetectionModel
import torch.nn as nn
from ultralytics.nn.modules import Conv
from ultralytics.nn import tasks

# Orjinal torch_safe_load fonksiyonunu yedekleyelim
original_torch_safe_load = tasks.torch_safe_load

# Yeni güvenli yükleme fonksiyonu ile değiştirelim
def safe_torch_load(file):
    if isinstance(file, str) and file.endswith('.pt'):
        # Güvenilir kaynaktan gelen modeller için weights_only=False
        return torch.load(file, map_location='cpu', weights_only=False), file
    else:
        # Diğer durumlar için orijinal fonksiyonu kullanalım
        return original_torch_safe_load(file)

# Monkey patching - orijinal fonksiyonu değiştiriyoruz
tasks.torch_safe_load = safe_torch_load

# Artık bu sınıfları eklemeye gerek kalmadı
# torch.serialization.add_safe_globals([
#     DetectionModel, 
#     nn.modules.container.Sequential,
#     Conv,
#     nn.Conv2d,
#     nn.Linear,
#     nn.BatchNorm2d,
#     nn.ReLU
# ])

import util
from sort.sort import *
from util import get_car, read_license_plate, write_csv
import numpy as np
import os
import easyocr  # Alternatif OCR için
import time

# Debug klasörü oluştur
debug_dir = "./debug_plates"
os.makedirs(debug_dir, exist_ok=True)

# Resim dosyasının tam yolu
image_path = 'C:/Users/Selin/Desktop/Smart-Parking-System/Backend/license-plate-service/debug_images/image15.jpg'
print(f"Görüntü dosyası mevcut mu: {os.path.exists(image_path)}")

results = {}

# SORT takip sistemi
mot_tracker = Sort()

# Modelleri yükle
coco_model = YOLO('yolov8n.pt')
license_plate_detector = YOLO('license_plate_detector.pt')

# EasyOCR okuyucusu
reader = easyocr.Reader(['en', 'tr'])  # Türkçe ve İngilizce

# Alternatif plaka okuma fonksiyonu
def read_license_plate_enhanced(img):
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
    print(f"  -> Mevcut OCR sonucu: {text_old}, Güven: {score_old if text_old else 'Okunamadı'}")
    
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
                    print(f"  -> EasyOCR ({method_name}): {text}, Güven: {confidence:.2f}")
                    
                    # Temizlenmiş metni ekle
                    clean_text = ''.join(c for c in text if c.isalnum()).upper()
                    if clean_text and confidence > 0.3:  # Minimum güven skoru
                        # X koordinatına göre sıralayarak metinlerin pozisyonunu kaydediyoruz
                        left, top = bbox[0]
                        all_detected_texts.append((clean_text, confidence, left))
        except Exception as e:
            print(f"  -> OCR hatası ({method_name}): {str(e)}")
    
    # Tespit edilen metinleri pozisyona göre sırala (soldan sağa)
    all_detected_texts.sort(key=lambda x: x[2])
    
    print(f"  -> Tespit edilen tüm metinler (soldan sağa): {[t[0] for t in all_detected_texts]}")
    
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
            for k in range(j+1, len(all_detected_texts)):
                three_combined = combined + all_detected_texts[k][0]
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
    
    print(f"  -> Olası plaka formatları: {valid_plates}")
    
    # En yüksek güven skoruna sahip olanı seç
    if valid_plates:
        valid_plates.sort(key=lambda x: x[1], reverse=True)
        best_text, best_score = valid_plates[0]
        print(f"  -> En iyi plaka sonucu: {best_text}, Güven: {best_score:.2f}")
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

# Statik görüntü için özel işleme
frame_nmr = 0
results[frame_nmr] = {}

# Görüntüyü okuma
frame = cv2.imread(image_path)
if frame is None:
    print(f"HATA: Görüntü yüklenemedi: {image_path}")
    exit(1)
else:
    print(f"Görüntü yüklendi, boyut: {frame.shape}")

# Araç tespiti
vehicles = [2, 3, 5, 7]  # COCO sınıfları: car, motorcycle, bus, truck

print("Araç tespiti yapılıyor...")
detections = coco_model(frame)[0]
detections_ = []

print(f"Toplam tespit edilen nesne: {len(detections.boxes.data)}")
for i, detection in enumerate(detections.boxes.data.tolist()):
    x1, y1, x2, y2, score, class_id = detection
    class_id = int(class_id)
    print(f"Nesne {i+1}: Sınıf ID={class_id}, Güven={score:.2f}, Kutu=[{x1:.1f}, {y1:.1f}, {x2:.1f}, {y2:.1f}]")
    if class_id in vehicles:
        detections_.append([x1, y1, x2, y2, score])
        print(f"  -> Araç olarak eklendi")

# Araç takibi
print(f"Takip edilecek araç tespiti: {len(detections_)}")
if len(detections_) > 0:
    track_ids = mot_tracker.update(np.asarray(detections_))
    print(f"Takip edilen araç sayısı: {len(track_ids)}")
    for i, track in enumerate(track_ids):
        print(f"Araç {i+1}: ID={track[4]}, Konum=[{track[0]:.1f}, {track[1]:.1f}, {track[2]:.1f}, {track[3]:.1f}]")
else:
    track_ids = np.array([])
    print("Takip edilecek araç yok")

# Plaka tespiti
print("Plaka tespiti yapılıyor...")
license_plates = license_plate_detector(frame)[0]
print(f"Tespit edilen plaka sayısı: {len(license_plates.boxes.data)}")

# Tespit edilen plakaları ve araçları görselleştir
visual_img = frame.copy()
for car in track_ids:
    x1, y1, x2, y2, car_id = car
    cv2.rectangle(visual_img, (int(x1), int(y1)), (int(x2), int(y2)), (0, 255, 0), 2)
    cv2.putText(visual_img, f"Car ID: {car_id}", (int(x1), int(y1)-10), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 255, 0), 2)

# Plaka işleme
for i, license_plate in enumerate(license_plates.boxes.data.tolist()):
    x1, y1, x2, y2, score, class_id = license_plate
    print(f"Plaka {i+1}: Güven={score:.2f}, Kutu=[{x1:.1f}, {y1:.1f}, {x2:.1f}, {y2:.1f}]")

    # Plakayı görselleştir
    cv2.rectangle(visual_img, (int(x1), int(y1)), (int(x2), int(y2)), (0, 0, 255), 2)

    # Crop plaka
    license_plate_crop = frame[int(y1):int(y2), int(x1): int(x2), :]

    # Eğer hiç araç tespit edilmediyse, plakaları doğrudan işle
    if len(track_ids) == 0:
        print("  -> Hiç araç takibi olmadığı için plakaları doğrudan işliyoruz.")
        
        # Geliştirilmiş OCR ile plakayı oku
        license_plate_text, license_plate_text_score = read_license_plate_enhanced(license_plate_crop)

        if license_plate_text is not None:
            # Plakanın kendi ID'sini atayalım
            placeholder_id = 1000 + i  # Plaka sıra numarası + 1000
            results[frame_nmr][placeholder_id] = {
                'car': {'bbox': [x1, y1, x2, y2]},  # Plaka alanını aracın kendisi olarak kabul ediyoruz
                'license_plate': {
                    'bbox': [x1, y1, x2, y2],
                    'text': license_plate_text,
                    'bbox_score': score,
                    'text_score': license_plate_text_score
                }
            }
            print(f"  -> Plaka kaydedildi, ID: {placeholder_id}")
            
            # Görselleştirmede metni göster
            cv2.putText(visual_img, license_plate_text, (int(x1), int(y2)+20), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)
    else:
        # Plakayı araca ata
        xcar1, ycar1, xcar2, ycar2, car_id = get_car(license_plate, track_ids)
        print(f"  -> Araç ID: {car_id if car_id != -1 else 'Eşleştirilemedi'}")
        
        # Geliştirilmiş OCR ile plakayı oku
        license_plate_text, license_plate_text_score = read_license_plate_enhanced(license_plate_crop)
        
        if license_plate_text is not None and car_id != -1:
            results[frame_nmr][car_id] = {
                'car': {'bbox': [xcar1, ycar1, xcar2, ycar2]},
                'license_plate': {
                    'bbox': [x1, y1, x2, y2],
                    'text': license_plate_text,
                    'bbox_score': score,
                    'text_score': license_plate_text_score
                }
            }
            print(f"  -> Plaka kaydedildi, Araç ID: {car_id}")
            
            # Görselleştirmede metni göster
            cv2.putText(visual_img, license_plate_text, (int(x1), int(y2)+20), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)
        
        # Eğer araç eşleştirilemedi ama plaka okundu ise
        elif license_plate_text is not None and car_id == -1:
            placeholder_id = 1000 + i
            results[frame_nmr][placeholder_id] = {
                'car': {'bbox': [x1, y1, x2, y2]},  # Plaka alanını aracın kendisi olarak kabul ediyoruz
                'license_plate': {
                    'bbox': [x1, y1, x2, y2],
                    'text': license_plate_text,
                    'bbox_score': score,
                    'text_score': license_plate_text_score
                }
            }
            print(f"  -> Plaka kaydedildi (araç eşleşmedi), ID: {placeholder_id}")
            
            # Görselleştirmede metni göster
            cv2.putText(visual_img, license_plate_text, (int(x1), int(y2)+20), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)

# Görselleştirme sonucunu kaydet
visual_path = f"{debug_dir}/result_{int(time.time())}.jpg"
cv2.imwrite(visual_path, visual_img)
print(f"Görselleştirme kaydedildi: {visual_path}")

# Sonuçları yaz ve kontrol et
print("\nSonuçları CSV'ye yazıyoruz...")
write_csv(results, './test.csv')

# CSV'nin yazıldığını kontrol et
if os.path.exists('./test.csv'):
    with open('./test.csv', 'r') as f:
        content = f.read()
        lines = content.count('\n')
        print(f"CSV dosyası oluşturuldu: {lines} satır içeriyor")
else:
    print("HATA: CSV dosyası oluşturulamadı!")

print("\nToplam sonuçlar:")
if not results[frame_nmr]:
    print("Hiç sonuç bulunamadı!")
else:
    for car_id, data in results[frame_nmr].items():
        print(f"ID: {car_id}, Plaka: {data['license_plate']['text']}, Güven: {data['license_plate']['text_score']:.2f}")