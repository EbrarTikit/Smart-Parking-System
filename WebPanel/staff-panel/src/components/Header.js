import React from "react";
import { Link, useLocation } from "react-router-dom";
import "./Header.css";

const Header = () => {
  const location = useLocation();

  return (
    <header className="header">
      <div className="header-container">
        <div className="logo">
          <Link to="/">
            <h1>Smart Parking</h1>
            <span>Staff Panel</span>
          </Link>
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
      </div>
    </header>
  );
};

export default Header;
