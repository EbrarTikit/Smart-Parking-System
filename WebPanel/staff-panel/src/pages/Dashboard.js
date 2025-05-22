import React, { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import useWebSocket from "../hooks/useWebSocket";
import { api } from "../services/api";
import "./Dashboard.css";

// Tarih formatlamak için yardımcı fonksiyon
const formatDateTime = (dateString) => {
  if (!dateString) return "";
  const date = new Date(dateString);
  return date.toLocaleString("tr-TR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
};

// WebSocket bağlantı durumunu gösteren bileşen
const WebSocketStatus = ({
  isConnected,
  error,
  reconnectAttempts,
  onReconnect,
  onPing,
}) => {
  return (
    <div
      style={{
        padding: "10px",
        borderRadius: "4px",
        backgroundColor: "#f5f5f5",
        marginBottom: "15px",
        border: `1px solid ${isConnected ? "#C8E6C9" : "#FFCDD2"}`,
      }}
    >
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
        }}
      >
        <div style={{ display: "flex", alignItems: "center" }}>
          <span
            style={{
              width: "10px",
              height: "10px",
              borderRadius: "50%",
              backgroundColor: isConnected ? "#4CAF50" : "#F44336",
              display: "inline-block",
              marginRight: "10px",
            }}
          ></span>
          <span style={{ fontWeight: "500" }}>
            {isConnected
              ? "WebSocket Bağlı - Gerçek zamanlı güncellemeler aktif"
              : `WebSocket Bağlantısı Yok (${reconnectAttempts}/20 deneme)`}
          </span>
        </div>

        <div>
          {isConnected && (
            <button
              onClick={onPing}
              style={{
                marginRight: "10px",
                padding: "5px 10px",
                backgroundColor: "#4CAF50",
                color: "white",
                border: "none",
                borderRadius: "4px",
                cursor: "pointer",
                fontSize: "12px",
              }}
            >
              Ping Gönder
            </button>
          )}

          {!isConnected && (
            <button
              onClick={onReconnect}
              style={{
                padding: "5px 10px",
                backgroundColor: "#2196F3",
                color: "white",
                border: "none",
                borderRadius: "4px",
                cursor: "pointer",
                fontSize: "12px",
              }}
            >
              Yeniden Bağlan
            </button>
          )}
        </div>
      </div>

      {error && (
        <div style={{ fontSize: "12px", color: "#D32F2F", marginTop: "5px" }}>
          <strong>Hata:</strong> {error}
        </div>
      )}

      <div style={{ fontSize: "12px", color: "#757575", marginTop: "5px" }}>
        <strong>Bağlantı URL:</strong>{" "}
        {process.env.REACT_APP_WS_URL || "ws://localhost:8005"}
        <br />
        <strong>Tarayıcı:</strong> {navigator.userAgent}
        <br />
        <strong>Son Deneme:</strong> {new Date().toLocaleTimeString()}
      </div>
    </div>
  );
};

