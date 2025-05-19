import React, { useState, useEffect } from "react";
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Navigate,
} from "react-router-dom";
import "./App.css";

// Bileşenler
import Header from "./components/Header";
import Dashboard from "./pages/Dashboard";
import VehicleEntry from "./pages/VehicleEntry";
import VehicleExit from "./pages/VehicleExit";
import NotFound from "./pages/NotFound";
import Login from "./pages/Login";
import Register from "./pages/Register";
import { api } from "./services/api";

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Sayfa yüklendiğinde kimlik doğrulama durumunu kontrol et
    const checkAuthStatus = () => {
      const authStatus = api.isAuthenticated();
      setIsAuthenticated(authStatus);
      setIsLoading(false);
    };

    checkAuthStatus();
  }, []);

  // Kimlik doğrulama gerektiren Route için özel bileşen
  const ProtectedRoute = ({ children }) => {
    if (isLoading) {
      return <div className="loading-container">Yükleniyor...</div>;
    }

    if (!isAuthenticated) {
      return <Navigate to="/login" replace />;
    }

    return children;
  };

  return (
    <Router>
      <div className="app">
        {isAuthenticated && <Header />}
        <main
          className={`main-content ${!isAuthenticated ? "full-height" : ""}`}
        >
          <Routes>
            {/* Kimlik doğrulama gerektirmeyen rotalar */}
            <Route
              path="/login"
              element={
                isAuthenticated ? (
                  <Navigate to="/" replace />
                ) : (
                  <Login setIsAuthenticated={setIsAuthenticated} />
                )
              }
            />
            <Route
              path="/register"
              element={
                isAuthenticated ? (
                  <Navigate to="/" replace />
                ) : (
                  <Register isAuthenticated={isAuthenticated} />
                )
              }
            />

            {/* Kimlik doğrulama gerektiren rotalar */}
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/vehicle-entry"
              element={
                <ProtectedRoute>
                  <VehicleEntry />
                </ProtectedRoute>
              }
            />
            <Route
              path="/vehicle-exit"
              element={
                <ProtectedRoute>
                  <VehicleExit />
                </ProtectedRoute>
              }
            />

            {/* 404 sayfası */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </main>
        {isAuthenticated && (
          <footer className="footer">
            <p>
              &copy; {new Date().getFullYear()} Smart Parking System - Staff
              Panel
            </p>
          </footer>
        )}
      </div>
    </Router>
  );
}

export default App;
