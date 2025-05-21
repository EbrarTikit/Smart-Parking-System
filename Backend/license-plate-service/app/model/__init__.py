"""
Plaka tespiti, takibi ve okunması için gerekli fonksiyonları içeren model modülü
"""

__all__ = ['process_image_for_plate_recognition', 'read_license_plate_enhanced']

import cv2
import numpy as np
import logging
import os
import time
from pathlib import Path
from typing import Dict, Tuple, List, Any, Optional, Union

# Loglama yapılandırması
logger = logging.getLogger(__name__)

# Plaka işleme fonksiyonu - API tarafından çağrılır
def process_image_for_plate_recognition(image: Union[str, np.ndarray, bytes], save_debug: bool = False) -> Dict[str, Any]:
    """
    Verilen görüntüdeki plakaları tespit edip okuyan ana fonksiyon.
    
    Args:
        image: Görüntü dosyası yolu, numpy dizisi veya bytes şeklinde görüntü verisi
        save_debug: Hata ayıklama görüntülerini kaydetme seçeneği
        
    Returns:
        Dict: Tespit edilen plakalar ve araçlar hakkında bilgi içeren sözlük
    """
    try:
        logger.info("Plaka tanıma işlemi başlatılıyor...")
        
        # Ana modül içeriklerini yükle
        try:
            # SORT/mot_tracker kaldırıldı
            from .main import coco_model, license_plate_detector, read_license_plate_enhanced, USE_REAL_MODEL
            logger.info("Model modülleri başarıyla yüklendi")
        except ImportError as e:
            logger.error(f"Model modüllerini içe aktarırken hata: {str(e)}")
            # Basit bir mock sonuç döndür
            return {"error": f"Model modülleri yüklenemedi: {str(e)}", "results": {}, "license_plates": []}
        
        # Görüntüyü doğru formata dönüştür
        frame = None
        
        # Görüntü yükleme
        try:
            if isinstance(image, str):
                # Dosya yolu olarak verildiyse
                frame = cv2.imread(image)
                logger.info(f"Görüntü dosyadan yüklendi: {image}")
            elif isinstance(image, np.ndarray):
                # Numpy dizisi olarak verildiyse
                frame = image.copy()
                logger.info("Görüntü numpy dizisinden yüklendi")
            elif isinstance(image, bytes):
                # Bytes olarak verildiyse
                nparr = np.frombuffer(image, np.uint8)
                frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
                logger.info("Görüntü bytes verisinden yüklendi")
            else:
                logger.error(f"Desteklenmeyen görüntü formatı: {type(image)}")
                return {"error": "Desteklenmeyen görüntü formatı", "results": {}, "license_plates": []}
            
            if frame is None:
                logger.warning("Görüntü yüklenemedi veya boş")
                frame = np.zeros((300, 300, 3), dtype=np.uint8)  # Boş bir çerçeve oluştur
        except Exception as e:
            logger.error(f"Görüntü yükleme hatası: {str(e)}")
            frame = np.zeros((300, 300, 3), dtype=np.uint8)  # Boş bir çerçeve oluştur
        
        # Sonuçlar için veri yapısı oluştur
        results = {}
        frame_nmr = 0
        results[frame_nmr] = {}
        
        # Debug görselleştirmesi için çerçeveyi kopyala
        debug_frame = frame.copy() if save_debug else None
        
        # Test amaçlı, model yüklenemezse sabit bir sonuç döndür
        if not USE_REAL_MODEL:
            logger.warning("Gerçek model yüklenemedi - test verisi döndürülüyor")
            return {
                "warning": "Model yüklenemedi, test sonuçları döndürülüyor",
                "results": {
                    "0": {
                        "license_plate": {
                            "text": "34ABC123",
                            "text_score": 0.9,
                            "bbox": [100, 100, 200, 150]
                        }
                    }
                },
                "license_plates": ["34ABC123"]
            }
        
        # Araç tespiti yap
        logger.info("Araç tespiti yapılıyor...")
        vehicles = [2, 3, 5, 7]  # COCO sınıfları: car, motorcycle, bus, truck
        
        try:
            detections = coco_model(frame)[0]
            detections_ = []
            
            # Araçları filtrele
            for detection in detections.boxes.data.tolist():
                x1, y1, x2, y2, score, class_id = detection
                class_id = int(class_id)
                if class_id in vehicles and score > 0.3:  # Güven skoru filtresi
                    detections_.append([x1, y1, x2, y2, score])
            
            logger.info(f"Tespit edilen araç sayısı: {len(detections_)}")
            
            # SORT tracker yok - boş bir dizi oluştur
            track_ids = np.array([])
            if len(detections_) > 0:
                # Her tespit için bir ID atayarak basit track_ids oluştur
                track_ids = np.zeros((len(detections_), 5))
                for i, det in enumerate(detections_):
                    x1, y1, x2, y2, score = det
                    track_ids[i] = [x1, y1, x2, y2, i+1]  # i+1 ile ID ata
                
                logger.info(f"Basit araç takibi: {len(track_ids)} araç")
                
                # Takip edilen araçları görselleştir
                if save_debug and debug_frame is not None:
                    for car in track_ids:
                        x1, y1, x2, y2, car_id = car
                        cv2.rectangle(debug_frame, (int(x1), int(y1)), (int(x2), int(y2)), (0, 255, 0), 2)
                        cv2.putText(debug_frame, f"Car {int(car_id)}", (int(x1), int(y1)-10), 
                                   cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 255, 0), 2)
            else:
                logger.info("Takip edilecek araç yok")
            
            # Plaka tespiti yap
            logger.info("Plaka tespiti yapılıyor...")
            license_plates = license_plate_detector(frame)[0]
            logger.info(f"Tespit edilen plaka sayısı: {len(license_plates.boxes.data)}")
            
            license_plate_results = []
            
            # Tespit edilen plakaları işle
            for i, license_plate in enumerate(license_plates.boxes.data.tolist()):
                try:
                    x1, y1, x2, y2, score, class_id = license_plate
                    
                    # Güven skoru çok düşük plakaları atla
                    if score < 0.3:
                        continue
                    
                    # Plakayı görselleştir
                    if save_debug and debug_frame is not None:
                        cv2.rectangle(debug_frame, (int(x1), int(y1)), (int(x2), int(y2)), (0, 0, 255), 2)
                    
                    # Plaka görüntüsünü kırp
                    license_plate_crop = frame[int(y1):int(y2), int(x1): int(x2), :]
                    
                    # Plakayı oku
                    from .main import read_license_plate_enhanced
                    license_plate_text, license_plate_text_score = read_license_plate_enhanced(license_plate_crop)
                    
                    if license_plate_text is not None:
                        # Plakaya ait araç var mı?
                        if len(track_ids) > 0:
                            from .util import get_car
                            xcar1, ycar1, xcar2, ycar2, car_id = get_car(license_plate, track_ids)
                            
                            # Araca eşleştirebildiysek
                            if car_id != -1:
                                results[frame_nmr][car_id] = {
                                    'car': {'bbox': [xcar1, ycar1, xcar2, ycar2]},
                                    'license_plate': {
                                        'bbox': [x1, y1, x2, y2],
                                        'text': license_plate_text,
                                        'bbox_score': score,
                                        'text_score': license_plate_text_score
                                    }
                                }
                                license_plate_results.append(license_plate_text)
                                
                                # Görselleştirmede metni göster
                                if save_debug and debug_frame is not None:
                                    cv2.putText(debug_frame, license_plate_text, (int(x1), int(y2)+20), 
                                               cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)
                            else:
                                # Araç eşleştirilemedi, ama plaka var
                                placeholder_id = f"plate_{i}"
                                results[frame_nmr][placeholder_id] = {
                                    'car': {'bbox': [x1-50, y1-50, x2+50, y2+50]},  # Plakanın çevresini genişlet
                                    'license_plate': {
                                        'bbox': [x1, y1, x2, y2],
                                        'text': license_plate_text,
                                        'bbox_score': score,
                                        'text_score': license_plate_text_score
                                    }
                                }
                                license_plate_results.append(license_plate_text)
                                
                                # Görselleştirmede metni göster
                                if save_debug and debug_frame is not None:
                                    cv2.putText(debug_frame, license_plate_text, (int(x1), int(y2)+20), 
                                               cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)
                        else:
                            # Hiç araç tespiti yoksa, plakayı doğrudan işle
                            placeholder_id = f"plate_{i}"
                            results[frame_nmr][placeholder_id] = {
                                'car': {'bbox': [x1-50, y1-50, x2+50, y2+50]},  # Plakanın çevresini genişlet
                                'license_plate': {
                                    'bbox': [x1, y1, x2, y2],
                                    'text': license_plate_text,
                                    'bbox_score': score,
                                    'text_score': license_plate_text_score
                                }
                            }
                            license_plate_results.append(license_plate_text)
                            
                            # Görselleştirmede metni göster
                            if save_debug and debug_frame is not None:
                                cv2.putText(debug_frame, license_plate_text, (int(x1), int(y2)+20), 
                                           cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)
                except Exception as e:
                    logger.error(f"Plaka işleme hatası ({i}): {str(e)}")
            
            # Debug görüntüsünü kaydet
            if save_debug and debug_frame is not None:
                debug_dir = "./debug_plates"
                os.makedirs(debug_dir, exist_ok=True)
                debug_path = f"{debug_dir}/result_{int(time.time())}.jpg"
                cv2.imwrite(debug_path, debug_frame)
                logger.info(f"Debug görüntüsü kaydedildi: {debug_path}")
            
            # Sonuçları döndür
            return {
                "results": results,
                "license_plates": license_plate_results,
                "timestamp": time.time()
            }
            
        except Exception as e:
            logger.error(f"Görüntü işleme hatası: {str(e)}")
            import traceback
            traceback.print_exc()
            return {
                "error": f"Görüntü işleme hatası: {str(e)}",
                "results": {},
                "license_plates": []
            }
    
    except Exception as e:
        logger.error(f"Genel işlem hatası: {str(e)}")
        import traceback
        traceback.print_exc()
        return {
            "error": f"Genel işlem hatası: {str(e)}",
            "results": {},
            "license_plates": []
        }

# Test için
if __name__ == "__main__":
    # Test görüntüsü
    test_image = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'debug_images', 'sample.jpg')
    if not os.path.exists(test_image):
        test_image = "sample.jpg"  # Çalışma dizininde ara
    
    # Test et
    result = process_image_for_plate_recognition(test_image, save_debug=True)
    
    # Sonuçları yazdır
    print("Test sonuçları:")
    print(result) 