const Dashboard = () => {
  const [activeVehicles, setActiveVehicles] = useState([]);
  const [recentActivity, setRecentActivity] = useState([]);
  const [parkingId, setParkingId] = useState(23); // Varsayılan otopark ID'si
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [lastUpdate, setLastUpdate] = useState(new Date());
  const [pingResponse, setPingResponse] = useState(null);

  // Admin WebSocket bağlantısı
  const {
    isConnected,
    messages,
    lastMessage,
    error,
    sendMessage,
    reconnectAttempts,
    reconnect,
  } = useWebSocket("admin");

  // Ping gönderme fonksiyonu
  const handlePing = useCallback(() => {
    if (isConnected) {
      console.log("WebSocket ping gönderiliyor...");
      setPingResponse("Ping gönderildi, yanıt bekleniyor...");

      // Benzersiz ID ile ping mesajı oluştur
      const pingId = Math.random().toString(36).substring(2, 10);
      const pingMessage = {
        type: "ping",
        timestamp: new Date().toISOString(),
        id: pingId,
      };

      sendMessage(JSON.stringify(pingMessage));

      // 5 saniye sonra yanıt gelmezse timeout
      const pingTimeout = setTimeout(() => {
        if (pingResponse === "Ping gönderildi, yanıt bekleniyor...") {
          setPingResponse("Ping zaman aşımı - yanıt alınamadı!");
          console.error("Ping zaman aşımı - yanıt alınamadı!");
        }
      }, 5000);

      return () => clearTimeout(pingTimeout);
    }
  }, [isConnected, sendMessage, pingResponse]);

  // Ping yanıtını işle
  useEffect(() => {
    if (lastMessage && lastMessage.type === "pong") {
      console.log("Ping yanıtı alındı:", lastMessage);

      // Yanıt zamanını hesapla
      const now = new Date();
      const messageTime = lastMessage.timestamp
        ? new Date(lastMessage.timestamp)
        : now;
      const latency = Math.max(0, now - messageTime);

      setPingResponse(`Ping yanıtı alındı! Gecikme: ${latency}ms`);

      // Ping detaylarını konsola yazdır
      console.log("Ping detayları:", {
        gönderimZamanı: messageTime.toISOString(),
        alımZamanı: now.toISOString(),
        gecikme: latency + "ms",
        yanıt: lastMessage,
      });

      // 3 saniye sonra ping yanıtını temizle
      const clearTimeout = setTimeout(() => {
        setPingResponse(null);
      }, 3000);

      return () => clearTimeout(clearTimeout);
    }
  }, [lastMessage]);

  // Verileri yenileme fonksiyonu - sadece ilk yükleme ve otopark değişiminde kullanılacak
  const refreshData = useCallback(async () => {
    setIsLoading(true);
    setLoadError(null);

    try {
      console.log(
        `Aktif araçlar ve son aktiviteler yükleniyor (Otopark ID: ${parkingId})...`
      );

      // API metotlarını kullanarak veri çek
      const [vehiclesData, activitiesData] = await Promise.all([
        api.getActiveVehicles(parkingId),
        api.getRecentActivities(parkingId),
      ]);

      console.log(
        `Aktif araçlar yüklendi (Otopark ID: ${parkingId}):`,
        vehiclesData
      );
      console.log(
        `Son aktiviteler yüklendi (Otopark ID: ${parkingId}):`,
        activitiesData
      );

      // Veri kontrolü ve işleme
      if (Array.isArray(vehiclesData)) {
        setActiveVehicles(vehiclesData);
      } else {
        console.warn("Aktif araçlar verisi dizi değil:", vehiclesData);
        setActiveVehicles([]);
      }

      if (Array.isArray(activitiesData)) {
        setRecentActivity(activitiesData);
      } else {
        console.warn("Son aktiviteler verisi dizi değil:", activitiesData);
        setRecentActivity([]);
      }

      // Son güncelleme zamanını ayarla
      setLastUpdate(new Date());
    } catch (error) {
      console.error("Veriler yüklenirken hata:", error);
      setLoadError(
        "Veriler yüklenirken bir hata oluştu. WebSocket ile güncellemeleri bekliyoruz."
      );
    } finally {
      setIsLoading(false);
    }
  }, [parkingId]);

  // Başlangıçta aktif araç listesini ve son aktiviteleri yükleme
  useEffect(() => {
    refreshData();
  }, [parkingId, refreshData]); // parkingId değiştiğinde veriyi yeniden yükle

  // WebSocket bağlantısı kurulduğunda durum sorgulama
  useEffect(() => {
    if (isConnected) {
      console.log("WebSocket bağlantısı kuruldu, durum sorgulanıyor...");
      sendMessage(JSON.stringify({ type: "status" }));
    }
  }, [isConnected, sendMessage]);

  // Gelen WebSocket mesajlarını işleme
  useEffect(() => {
    if (lastMessage) {
      // Debug: Konsolda mesajı göster
      console.log("WebSocket mesajı alındı:", lastMessage);

      // Mesaj formatını kontrol et
      if (!lastMessage.type) {
        console.error("Geçersiz WebSocket mesajı formatı:", lastMessage);
        return;
      }

      // Mesaj tipine göre işlem yap
      switch (lastMessage.type) {
        case "parking_record_update":
          if (!lastMessage.data) {
            console.error("Mesajda data alanı eksik:", lastMessage);
            return;
          }

          const { data } = lastMessage;
          console.log("Park kaydı güncelleme:", data);

          // Gelen mesajdaki otopark ID'si şu anki seçili otopark ID'si ile uyuşuyor mu kontrol et
          if (data.parking_id && data.parking_id !== parkingId) {
            console.log(
              `Farklı otopark için güncelleme (${data.parking_id}), atlanıyor.`
            );
            return;
          }

          // Aktivite listesini güncelle
          setRecentActivity((prev) => {
            // Son aktivite verisinin formatını kontrol et
            if (!data.id || !data.action) {
              console.error("Geçersiz aktivite verisi:", data);
              return prev;
            }

            // Aynı ID'li kayıt varsa güncelle, yoksa ekle
            const exists = prev.findIndex((item) => item.id === data.id);
            let newActivity = [...prev];

            if (exists >= 0) {
              newActivity[exists] = data;
            } else {
              newActivity = [data, ...prev];
            }

            // En fazla 20 aktivite göster
            return newActivity.slice(0, 20);
          });

          // Araç durumunu güncelle
          if (data.action === "entry") {
            // Yeni araç girişi
            setActiveVehicles((prev) => {
              // Araç zaten listede var mı kontrol et
              const existingIndex = prev.findIndex(
                (v) => v.license_plate === data.license_plate
              );

              if (existingIndex >= 0) {
                // Varsa güncelle
                const updated = [...prev];
                updated[existingIndex] = {
                  id: data.vehicle_id,
                  license_plate: data.license_plate,
                  entry_time: data.entry_time,
                  parking_record_id: data.id,
                };
                return updated;
              } else {
                // Yoksa ekle
                return [
                  {
                    id: data.vehicle_id,
                    license_plate: data.license_plate,
                    entry_time: data.entry_time,
                    parking_record_id: data.id,
                  },
                  ...prev,
                ];
              }
            });

            // Son güncelleme zamanını ayarla
            setLastUpdate(new Date());
          } else if (data.action === "exit") {
            // Araç çıkışı
            setActiveVehicles((prev) =>
              prev.filter((v) => v.license_plate !== data.license_plate)
            );

            // Son güncelleme zamanını ayarla
            setLastUpdate(new Date());
          }
          break;

        case "vehicle_update":
          // Araç güncelleme mesajını işle
          if (!lastMessage.data) {
            console.error("Mesajda data alanı eksik:", lastMessage);
            return;
          }

          const vehicleData = lastMessage.data;
          console.log("Araç güncelleme:", vehicleData);

          // Gelen mesajdaki otopark ID'si şu anki seçili otopark ID'si ile uyuşuyor mu kontrol et
          if (vehicleData.parking_id && vehicleData.parking_id !== parkingId) {
            console.log(
              `Farklı otopark için araç güncellemesi (${vehicleData.parking_id}), atlanıyor.`
            );
            return;
          }

          // Aktif araçlar verisini güncelle
          if (vehicleData.status === "already_parked") {
            console.log("Zaten park edilmiş araç:", vehicleData.license_plate);
          }

          // Verileri yenile - WebSocket mesajı yetersiz olabilir
          refreshData();
          break;

        case "welcome":
          // Karşılama mesajı
          console.log("WebSocket bağlantısı kuruldu:", lastMessage.message);
          break;

        case "status":
          // Durum mesajı
          console.log("WebSocket durum bilgisi:", lastMessage.data);
          break;

        case "error":
          // Hata mesajı
          console.error("WebSocket hata mesajı:", lastMessage.message);
          break;

        default:
          console.log("Bilinmeyen mesaj tipi:", lastMessage.type);
      }
    }
  }, [lastMessage, parkingId, refreshData]);

  // Otopark ID'si değiştiğinde WebSocket bağlantısını ve verileri güncelle
  const handleParkingChange = (e) => {
    const newParkingId = parseInt(e.target.value);
    console.log(`Otopark değişikliği: ${parkingId} -> ${newParkingId}`);

    // Otopark ID'sini güncelle
    setParkingId(newParkingId);

    // Yükleme durumunu güncelle - useEffect tetiklenecek ve verileri yeniden yükleyecek
    setIsLoading(true);

    // Yeni seçilen otopark için WebSocket durumunu güncelle
    if (isConnected) {
      console.log(
        `WebSocket: Otopark ID değişimi bildiriliyor: ${newParkingId}`
      );
      sendMessage(
        JSON.stringify({
          type: "parking_change",
          data: { parking_id: newParkingId },
        })
      );
    }
  };

  // Manuel yenileme işlevi
  const handleManualRefresh = () => {
    console.log("Manuel veri yenileme...");
    refreshData();
  };

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h2>Otopark Yönetim Paneli</h2>
        <div className="parking-selector-container">
          <label
            htmlFor="parking-id"
            style={{ marginRight: "10px", fontWeight: 600, color: "#333" }}
          >
            Otopark Seçimi:
          </label>
          <select
            id="parking-id"
            value={parkingId}
            onChange={handleParkingChange}
            style={{
              padding: "8px",
              borderRadius: "4px",
              border: "1px solid #ccc",
            }}
          >
            <option value="17">Merkez Otopark (ID: 17)</option>
            <option value="23">Milas Otopark (ID: 23)</option>
          </select>

          {/* Yenileme düğmeleri ve bağlantı durumu */}
          <div
            className="dashboard-controls"
            style={{
              marginLeft: "20px",
              display: "flex",
              flexDirection: "column",
              width: "100%",
            }}
          >
            <div style={{ display: "flex", alignItems: "center" }}>
              <button
                onClick={handleManualRefresh}
                style={{
                  padding: "8px 12px",
                  backgroundColor: "#4CAF50",
                  color: "white",
                  border: "none",
                  borderRadius: "4px",
                  cursor: "pointer",
                  marginRight: "10px",
                }}
              >
                Yenile
              </button>
            </div>

            <WebSocketStatus
              isConnected={isConnected}
              error={error}
              reconnectAttempts={reconnectAttempts}
              onReconnect={reconnect}
              onPing={handlePing}
            />
          </div>
        </div>
        <div className="quick-actions" style={{ marginTop: "15px" }}>
          <a href="/manual-entry" className="camera-demo-button">
            <span>Manuel Giriş</span>
          </a>
          <a href="/webcam-demo.html" className="camera-demo-button">
            <span>Kameralı Demo</span>
          </a>
          <a href="/settings" className="camera-demo-button">
            <span>Ayarlar</span>
          </a>
        </div>
      </div>

      {/* WebSocket hata mesajı */}
      {error && (
        <div
          className="error-message"
          style={{
            backgroundColor: "#ffebee",
            padding: "10px",
            borderRadius: "4px",
            margin: "10px 0",
            color: "#d32f2f",
          }}
        >
          <strong>WebSocket Hatası:</strong> {error}
        </div>
      )}

      {/* Yükleme hatası mesajı */}
      {loadError && (
        <div
          className="error-message"
          style={{
            backgroundColor: "#fff8e1",
            padding: "10px",
            borderRadius: "4px",
            margin: "10px 0",
            color: "#ff6f00",
          }}
        >
          <strong>Uyarı:</strong> {loadError}
        </div>
      )}

      {pingResponse && (
        <div
          style={{
            padding: "10px",
            backgroundColor: "#E3F2FD",
            borderRadius: "4px",
            marginBottom: "15px",
            fontSize: "14px",
          }}
        >
          {pingResponse}
        </div>
      )}

      <div className="dashboard-content">
        <div className="dashboard-section">
          <div className="section-header">
            <h3>Aktif Araçlar</h3>
            <span className="count-badge">{activeVehicles.length}</span>
            <span className="update-time">
              Son Güncelleme: {formatDateTime(lastUpdate)}
            </span>
          </div>

          {isLoading ? (
            <div className="loading-spinner">Yükleniyor...</div>
          ) : (
            <div className="active-vehicles">
              {activeVehicles.length === 0 ? (
                <div className="no-data">Aktif araç bulunmuyor</div>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Plaka</th>
                      <th>Giriş Zamanı</th>
                      <th>İşlemler</th>
                    </tr>
                  </thead>
                  <tbody>
                    {activeVehicles.map((vehicle) => (
                      <tr key={vehicle.parking_record_id || vehicle.id}>
                        <td className="license-plate">
                          {vehicle.license_plate}
                        </td>
                        <td>{formatDateTime(vehicle.entry_time)}</td>
                        <td>
                          <button
                            className="action-button exit-button"
                            onClick={() =>
                              api
                                .vehicleExit(vehicle.license_plate, parkingId)
                                .then(() => {
                                  console.log(
                                    `Araç çıkışı yapıldı: ${vehicle.license_plate}`
                                  );
                                })
                                .catch((err) => {
                                  console.error(
                                    `Araç çıkışı sırasında hata: ${err.message}`
                                  );
                                  alert(
                                    `Araç çıkışı yapılamadı: ${err.message}`
                                  );
                                })
                            }
                          >
                            Çıkış Yap
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}
        </div>

        <div className="dashboard-section">
          <div className="section-header">
            <h3>Son Aktiviteler</h3>
            <span className="count-badge">{recentActivity.length}</span>
          </div>

          {isLoading ? (
            <div className="loading-spinner">Yükleniyor...</div>
          ) : (
            <div className="recent-activities">
              {recentActivity.length === 0 ? (
                <div className="no-data">Aktivite bulunmuyor</div>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Plaka</th>
                      <th>İşlem</th>
                      <th>Zaman</th>
                      <th>Detay</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentActivity.map((activity) => (
                      <tr
                        key={activity.id}
                        className={
                          activity.action === "entry"
                            ? "entry-activity"
                            : "exit-activity"
                        }
                      >
                        <td className="license-plate">
                          {activity.license_plate}
                        </td>
                        <td>
                          {activity.action === "entry" ? "Giriş" : "Çıkış"}
                        </td>
                        <td>
                          {activity.action === "entry"
                            ? formatDateTime(activity.entry_time)
                            : formatDateTime(activity.exit_time)}
                        </td>
                        <td>
                          {activity.action === "exit" && activity.parking_fee
                            ? `Ücret: ${
                                activity.parking_fee
                              } TL (${activity.duration_hours?.toFixed(
                                2
                              )} saat)`
                            : ""}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="dashboard-footer">
        {/* Butonlar header bölümüne taşındı */}
      </div>
    </div>
  );
};

export default Dashboard;
