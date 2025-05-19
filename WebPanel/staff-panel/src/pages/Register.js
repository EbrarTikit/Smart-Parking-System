import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../services/api";
import "./Register.css";

const Register = ({ isAuthenticated }) => {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    // Kullanıcı zaten giriş yapmışsa dashboard'a yönlendir
    if (isAuthenticated) {
      navigate("/");
    }
  }, [isAuthenticated, navigate]);

  const validateForm = () => {
    // Form validasyonu
    if (!username || !email || !password || !confirmPassword) {
      setError("Tüm alanları doldurunuz");
      return false;
    }

    if (password !== confirmPassword) {
      setError("Şifreler eşleşmiyor");
      return false;
    }

    // Email validasyonu
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setError("Geçerli bir email adresi giriniz");
      return false;
    }

    // Şifre uzunluğu kontrolü
    if (password.length < 6) {
      setError("Şifre en az 6 karakter olmalıdır");
      return false;
    }

    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!validateForm()) {
      return;
    }

    setLoading(true);

    try {
      // API üzerinden kayıt işlemi
      await api.register(username, email, password);

      // Başarılı kayıt sonrası login sayfasına yönlendir
      navigate("/login", {
        state: { message: "Kayıt başarılı! Şimdi giriş yapabilirsiniz." },
      });
    } catch (err) {
      setError(
        err.message || "Kayıt işlemi başarısız. Lütfen tekrar deneyiniz."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="register-container">
      <div className="register-card">
        <div className="register-header">
          <div className="logo-container">
            <div className="logo">
              <i className="parking-icon">P</i>
            </div>
          </div>
          <h2>Smart Parking Sistemi</h2>
          <p>Yeni Hesap Oluşturun</p>
        </div>

        {error && (
          <div className="error-message">
            <i className="error-icon">!</i> {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="register-form">
          <div className="form-group">
            <label htmlFor="username">
              <i className="input-icon">👤</i> Kullanıcı Adı
            </label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={loading}
              placeholder="Kullanıcı adınızı girin"
              autoComplete="username"
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">
              <i className="input-icon">✉️</i> Email
            </label>
            <input
              type="email"
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={loading}
              placeholder="Email adresinizi girin"
              autoComplete="email"
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">
              <i className="input-icon">🔒</i> Şifre
            </label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
              placeholder="Şifrenizi girin"
              autoComplete="new-password"
            />
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">
              <i className="input-icon">🔐</i> Şifre Tekrar
            </label>
            <input
              type="password"
              id="confirmPassword"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              disabled={loading}
              placeholder="Şifrenizi tekrar girin"
              autoComplete="new-password"
            />
          </div>

          <button type="submit" className="register-button" disabled={loading}>
            {loading ? (
              <span className="loading-spinner">
                <span className="spinner"></span> Kayıt Yapılıyor...
              </span>
            ) : (
              "Kayıt Ol"
            )}
          </button>
        </form>

        <div className="register-footer">
          Zaten bir hesabınız var mı? <Link to="/login">Giriş Yap</Link>
        </div>
      </div>
    </div>
  );
};

export default Register;
