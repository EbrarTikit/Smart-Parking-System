import { useState, useEffect, useCallback, useRef } from "react";
import { createWebSocket } from "../services/api";

const useWebSocket = (type = "general", id = null) => {
  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState([]);
  const [lastMessage, setLastMessage] = useState(null);
  const [error, setError] = useState(null);
  const ws = useRef(null);

  // WebSocket bağlantısını kurma
  useEffect(() => {
    const connectWebSocket = () => {
      try {
        // WebSocket bağlantısını oluştur
        ws.current = createWebSocket(type, id);

        // WebSocket olaylarını dinle
        ws.current.onopen = () => {
          setIsConnected(true);
          setError(null);
        };

        ws.current.onclose = () => {
          setIsConnected(false);
        };

        ws.current.onerror = (event) => {
          setError("WebSocket bağlantı hatası");
          console.error("WebSocket error:", event);
        };

        ws.current.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            setLastMessage(data);
            setMessages((prevMessages) => [...prevMessages, data]);
          } catch (err) {
            console.error("WebSocket mesajı işlenirken hata:", err);
          }
        };
      } catch (err) {
        setError(`WebSocket bağlantısı kurulamadı: ${err.message}`);
        console.error("WebSocket bağlantısı oluşturulurken hata:", err);
      }
    };

    connectWebSocket();

    // Cleanup fonksiyonu
    return () => {
      if (ws.current) {
        ws.current.close();
      }
    };
  }, [type, id]);

  // Mesaj gönderme fonksiyonu
  const sendMessage = useCallback(
    (message) => {
      if (ws.current && isConnected) {
        try {
          // Eğer message bir string değilse JSON'a çevir
          const messageStr =
            typeof message === "string" ? message : JSON.stringify(message);

          ws.current.send(messageStr);
          return true;
        } catch (err) {
          console.error("WebSocket mesajı gönderilirken hata:", err);
          return false;
        }
      }
      return false;
    },
    [isConnected]
  );

  // Mesajları temizleme
  const clearMessages = useCallback(() => {
    setMessages([]);
  }, []);

  // WebSocket durumunu sorgulama
  const checkStatus = useCallback(() => {
    if (isConnected) {
      sendMessage(JSON.stringify({ type: "status" }));
    }
  }, [isConnected, sendMessage]);

  return {
    isConnected,
    messages,
    lastMessage,
    error,
    sendMessage,
    clearMessages,
    checkStatus,
  };
};

export default useWebSocket;
