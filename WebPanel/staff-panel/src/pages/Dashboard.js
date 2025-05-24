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

const Dashboard = () => {
  const [activeVehicles, setActiveVehicles] = useState([]);
  const [recentActivity, setRecentActivity] = useState([]);
  const [parkingId, setParkingId] = useState(23); // Varsayılan otopark ID'si
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  // Admin WebSocket bağlantısı
  const { isConnected, messages, lastMessage, error, sendMessage } =
    useWebSocket("admin");

  // Veri yenileme fonksiyonu
  const fetchData = useCallback(async () => {
    try {
      console.log(`Veriler yenileniyor (Otopark ID: ${parkingId})...`);

      const [vehiclesData, activitiesData] = await Promise.all([
        api.getActiveVehicles(parkingId),
        api.getRecentActivities(parkingId),
      ]);

      if (Array.isArray(vehiclesData)) {
        setActiveVehicles(vehiclesData);
        console.log(`Aktif araçlar güncellendi (${vehiclesData.length} araç)`);
      }

      if (Array.isArray(activitiesData)) {
        setRecentActivity(activitiesData);
        console.log(
          `Son aktiviteler güncellendi (${activitiesData.length} aktivite)`
        );
      }

      console.log("Veriler başarıyla güncellendi");
    } catch (error) {
      console.error("Veri yenileme hatası:", error);
    }
  }, [parkingId]);

  // Başlangıçta aktif araç listesini ve son aktiviteleri yükleme
  useEffect(() => {
    setIsLoading(true);
    setLoadError(null);

    fetchData()
      .catch((error) => {
        console.error("Başlangıç verileri yüklenirken hata:", error);
        setLoadError(
          "Başlangıç verileri yüklenirken bir hata oluştu. WebSocket ile güncellemeleri bekliyoruz."
        );
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [parkingId, fetchData]); // parkingId değiştiğinde veriyi yeniden yükle

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

          // Veriyi yenile - WebSocket mesajı yerine doğrudan API'den güncel veriyi çekelim
          // Bu, olası veri tutarsızlıklarını önler
          fetchData();
          break;

        case "vehicle_update":
          // Araç güncelleme mesajını işle
          if (!lastMessage.data) {
            console.error("Mesajda data alanı eksik:", lastMessage);
            return;
          }

          const vehicleData = lastMessage.data;
          console.log("Araç güncelleme:", vehicleData);

          // Veriyi yenile
          fetchData();
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

        case "parking_info":
          // Otopark bilgisi mesajı - Webcam demo'dan gelen bilgiler
          console.log("Otopark bilgisi:", lastMessage.data);
          if (lastMessage.data && lastMessage.data.parking_id) {
            const newParkingId = parseInt(lastMessage.data.parking_id);
            console.log(
              `Otopark değişikliği tespit edildi: ${parkingId} -> ${newParkingId}`
            );

            // Eğer mevcut otopark ID'sinden farklıysa güncelle
            if (newParkingId !== parkingId) {
              setParkingId(newParkingId);
            } else {
              // Aynı otopark ID'si olsa bile verileri yenile
              console.log("Aynı otopark için veri yenileniyor...");
              fetchData();
            }
          }
          break;

        default:
          console.log("Bilinmeyen mesaj tipi:", lastMessage.type);
      }
    }
  }, [lastMessage, parkingId, fetchData]);

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
          type: "parking_record_update",
          data: {
            parking_id: newParkingId,
            action: "change",
            message: `Otopark değişikliği: ${newParkingId}`,
          },
        })
      );
    }

    // Not: API çağrıları artık useEffect içinde parkingId değişimi ile tetikleniyor
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
            Otopark:
          </label>
          <div
            className="custom-select"
            style={{
              position: "relative",
              display: "inline-block",
              width: "250px",
            }}
          >
            <select
              id="parking-id"
              value={parkingId}
              onChange={handleParkingChange}
              style={{
                display: "block",
                width: "100%",
                padding: "10px 15px",
                fontSize: "14px",
                fontWeight: "500",
                color: "#333",
                backgroundColor: "#fff",
                border: "1px solid #ddd",
                borderRadius: "5px",
                appearance: "none",
                WebkitAppearance: "none",
                MozAppearance: "none",
                cursor: "pointer",
                boxShadow: "0 2px 4px rgba(0, 0, 0, 0.05)",
              }}
            >
              <option value="23">Milas Otopark (ID: 23)</option>
              <option value="17">Merkez Otopark (ID: 17)</option>
            </select>
            <div
              style={{
                position: "absolute",
                top: "50%",
                right: "15px",
                transform: "translateY(-50%)",
                fontSize: "10px",
                color: "#666",
                pointerEvents: "none",
              }}
            >
              ▼
            </div>
          </div>
        </div>
        <a href="/webcam-demo.html" className="camera-demo-button">
          <span>Kameralı Demo</span>
        </a>
      </div>

      {error && (
        <div className="alert alert-danger">
          <p>{error}</p>
        </div>
      )}

      {loadError && (
        <div className="alert alert-warning">
          <p>{loadError}</p>
        </div>
      )}

      <div className="connection-status">
        <span
          className={`status-indicator ${
            isConnected ? "connected" : "disconnected"
          }`}
        ></span>
        WebSocket Durumu: {isConnected ? "Bağlı" : "Bağlı Değil"}
      </div>

      <div className="dashboard-grid">
        <div className="container active-vehicles">
          <div className="section-header">
            <h3>Aktif Araçlar</h3>
            <span className="badge">{activeVehicles.length}</span>
          </div>

          {isLoading ? (
            <p className="loading">Yükleniyor...</p>
          ) : activeVehicles.length === 0 ? (
            <p className="no-data">Otoparkta aktif araç bulunmuyor.</p>
          ) : (
            <table className="table">
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
                    <td>{vehicle.license_plate}</td>
                    <td>{formatDateTime(vehicle.entry_time)}</td>
                    <td>
                      <Link
                        to={`/vehicle-exit?plate=${vehicle.license_plate}&parking=${parkingId}`}
                        className="btn btn-sm btn-primary"
                      >
                        Çıkış
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <div className="action-buttons">
            <Link to="/vehicle-entry" className="btn btn-success">
              Yeni Araç Girişi
            </Link>
          </div>
        </div>

        <div className="container recent-activity">
          <h3>Son Aktiviteler</h3>

          {isLoading ? (
            <p className="loading">Yükleniyor...</p>
          ) : recentActivity.length === 0 ? (
            <p className="no-data">Henüz aktivite kaydı bulunmuyor.</p>
          ) : (
            <ul className="activity-list">
              {recentActivity.map((activity, index) => (
                <li
                  key={activity.id || index}
                  className={`activity-item ${
                    activity.action === "entry" ? "entry" : "exit"
                  }`}
                >
                  <div className="activity-icon">
                    {activity.action === "entry" ? (
                      <i className="fas fa-sign-in-alt"></i>
                    ) : (
                      <i className="fas fa-sign-out-alt"></i>
                    )}
                  </div>
                  <div className="activity-content">
                    <p className="activity-message">{activity.message}</p>
                    <p className="activity-time">
                      {activity.action === "entry"
                        ? formatDateTime(activity.entry_time)
                        : formatDateTime(activity.exit_time)}
                    </p>
                    {activity.action === "exit" && (
                      <p className="activity-details">
                        Süre: {activity.duration_hours} saat, Ücret:{" "}
                        {activity.parking_fee && activity.parking_fee.toFixed
                          ? activity.parking_fee.toFixed(2)
                          : 0}{" "}
                        TL
                      </p>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
