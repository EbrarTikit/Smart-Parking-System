// API URL'leri
const API_BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:8005";
const WS_BASE_URL = process.env.REACT_APP_WS_URL || "ws://localhost:8005";

// HTTP İstekleri
export const api = {
  // Araç Girişi
  vehicleEntry: async (licensePlate) => {
    try {
      const response = await fetch(`${API_BASE_URL}/vehicle/entry`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ license_plate: licensePlate }),
      });

      return await response.json();
    } catch (error) {
      console.error("Araç girişi sırasında hata:", error);
      throw error;
    }
  },

  // Araç Çıkışı
  vehicleExit: async (licensePlate, parkingId = 1) => {
    try {
      const response = await fetch(`${API_BASE_URL}/vehicle/exit`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          license_plate: licensePlate,
          parking_id: parkingId,
        }),
      });

      return await response.json();
    } catch (error) {
      console.error("Araç çıkışı sırasında hata:", error);
      throw error;
    }
  },

  // Plaka Kayıtlarını Getir
  getPlates: async (limit = 100, skip = 0) => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/plates?limit=${limit}&skip=${skip}`
      );
      return await response.json();
    } catch (error) {
      console.error("Plaka kayıtları alınırken hata:", error);
      throw error;
    }
  },

  // Görüntü ile Araç Girişi
  processPlateForEntry: async (imageFile, saveDebug = false) => {
    try {
      const formData = new FormData();
      formData.append("file", imageFile);

      const response = await fetch(
        `${API_BASE_URL}/process-plate-entry?save_debug=${saveDebug}`,
        {
          method: "POST",
          body: formData,
        }
      );

      return await response.json();
    } catch (error) {
      console.error("Görüntü işleme sırasında hata:", error);
      throw error;
    }
  },

  // Görüntü ile Araç Çıkışı
  processPlateForExit: async (imageFile, parkingId = 1, saveDebug = false) => {
    try {
      const formData = new FormData();
      formData.append("file", imageFile);

      const response = await fetch(
        `${API_BASE_URL}/process-plate-exit?parking_id=${parkingId}&save_debug=${saveDebug}`,
        {
          method: "POST",
          body: formData,
        }
      );

      return await response.json();
    } catch (error) {
      console.error("Görüntü işleme sırasında hata:", error);
      throw error;
    }
  },

  // WebSocket Bilgilerini Getir
  getWebSocketInfo: async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/ws/info`);
      return await response.json();
    } catch (error) {
      console.error("WebSocket bilgileri alınırken hata:", error);
      throw error;
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

  const ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    console.log(`WebSocket bağlantısı başarıyla kuruldu: ${wsUrl}`);
  };

  ws.onerror = (error) => {
    console.error("WebSocket hatası:", error);
  };

  ws.onclose = () => {
    console.log("WebSocket bağlantısı kapandı");
  };

  return ws;
};
