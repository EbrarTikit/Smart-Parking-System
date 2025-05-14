import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../services/api";
import "./VehicleExit.css";

const VehicleExit = () => {
  const [searchParams] = useSearchParams();
  const [licensePlate, setLicensePlate] = useState("");
  const [parkingId, setParkingId] = useState(23); // Varsayılan otopark ID'si
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [exitResult, setExitResult] = useState(null);
  const [exitMode, setExitMode] = useState("manual"); // manual veya image

  const navigate = useNavigate();

  // URL parametrelerinden plaka ve otopark ID'sini al
  useEffect(() => {
    const plateFromUrl = searchParams.get("plate");
    const parkingFromUrl = searchParams.get("parking");

    if (plateFromUrl) {
      setLicensePlate(plateFromUrl);
    }

    if (parkingFromUrl) {
      setParkingId(parseInt(parkingFromUrl));
    }
  }, [searchParams]);

  // Image dosyası seçildiğinde önizleme oluşturma
  const handleImageChange = (e) => {
    const file = e.target.files[0];

    if (file) {
      setImageFile(file);

      // Dosya önizlemesi oluştur
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result);
      };
      reader.readAsDataURL(file);
    } else {
      setImageFile(null);
      setImagePreview(null);
    }
  };

  // Manuel çıkış için form gönderimi
  const handleManualSubmit = async (e) => {
    e.preventDefault();

    if (!licensePlate.trim()) {
      setError("Plaka bilgisi gereklidir");
      return;
    }

    setLoading(true);
    setError(null);
    setExitResult(null);

    try {
      const result = await api.vehicleExit(licensePlate.trim(), parkingId);

      if (result.success) {
        setSuccess(`Araç çıkışı başarılı: ${licensePlate}`);
        setExitResult(result);
      } else {
        setError(result.message || "Araç çıkışı yapılamadı");
      }
    } catch (err) {
      setError("Sunucu hatası: " + (err.message || "Bilinmeyen hata"));
    } finally {
      setLoading(false);
    }
  };

  // Görüntü ile çıkış için form gönderimi
  const handleImageSubmit = async (e) => {
    e.preventDefault();

    if (!imageFile) {
      setError("Lütfen bir görüntü seçin");
      return;
    }

    setLoading(true);
    setError(null);
    setExitResult(null);

    try {
      const result = await api.processPlateForExit(imageFile, parkingId);

      if (result.success) {
        setSuccess(
          `Görüntüden plaka tespit edildi ve çıkış yapıldı: ${
            result.license_plate || ""
          }`
        );
        setExitResult(result);
        setImageFile(null);
        setImagePreview(null);
      } else {
        setError(result.message || "Araç çıkışı yapılamadı");
      }
    } catch (err) {
      setError("Görüntü işleme hatası: " + (err.message || "Bilinmeyen hata"));
    } finally {
      setLoading(false);
    }
  };

  // Ana sayfaya dön
  const handleBackToHome = () => {
    navigate("/");
  };

  // Ücret ve süre bilgilerini gösteren sonuç kartı
  const ResultCard = ({ result }) => {
    if (!result) return null;

    return (
      <div className="result-card">
        <h3>Otopark Çıkış Bilgileri</h3>

        <div className="result-info">
          <p>
            <span className="label">Plaka:</span>
            <span className="value">{licensePlate}</span>
          </p>

          <p>
            <span className="label">Giriş Zamanı:</span>
            <span className="value">
              {new Date(result.entry_time).toLocaleString("tr-TR")}
            </span>
          </p>

          <p>
            <span className="label">Çıkış Zamanı:</span>
            <span className="value">
              {new Date(result.exit_time).toLocaleString("tr-TR")}
            </span>
          </p>

          <p>
            <span className="label">Park Süresi:</span>
            <span className="value">
              {result.duration_hours.toFixed(2)} saat
            </span>
          </p>

          <p className="fee">
            <span className="label">Ücret:</span>
            <span className="value">{result.parking_fee.toFixed(2)} TL</span>
          </p>
        </div>

        <button
          type="button"
          className="btn btn-primary"
          onClick={handleBackToHome}
        >
          Ana Sayfaya Dön
        </button>
      </div>
    );
  };

  return (
    <div className="container vehicle-exit">
      <h2>Araç Çıkışı</h2>

      {/* Sonuç kartını göster */}
      {exitResult ? (
        <ResultCard result={exitResult} />
      ) : (
        <>
          <div className="exit-mode-select">
            <button
              className={`mode-btn ${exitMode === "manual" ? "active" : ""}`}
              onClick={() => setExitMode("manual")}
            >
              Manuel Çıkış
            </button>
            <button
              className={`mode-btn ${exitMode === "image" ? "active" : ""}`}
              onClick={() => setExitMode("image")}
            >
              Görüntü ile Çıkış
            </button>
          </div>

          {error && (
            <div className="alert alert-danger">
              <p>{error}</p>
            </div>
          )}

          {success && (
            <div className="alert alert-success">
              <p>{success}</p>
            </div>
          )}

          <div className="parking-selector">
            <label htmlFor="parking-id">Otopark:</label>
            <select
              id="parking-id"
              value={parkingId}
              onChange={(e) => setParkingId(parseInt(e.target.value))}
              disabled={loading}
            >
              <option value="23">Milas Otopark (ID: 23)</option>
              <option value="17">Merkez Otopark (ID: 17)</option>
            </select>
          </div>

          {exitMode === "manual" ? (
            <form onSubmit={handleManualSubmit} className="exit-form">
              <div className="form-group">
                <label htmlFor="license-plate">Plaka</label>
                <input
                  type="text"
                  id="license-plate"
                  className="form-control"
                  value={licensePlate}
                  onChange={(e) =>
                    setLicensePlate(e.target.value.toUpperCase())
                  }
                  placeholder="Örn: 34ABC123"
                  disabled={loading}
                />
              </div>

              <div className="form-group">
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={loading}
                >
                  {loading ? "İşleniyor..." : "Araç Çıkışı Yap"}
                </button>
                <button
                  type="button"
                  className="btn"
                  onClick={() => navigate("/")}
                  disabled={loading}
                >
                  İptal
                </button>
              </div>
            </form>
          ) : (
            <form onSubmit={handleImageSubmit} className="exit-form">
              <div className="form-group">
                <label htmlFor="image-file">Plaka Görüntüsü</label>
                <input
                  type="file"
                  id="image-file"
                  className="form-control"
                  accept="image/*"
                  onChange={handleImageChange}
                  disabled={loading}
                />

                {imagePreview && (
                  <div className="image-preview">
                    <img src={imagePreview} alt="Plaka Önizleme" />
                  </div>
                )}
              </div>

              <div className="form-group">
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={loading || !imageFile}
                >
                  {loading
                    ? "Görüntü İşleniyor..."
                    : "Görüntüden Plaka Tanı ve Çıkış Yap"}
                </button>
                <button
                  type="button"
                  className="btn"
                  onClick={() => navigate("/")}
                  disabled={loading}
                >
                  İptal
                </button>
              </div>
            </form>
          )}
        </>
      )}
    </div>
  );
};

export default VehicleExit;
