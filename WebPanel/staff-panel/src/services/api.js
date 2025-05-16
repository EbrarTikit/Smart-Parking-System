// API URL'leri
const API_BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:8005";
const WS_BASE_URL = process.env.REACT_APP_WS_URL || "ws://localhost:8005";

// API istek göndermek için ortak fonksiyon
const fetchWithTimeout = async (url, options = {}, timeout = 8000) => {
  const controller = new AbortController();
  const { signal } = controller;

  // Timeout mekanizması
  const timeoutId = setTimeout(() => {
    controller.abort();
  }, timeout);

  try {
    const response = await fetch(url, { ...options, signal });
    clearTimeout(timeoutId);
    return response;
  } catch (error) {
    clearTimeout(timeoutId);
    if (error.name === "AbortError") {
      console.error(`İstek zaman aşımına uğradı: ${url}`);
      throw new Error(`İstek zaman aşımına uğradı (${timeout}ms): ${url}`);
    }
    throw error;
  }
};

// HTTP İstekleri
export const api = {
  // API URL sabitleri
  API_BASE_URL,
  WS_BASE_URL,

  // Araç Girişi
  vehicleEntry: async (licensePlate, parkingId = 1) => {
    try {
      console.log(
        `Araç girişi isteği gönderiliyor: ${licensePlate}, Otopark ID: ${parkingId}`
      );
      const response = await fetchWithTimeout(`${API_BASE_URL}/vehicle/entry`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          license_plate: licensePlate,
          parking_id: parkingId,
        }),
      });

      if (!response.ok) {
        throw new Error(
          `HTTP hatası: ${response.status} ${response.statusText}`
        );
      }

      const data = await response.json();
      console.log("Araç girişi yanıtı:", data);
      return data;
    } catch (error) {
      console.error("Araç girişi sırasında hata:", error);
      throw error;
    }
  },

  // Araç Çıkışı
  vehicleExit: async (licensePlate, parkingId = 1) => {
    try {
      console.log(
        `Araç çıkışı isteği gönderiliyor: ${licensePlate}, Otopark ID: ${parkingId}`
      );
      const response = await fetchWithTimeout(`${API_BASE_URL}/vehicle/exit`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          license_plate: licensePlate,
          parking_id: parkingId,
        }),
      });

      if (!response.ok) {
        throw new Error(
          `HTTP hatası: ${response.status} ${response.statusText}`
        );
      }

      const data = await response.json();
      console.log("Araç çıkışı yanıtı:", data);
      return data;
    } catch (error) {
      console.error("Araç çıkışı sırasında hata:", error);
      throw error;
    }
  },

  // Plaka Kayıtlarını Getir
  getPlates: async (limit = 100, skip = 0) => {
    try {
      console.log(`Plaka kayıtları alınıyor: limit=${limit}, skip=${skip}`);
      const response = await fetchWithTimeout(
        `${API_BASE_URL}/plates?limit=${limit}&skip=${skip}`
      );

      if (!response.ok) {
        throw new Error(
          `HTTP hatası: ${response.status} ${response.statusText}`
        );
      }

      const data = await response.json();
      console.log(`${data.length} plaka kaydı alındı`);
      return data;
    } catch (error) {
      console.error("Plaka kayıtları alınırken hata:", error);
      throw error;
    }
  },

  // Görüntü ile Araç Girişi
  processPlateForEntry: async (imageFile, parkingId = 1, saveDebug = false) => {
    try {
      console.log(
        `Görüntü ile araç girişi isteği gönderiliyor: Otopark ID: ${parkingId}`
      );
      const formData = new FormData();
      formData.append("file", imageFile);

      const response = await fetchWithTimeout(
        `${API_BASE_URL}/process-plate-entry?parking_id=${parkingId}&save_debug=${saveDebug}`,
        {
          method: "POST",
          body: formData,
        },
        30000 // Görüntü işleme için daha uzun timeout (30 saniye)
      );

      if (!response.ok) {
        throw new Error(
          `HTTP hatası: ${response.status} ${response.statusText}`
        );
      }

      const data = await response.json();
      console.log("Görüntü işleme yanıtı:", data);
      return data;
    } catch (error) {
      console.error("Görüntü işleme sırasında hata:", error);
      throw error;
    }
  },

  // Görüntü ile Araç Çıkışı
  processPlateForExit: async (imageFile, parkingId = 1, saveDebug = false) => {
    try {
      console.log(
        `Görüntü ile araç çıkışı isteği gönderiliyor: Otopark ID: ${parkingId}`
      );
      const formData = new FormData();
      formData.append("file", imageFile);

      const response = await fetchWithTimeout(
        `${API_BASE_URL}/process-plate-exit?parking_id=${parkingId}&save_debug=${saveDebug}`,
        {
          method: "POST",
          body: formData,
        },
        30000 // Görüntü işleme için daha uzun timeout (30 saniye)
      );

      if (!response.ok) {
        throw new Error(
          `HTTP hatası: ${response.status} ${response.statusText}`
        );
      }

      const data = await response.json();
      console.log("Görüntü işleme yanıtı:", data);
      return data;
    } catch (error) {
      console.error("Görüntü işleme sırasında hata:", error);
      throw error;
    }
  },

  // WebSocket Bilgilerini Getir
  getWebSocketInfo: async () => {
    try {
      console.log("WebSocket bilgileri alınıyor");
      const response = await fetchWithTimeout(`${API_BASE_URL}/ws/info`);

      if (!response.ok) {
        throw new Error(
          `HTTP hatası: ${response.status} ${response.statusText}`
        );
      }

      const data = await response.json();
      console.log("WebSocket bilgileri alındı:", data);
      return data;
    } catch (error) {
      console.error("WebSocket bilgileri alınırken hata:", error);
      throw error;
    }
  },

  // Aktif Araçları Getir
  getActiveVehicles: async (parkingId = null) => {
    try {
      console.log(
        `Aktif araçlar alınıyor${
          parkingId ? ` (Otopark ID: ${parkingId})` : ""
        }`
      );
      let url = `${API_BASE_URL}/active-vehicles`;

      // Otopark ID'si varsa URL'e ekle
      if (parkingId) {
        url += `?parking_id=${parkingId}`;
      }

      const response = await fetchWithTimeout(url);

      if (!response.ok) {
        if (response.status === 404) {
          console.warn(
            "Aktif araçlar endpoint'i bulunamadı (404). WebSocket güncellemelerine bakılacak."
          );
          return [];
        }
        throw new Error(
          `HTTP hatası: ${response.status} ${response.statusText}`
        );
      }

      const data = await response.json();
      console.log(
        `${data.length} aktif araç alındı${
          parkingId ? ` (Otopark ID: ${parkingId})` : ""
        }`
      );
      return data;
    } catch (error) {
      console.error("Aktif araçlar alınırken hata:", error);
      return []; // Hata durumunda boş dizi dön, UI çalışmaya devam etsin
    }
  },

  // Son Aktiviteleri Getir
  getRecentActivities: async (parkingId = null) => {
    try {
      console.log(
        `Son aktiviteler alınıyor${
          parkingId ? ` (Otopark ID: ${parkingId})` : ""
        }`
      );
      let url = `${API_BASE_URL}/recent-activities`;

      // Otopark ID'si varsa URL'e ekle
      if (parkingId) {
        url += `?parking_id=${parkingId}`;
      }

      const response = await fetchWithTimeout(url);

      if (!response.ok) {
        if (response.status === 404) {
          console.warn(
            "Son aktiviteler endpoint'i bulunamadı (404). WebSocket güncellemelerine bakılacak."
          );
          return [];
        }
        throw new Error(
          `HTTP hatası: ${response.status} ${response.statusText}`
        );
      }

      const data = await response.json();
      console.log(
        `${data.length} son aktivite alındı${
          parkingId ? ` (Otopark ID: ${parkingId})` : ""
        }`
      );
      return data;
    } catch (error) {
      console.error("Son aktiviteler alınırken hata:", error);
      return []; // Hata durumunda boş dizi dön, UI çalışmaya devam etsin
    }
  },
};

// WebSocket Bağlantısı
export const createWebSocket = (type = "general", id = null) => {
  let wsUrl = `${WS_BASE_URL}/ws`;

  if (type === "admin") {
    wsUrl = `${WS_BASE_URL}/ws/admin`;
  } else if (type === "parking" && id) {
    wsUrl = `${WS_BASE_URL}/ws/parking/${id}`;
  } else if (type === "vehicle" && id) {
    wsUrl = `${WS_BASE_URL}/ws/vehicle/${id}`;
  }

  console.log(`WebSocket bağlantısı oluşturuluyor: ${wsUrl}`);

  try {
    const ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log(`WebSocket bağlantısı başarıyla kuruldu: ${wsUrl}`);
    };

    ws.onerror = (error) => {
      console.error(`WebSocket hatası (${wsUrl}):`, error);
    };

    ws.onclose = (event) => {
      console.log(
        `WebSocket bağlantısı kapandı (${wsUrl}): Kod: ${event.code}, Sebep: ${
          event.reason || "Sebep belirtilmedi"
        }`
      );
    };

    return ws;
  } catch (error) {
    console.error(
      `WebSocket bağlantısı oluşturulurken hata (${wsUrl}):`,
      error
    );
    throw error;
  }
};
