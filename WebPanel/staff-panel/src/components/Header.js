import React, { useState, useEffect } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { api } from "../services/api";
import "./Header.css";

const Header = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");

  useEffect(() => {
    // Kullanıcı adını almak için
    const fetchUserInfo = async () => {
      try {
        const userId = localStorage.getItem("userId");
        if (userId) {
          const userInfo = await api.getUserInfo(userId);
          if (userInfo && userInfo.username) {
            setUsername(userInfo.username);
          }
        }
      } catch (error) {
        console.error("Kullanıcı bilgileri alınamadı:", error);
      }
    };

    fetchUserInfo();
  }, []);

  const handleLogout = () => {
    api.logout();
    // Sayfayı yenile ve giriş sayfasına yönlendir
    window.location.href = "/login";
  };

  return (
    <header className="header">
      <div className="header-container">
        <div className="brand">
          <div className="logo">
            <Link to="/">
              <h1>SP</h1>
            </Link>
          </div>
          <div className="brand-text">
            <h2>Smart Parking</h2>
            <span>Staff Panel</span>
          </div>
        </div>
        <nav className="nav">
          <ul className="nav-list">
            <li className={location.pathname === "/" ? "active" : ""}>
              <Link to="/">Dashboard</Link>
            </li>
            <li
              className={location.pathname === "/vehicle-entry" ? "active" : ""}
            >
              <Link to="/vehicle-entry">Araç Girişi</Link>
            </li>
            <li
              className={location.pathname === "/vehicle-exit" ? "active" : ""}
            >
              <Link to="/vehicle-exit">Araç Çıkışı</Link>
            </li>
          </ul>
        </nav>

        <div className="user-menu">
          {username && <span className="username">Merhaba, {username}</span>}
          <button className="logout-button" onClick={handleLogout}>
            Çıkış Yap
          </button>
        </div>
      </div>
    </header>
  );
};

export default Header;
