import React, { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import useWebSocket from "../hooks/useWebSocket";
import { api } from "../services/api";
import "./Dashboard.css";

// Helper function to format dates
const formatDateTime = (dateString) => {
  if (!dateString) return "";
  const date = new Date(dateString);
  return date.toLocaleString("en-US", {
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
  const [parkingId, setParkingId] = useState(32); // Default parking ID
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  // Admin WebSocket connection
  const { isConnected, messages, lastMessage, error, sendMessage } =
    useWebSocket("admin");

  // Data refresh function
  const fetchData = useCallback(async () => {
    try {
      console.log(`Refreshing data (Parking ID: ${parkingId})...`);

      const [vehiclesData, activitiesData] = await Promise.all([
        api.getActiveVehicles(parkingId),
        api.getRecentActivities(parkingId),
      ]);

      if (Array.isArray(vehiclesData)) {
        setActiveVehicles(vehiclesData);
        console.log(
          `Active vehicles updated (${vehiclesData.length} vehicles)`
        );
      }

      if (Array.isArray(activitiesData)) {
        setRecentActivity(activitiesData);
        console.log(
          `Recent activities updated (${activitiesData.length} activities)`
        );
      }

      console.log("Data successfully updated");
    } catch (error) {
      console.error("Data refresh error:", error);
    }
  }, [parkingId]);

  // Initially load active vehicles and recent activities
  useEffect(() => {
    setIsLoading(true);
    setLoadError(null);

    fetchData()
      .catch((error) => {
        console.error("Error loading initial data:", error);
        setLoadError(
          "An error occurred while loading initial data. Waiting for WebSocket updates."
        );
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [parkingId, fetchData]); // Reload data when parkingId changes

  // Query status when WebSocket connection is established
  useEffect(() => {
    if (isConnected) {
      console.log("WebSocket connection established, querying status...");
      sendMessage(JSON.stringify({ type: "status" }));
    }
  }, [isConnected, sendMessage]);

  // Process incoming WebSocket messages
  useEffect(() => {
    if (lastMessage) {
      // Debug: Show message in console
      console.log("WebSocket message received:", lastMessage);

      // Check message format
      if (!lastMessage.type) {
        console.error("Invalid WebSocket message format:", lastMessage);
        return;
      }

      // Process based on message type
      switch (lastMessage.type) {
        case "parking_record_update":
          if (!lastMessage.data) {
            console.error("Message missing data field:", lastMessage);
            return;
          }

          const { data } = lastMessage;
          console.log("Parking record update:", data);

          // Refresh data - get current data directly from API instead of WebSocket message
          // This prevents potential data inconsistencies
          fetchData();
          break;

        case "vehicle_update":
          // Process vehicle update message
          if (!lastMessage.data) {
            console.error("Message missing data field:", lastMessage);
            return;
          }

          const vehicleData = lastMessage.data;
          console.log("Vehicle update:", vehicleData);

          // Refresh data
          fetchData();
          break;

        case "welcome":
          // Welcome message
          console.log("WebSocket connection established:", lastMessage.message);
          break;

        case "status":
          // Status message
          console.log("WebSocket status information:", lastMessage.data);
          break;

        case "error":
          // Error message
          console.error("WebSocket error message:", lastMessage.message);
          break;

        case "parking_info":
          // Parking information message - from Webcam demo
          console.log("Parking information:", lastMessage.data);
          if (lastMessage.data && lastMessage.data.parking_id) {
            const newParkingId = parseInt(lastMessage.data.parking_id);
            console.log(
              `Parking change detected: ${parkingId} -> ${newParkingId}`
            );

            // Update if different from current parking ID
            if (newParkingId !== parkingId) {
              setParkingId(newParkingId);
            } else {
              // Refresh data even if parking ID is the same
              console.log("Refreshing data for the same parking...");
              fetchData();
            }
          }
          break;

        default:
          console.log("Unknown message type:", lastMessage.type);
      }
    }
  }, [lastMessage, parkingId, fetchData]);

  // Update WebSocket connection and data when parking ID changes
  const handleParkingChange = (e) => {
    const newParkingId = parseInt(e.target.value);
    console.log(`Parking change: ${parkingId} -> ${newParkingId}`);

    // Update parking ID
    setParkingId(newParkingId);

    // Update loading status - useEffect will trigger and reload data
    setIsLoading(true);

    // Update WebSocket status for newly selected parking
    if (isConnected) {
      console.log(`WebSocket: Notifying parking ID change: ${newParkingId}`);
      sendMessage(
        JSON.stringify({
          type: "parking_record_update",
          data: {
            parking_id: newParkingId,
            action: "change",
            message: `Parking change: ${newParkingId}`,
          },
        })
      );
    }

    // Note: API calls are now triggered by useEffect when parkingId changes
  };

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h2>Parking Management Dashboard</h2>
        <div className="parking-selector-container">
          <label
            htmlFor="parking-id"
            style={{ marginRight: "10px", fontWeight: 600, color: "#333" }}
          >
            Parking:
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
              <option value="35">Kaleici Parking (ID: 35)</option>
              <option value="34">Otopark Antalya Parking (ID: 34)</option>
              <option value="33">Şişli Park Parking (ID: 33)</option>
              <option value="32">Sabiha Gökçen Parking (ID: 32)</option>
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
          <span>Camera Demo</span>
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
        WebSocket Status: {isConnected ? "Connected" : "Disconnected"}
      </div>

      <div className="dashboard-grid">
        <div className="container active-vehicles">
          <div className="section-header">
            <h3>Active Vehicles</h3>
            <span className="badge">{activeVehicles.length}</span>
          </div>

          {isLoading ? (
            <p className="loading">Loading...</p>
          ) : activeVehicles.length === 0 ? (
            <p className="no-data">No active vehicles in the parking lot.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>License Plate</th>
                  <th>Entry Time</th>
                  <th>Actions</th>
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
                        Exit
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <div className="action-buttons">
            <Link to="/vehicle-entry" className="btn btn-success">
              New Vehicle Entry
            </Link>
          </div>
        </div>

        <div className="container recent-activity">
          <h3>Recent Activities</h3>

          {isLoading ? (
            <p className="loading">Loading...</p>
          ) : recentActivity.length === 0 ? (
            <p className="no-data">No activity records yet.</p>
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
                        Duration: {activity.duration_hours} hours, Fee:{" "}
                        {activity.parking_fee && activity.parking_fee.toFixed
                          ? activity.parking_fee.toFixed(2)
                          : 0}{" "}
                        $
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
