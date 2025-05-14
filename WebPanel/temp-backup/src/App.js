import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import "./App.css";

// Bileşenler
import Header from "./components/Header";
import Dashboard from "./pages/Dashboard";
import VehicleEntry from "./pages/VehicleEntry";
import VehicleExit from "./pages/VehicleExit";
import NotFound from "./pages/NotFound";

function App() {
  return (
    <Router>
      <div className="app">
        <Header />
        <main className="main-content">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/vehicle-entry" element={<VehicleEntry />} />
            <Route path="/vehicle-exit" element={<VehicleExit />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </main>
        <footer className="footer">
          <p>
            &copy; {new Date().getFullYear()} Smart Parking System - Staff Panel
          </p>
        </footer>
      </div>
    </Router>
  );
}

export default App;
