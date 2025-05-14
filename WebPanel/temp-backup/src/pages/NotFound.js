import React from "react";
import { Link } from "react-router-dom";
import "./NotFound.css";

const NotFound = () => {
  return (
    <div className="not-found">
      <div className="not-found-content">
        <h1>404</h1>
        <h2>Sayfa Bulunamadı</h2>
        <p>Aradığınız sayfa bulunmamaktadır.</p>
        <Link to="/" className="home-link">
          Ana Sayfaya Dön
        </Link>
      </div>
    </div>
  );
};

export default NotFound;
