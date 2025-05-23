import { useState, useEffect, useCallback, useRef } from "react";
import { createWebSocket } from "../services/api";

const useWebSocket = (type = "general", id = null) => {
  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState([]);
  const [lastMessage, setLastMessage] = useState(null);
  const [error, setError] = useState(null);
  const ws = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const reconnectCountRef = useRef(0);
  const [reconnectAttempts, setReconnectAttempts] = useState(0);
  const MAX_RECONNECT_ATTEMPTS = 10; // Maksimum yeniden bağlanma denemesi
  const RECONNECT_INTERVAL = 2000; // Başlangıç bekleme süresi (ms)
  const pingIntervalRef = useRef(null);
  const lastPongTimeRef = useRef(Date.now()); // Son pong zamanını takip etmek için
  const MAX_PONG_DELAY = 45000; // Maksimum pong gecikmesi (ms) - 45 saniye

  // Bağlantının canlı olup olmadığını kontrol eden ping/pong
  const startPingInterval = useCallback(() => {
    // Önceki ping interval'i temizle
    if (pingIntervalRef.current) {
      clearInterval(pingIntervalRef.current);
    }

    // Her 30 saniyede bir ping gönder
    pingIntervalRef.current = setInterval(() => {
      if (ws.current && ws.current.readyState === WebSocket.OPEN) {
        try {
          console.log("WebSocket ping gönderiliyor...");
          ws.current.send(JSON.stringify({ type: "ping" }));

          // Son pong'dan bu yana geçen süreyi kontrol et
          const timeSinceLastPong = Date.now() - lastPongTimeRef.current;
          if (timeSinceLastPong > MAX_PONG_DELAY) {
            console.warn(
              `Son ${
                MAX_PONG_DELAY / 1000
              } saniyedir pong alınamadı, bağlantı yenileniyor...`
            );
            handleReconnect(); // Uzun süredir pong alınmadıysa yeniden bağlan
          }
        } catch (err) {
          console.error("Ping gönderilirken hata:", err);
          handleReconnect(); // Ping hatasında yeniden bağlan
        }
      } else if (ws.current) {
        console.warn(
          `WebSocket bağlantısı açık değil, durum: ${ws.current.readyState}`
        );
        handleReconnect(); // WebSocket bağlantısı açık değilse yeniden bağlan
      }
    }, 30000);

    return () => {
      if (pingIntervalRef.current) {
        clearInterval(pingIntervalRef.current);
      }
    };
  }, []);

  // WebSocket bağlantısını kurma
  const connectWebSocket = useCallback(() => {
    try {
      console.log(
        `WebSocket bağlantısı kuruluyor: ${type}${id ? "/" + id : ""}`
      );

      // Önceki bağlantıyı temizle
      if (ws.current) {
        try {
          ws.current.close();
        } catch (err) {
          console.error("Önceki WebSocket bağlantısı kapatılırken hata:", err);
        }
      }

      // WebSocket bağlantısını oluştur
      ws.current = createWebSocket(type, id);

      // WebSocket olaylarını dinle
      ws.current.onopen = () => {
        console.log(
          `WebSocket bağlantısı başarıyla kuruldu: ${type}${id ? "/" + id : ""}`
        );
        setIsConnected(true);
        setError(null);
        reconnectCountRef.current = 0; // Başarılı bağlantı sonrası sıfırla
        setReconnectAttempts(0);
        lastPongTimeRef.current = Date.now(); // Bağlantı kurulduğunda pong zamanını sıfırla
        startPingInterval(); // Ping interval'i başlat

        // Bağlantı kurulduğunda durum sorgusu gönder
        try {
          ws.current.send(JSON.stringify({ type: "status" }));
        } catch (err) {
          console.error("Durum sorgusu gönderilirken hata:", err);
        }
      };

      ws.current.onclose = (event) => {
        console.log(
          `WebSocket bağlantısı kapandı: Kod: ${event.code}, Sebep: ${
            event.reason || "Belirtilmedi"
          }`
        );
        setIsConnected(false);

        // Bağlantı normal şekilde kapandıysa yeniden bağlanma (1000 = normal kapatma)
        if (
          event.code !== 1000 &&
          reconnectCountRef.current < MAX_RECONNECT_ATTEMPTS
        ) {
          console.log(
            `Yeniden bağlanma denemesi (${
              reconnectCountRef.current + 1
            }/${MAX_RECONNECT_ATTEMPTS})...`
          );
          handleReconnect();
        } else if (reconnectCountRef.current >= MAX_RECONNECT_ATTEMPTS) {
          setError(
            "WebSocket bağlantısı kurulamadı: Maksimum yeniden bağlanma denemesi aşıldı."
          );
        }
      };

      ws.current.onerror = (event) => {
        console.error("WebSocket bağlantı hatası:", event);
        setError(
          "WebSocket bağlantı hatası oluştu. Yeniden bağlanma deneniyor..."
        );
      };

      ws.current.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          console.log("WebSocket mesajı alındı:", data);

          // Ping yanıtını işleme (ping-pong kontrolü)
          if (data.type === "pong") {
            console.log("WebSocket pong alındı, bağlantı canlı.");
            lastPongTimeRef.current = Date.now(); // Pong alındığında zamanı güncelle
            return; // Pong mesajlarını kaydetme
          }

          setLastMessage(data);
          setMessages((prevMessages) => [...prevMessages, data]);
        } catch (err) {
          console.error(
            "WebSocket mesajı işlenirken hata:",
            err,
            "Veri:",
            event.data
          );
        }
      };
    } catch (err) {
      console.error("WebSocket bağlantısı oluşturulurken hata:", err);
      setError(`WebSocket bağlantısı oluşturulamadı: ${err.message}`);

      // Hata durumunda yeniden bağlanma
      if (reconnectCountRef.current < MAX_RECONNECT_ATTEMPTS) {
        handleReconnect();
      }
    }
  }, [type, id, startPingInterval]);

  // Yeniden bağlanma işlemi
  const handleReconnect = useCallback(() => {
    // Önceki zamanlayıcıyı temizle
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }

    // Ping interval'i temizle
    if (pingIntervalRef.current) {
      clearInterval(pingIntervalRef.current);
    }

    // WebSocket'i kapat (eğer açıksa)
    if (ws.current) {
      try {
        ws.current.close();
      } catch (err) {
        console.error("WebSocket kapatılırken hata:", err);
      }
      ws.current = null;
    }

    // Deneme sayısını artır
    reconnectCountRef.current += 1;
    setReconnectAttempts(reconnectCountRef.current);

    // Exponential backoff ile yeniden bağlanma süresi (max 30 saniye)
    const delay = Math.min(
      RECONNECT_INTERVAL * Math.pow(1.5, reconnectCountRef.current - 1),
      30000
    );

    console.log(
      `WebSocket yeniden bağlantı planlandı: ${delay}ms sonra, deneme: ${reconnectCountRef.current}/${MAX_RECONNECT_ATTEMPTS}`
    );

    // Yeniden bağlanma için zamanlayıcı ayarla
    reconnectTimeoutRef.current = setTimeout(() => {
      console.log(
        `WebSocket yeniden bağlanma denemesi başlatılıyor: #${reconnectCountRef.current}`
      );
      connectWebSocket();
    }, delay);
  }, [connectWebSocket]);

  // İlk bağlantı ve temizleme
  useEffect(() => {
    console.log("WebSocket hook başlatılıyor...");
    connectWebSocket();

    // Cleanup fonksiyonu
    return () => {
      console.log("WebSocket hook temizleniyor...");

      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }

      if (pingIntervalRef.current) {
        clearInterval(pingIntervalRef.current);
      }

      if (ws.current) {
        try {
          ws.current.close();
        } catch (err) {
          console.error("WebSocket kapatılırken hata:", err);
        }
      }
    };
  }, [connectWebSocket]);

  // Mesaj gönderme fonksiyonu
  const sendMessage = useCallback(
    (message) => {
      if (!ws.current || ws.current.readyState !== WebSocket.OPEN) {
        console.warn(
          `WebSocket bağlı değil (Durum: ${
            ws.current ? ws.current.readyState : "null"
          }), mesaj gönderilemiyor`
        );

        // Bağlantı kapalıysa yeniden bağlanmayı dene ve mesajı kuyruğa al
        if (ws.current && ws.current.readyState === WebSocket.CLOSED) {
          console.log(
            "WebSocket bağlantısı kapalı, yeniden bağlanmayı deniyorum..."
          );
          handleReconnect();
        }

        return false;
      }

      try {
        // Eğer message bir string değilse JSON'a çevir
        const messageStr =
          typeof message === "string" ? message : JSON.stringify(message);

        ws.current.send(messageStr);
        console.log("WebSocket mesajı gönderildi:", message);
        return true;
      } catch (err) {
        console.error("WebSocket mesajı gönderilirken hata:", err);

        // Hata durumunda yeniden bağlanmayı dene
        handleReconnect();
        return false;
      }
    },
    [handleReconnect]
  );

  // Mesajları temizleme
  const clearMessages = useCallback(() => {
    setMessages([]);
    setLastMessage(null);
  }, []);

  // WebSocket durumunu sorgulama
  const checkStatus = useCallback(() => {
    if (isConnected) {
      console.log("WebSocket durum sorgusu gönderiliyor...");
      return sendMessage(JSON.stringify({ type: "status" }));
    } else {
      console.warn("WebSocket bağlı değil, durum sorgulanamıyor");
      return false;
    }
  }, [isConnected, sendMessage]);

  // Manuel yeniden bağlanma
  const reconnect = useCallback(() => {
    console.log("Manuel WebSocket yeniden bağlanma başlatılıyor...");
    reconnectCountRef.current = 0; // Sayacı sıfırla
    setReconnectAttempts(0);
    handleReconnect();
  }, [handleReconnect]);

  return {
    isConnected,
    messages,
    lastMessage,
    error,
    sendMessage,
    clearMessages,
    checkStatus,
    reconnect,
    reconnectAttempts,
  };
};

export default useWebSocket;
