// API URL'leri
const API_BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:8005";
const WS_BASE_URL = process.env.REACT_APP_WS_URL || "ws://localhost:8005";
const USER_SERVICE_URL =
  process.env.REACT_APP_USER_SERVICE_URL || "http://localhost:8050";

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
  USER_SERVICE_URL,

  // Giriş Yapma (Login)
  login: async (username, password) => {
    try {
      console.log(`Kullanıcı girişi yapılıyor: ${username}`);
      const response = await fetch(`${USER_SERVICE_URL}/api/auth/signin`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username,
          password,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `HTTP hatası: ${response.status}`);
      }

      const data = await response.json();

      // JWT token'ı local storage'a kaydet
      localStorage.setItem("token", data.token);
      localStorage.setItem("userId", data.userId);

      console.log("Kullanıcı girişi başarılı:", data);
      return data;
    } catch (error) {
      console.error("Kullanıcı girişi sırasında hata:", error);
      throw error;
    }
  },

  // Kayıt Olma (Register)
  register: async (username, email, password) => {
    try {
      console.log(`Yeni kullanıcı kaydı yapılıyor: ${username}, ${email}`);
      const response = await fetch(`${USER_SERVICE_URL}/api/auth/signup`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username,
          email,
          password,
        }),
      });

      if (!response.ok) {
        // Hata mesajını text olarak alıp konsola yazdıralım
        const errorText = await response.text();
        console.error("Hata yanıtı (ham metin):", errorText);

        // Eğer JSON olarak ayrıştırılabilirse, message'ı alalım
        let errorMessage = `HTTP hatası: ${response.status}`;
        try {
          const errorData = JSON.parse(errorText);
          if (errorData && errorData.message) {
            errorMessage = errorData.message;
          }
        } catch (jsonError) {
          console.error("Hata yanıtı JSON olarak ayrıştırılamadı:", jsonError);
        }

        throw new Error(errorMessage);
      }

      // Başarılı yanıtı text olarak alıp konsola yazdıralım
      const responseText = await response.text();
      console.log("Başarılı yanıt (ham metin):", responseText);

      // Boş yanıt kontrolü
      if (!responseText || responseText.trim() === "") {
        console.log("Boş yanıt alındı, varsayılan başarı mesajı kullanılıyor");
        return { message: "Kullanıcı kaydı başarılı" };
      }

      // JSON olarak ayrıştırmayı dene
      try {
        const data = JSON.parse(responseText);
        console.log("Kullanıcı kaydı başarılı:", data);
        return data;
      } catch (jsonError) {
        console.error("Yanıt JSON olarak ayrıştırılamadı:", jsonError);
        // JSON ayrıştırma başarısız olursa, ham metni bir mesaj olarak döndür
        return { message: responseText };
      }
    } catch (error) {
      console.error("Kullanıcı kaydı sırasında hata:", error);
      throw error;
    }
  },

  // Çıkış Yapma (Logout)
  logout: () => {
    // Token ve kullanıcı bilgilerini local storage'dan temizle
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    console.log("Kullanıcı çıkışı yapıldı");
  },

  // Kullanıcı Bilgilerini Getir
  getUserInfo: async (userId) => {
    try {
      console.log(`Kullanıcı bilgileri alınıyor: ID=${userId}`);

      // JWT token'ı al
      const token = localStorage.getItem("token");

      if (!token) {
        throw new Error("Oturum bulunamadı");
      }

      const response = await fetch(`${USER_SERVICE_URL}/api/users/${userId}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        if (response.status === 401) {
          // Token geçersiz veya süresi dolmuş
          localStorage.removeItem("token");
          localStorage.removeItem("userId");
          throw new Error("Oturum süresi dolmuş");
        }
        throw new Error(`HTTP hatası: ${response.status}`);
      }

      const data = await response.json();
      console.log("Kullanıcı bilgileri alındı:", data);
      return data;
    } catch (error) {
      console.error("Kullanıcı bilgileri alınırken hata:", error);
      throw error;
    }
  },

  // Token kontrolü - oturum açık mı?
  isAuthenticated: () => {
    const token = localStorage.getItem("token");
    return !!token; // token varsa true, yoksa false döner
  },

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
      const response = await fetchWithTimeout(
        `${API_BASE_URL}/vehicle/exit`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            license_plate: licensePlate,
            parking_id: parkingId,
          }),
        },
        30000
      ); // 30 saniye timeout

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
