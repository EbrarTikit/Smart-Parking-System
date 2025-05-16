import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../services/api";
import "./VehicleEntry.css";

const VehicleEntry = () => {
  const [searchParams] = useSearchParams();
  const [licensePlate, setLicensePlate] = useState("");
  const [parkingId, setParkingId] = useState(17); // Varsayılan otopark ID'si (Merkez Otopark)
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [entryMode, setEntryMode] = useState("manual"); // manual veya image

  const navigate = useNavigate();

  // URL parametrelerinden otopark ID'sini al
  useEffect(() => {
    const parkingFromUrl = searchParams.get("parking");

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

  // Manuel giriş için form gönderimi
  const handleManualSubmit = async (e) => {
    e.preventDefault();

    if (!licensePlate.trim()) {
      setError("Plaka bilgisi gereklidir");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const result = await api.vehicleEntry(licensePlate.trim(), parkingId);

      if (result.success) {
        setSuccess(`Araç girişi başarılı: ${licensePlate}`);
        setLicensePlate("");

        // 2 saniye sonra ana sayfaya yönlendir
        setTimeout(() => {
          navigate("/");
        }, 2000);
      } else {
        setError(result.message || "Araç girişi yapılamadı");
      }
    } catch (err) {
      setError("Sunucu hatası: " + (err.message || "Bilinmeyen hata"));
    } finally {
      setLoading(false);
    }
  };

  // Görüntü ile giriş için form gönderimi
  const handleImageSubmit = async (e) => {
    e.preventDefault();

    if (!imageFile) {
      setError("Lütfen bir görüntü seçin");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const result = await api.processPlateForEntry(imageFile, parkingId);

      if (result.success) {
        setSuccess(
          `Görüntüden plaka tespit edildi ve giriş yapıldı: ${result.vehicle.license_plate}`
        );
        setImageFile(null);
        setImagePreview(null);

        // 2 saniye sonra ana sayfaya yönlendir
        setTimeout(() => {
          navigate("/");
        }, 2000);
      } else {
        setError(result.message || "Araç girişi yapılamadı");
      }
    } catch (err) {
      setError("Görüntü işleme hatası: " + (err.message || "Bilinmeyen hata"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container vehicle-entry">
      <h2>Araç Girişi</h2>

      <div className="entry-mode-select">
        <button
          className={`mode-btn ${entryMode === "manual" ? "active" : ""}`}
          onClick={() => setEntryMode("manual")}
        >
          Manuel Giriş
        </button>
        <button
          className={`mode-btn ${entryMode === "image" ? "active" : ""}`}
          onClick={() => setEntryMode("image")}
        >
          Görüntü ile Giriş
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
          <option value="17">Merkez Otopark (ID: 17)</option>
          <option value="23">Milas Otopark (ID: 23)</option>
        </select>
      </div>

      {entryMode === "manual" ? (
        <form onSubmit={handleManualSubmit} className="entry-form">
          <div className="form-group">
            <label htmlFor="license-plate">Plaka</label>
            <input
              type="text"
              id="license-plate"
              className="form-control"
              value={licensePlate}
              onChange={(e) => setLicensePlate(e.target.value.toUpperCase())}
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
              {loading ? "İşleniyor..." : "Araç Girişi Kaydet"}
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
        <form onSubmit={handleImageSubmit} className="entry-form">
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
                : "Görüntüden Plaka Tanı ve Kaydet"}
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
    </div>
  );
};

export default VehicleEntry;
