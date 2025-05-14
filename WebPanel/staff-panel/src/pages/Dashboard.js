import React, { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import useWebSocket from "../hooks/useWebSocket";
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

  // Admin WebSocket bağlantısı
  const { isConnected, messages, lastMessage, error } = useWebSocket("admin");

  // Gelen WebSocket mesajlarını işleme
  useEffect(() => {
    if (lastMessage) {
      // Yeni mesaj geldiğinde aktivitelere ekle
      if (lastMessage.type === "parking_record_update") {
        const { data } = lastMessage;

        // Aktivite listesini güncelle
        setRecentActivity((prev) => {
          // En fazla 20 aktivite göster
          const newActivity = [data, ...prev].slice(0, 20);
          return newActivity;
        });

        // Eğer araç girişi bildirimi geldiyse aktif araçları güncelle
        if (data.action === "entry") {
          setActiveVehicles((prev) => {
            // Araç zaten listede var mı kontrol et
            const exists = prev.some(
              (v) => v.license_plate === data.license_plate
            );
            if (!exists) {
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
            return prev;
          });
        }

        // Eğer araç çıkışı bildirimi geldiyse aktif araçlardan çıkar
        if (data.action === "exit") {
          setActiveVehicles((prev) =>
            prev.filter((v) => v.license_plate !== data.license_plate)
          );
        }
      }
    }
  }, [lastMessage]);

  // Otopark ID'si değiştiğinde WebSocket bağlantısını güncellemek için bir yer tutucu
  const handleParkingChange = (e) => {
    setParkingId(parseInt(e.target.value));
  };

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h2>Otopark Yönetim Paneli</h2>
        <div className="parking-selector">
          <label htmlFor="parking-id">Otopark:</label>
          <select
            id="parking-id"
            value={parkingId}
            onChange={handleParkingChange}
          >
            <option value="23">Milas Otopark (ID: 23)</option>
            <option value="17">Merkez Otopark (ID: 17)</option>
          </select>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger">
          <p>{error}</p>
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

          {activeVehicles.length === 0 ? (
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
                  <tr key={vehicle.parking_record_id}>
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

          {recentActivity.length === 0 ? (
            <p className="no-data">Henüz aktivite kaydı bulunmuyor.</p>
          ) : (
            <ul className="activity-list">
              {recentActivity.map((activity, index) => (
                <li
                  key={index}
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
                        {activity.parking_fee.toFixed(2)} TL
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
