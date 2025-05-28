import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../services/api";
import "./VehicleExit.css";

const VehicleExit = () => {
  const [searchParams] = useSearchParams();
  const [licensePlate, setLicensePlate] = useState("");
  const [parkingId, setParkingId] = useState(32); // Default parking ID
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [exitResult, setExitResult] = useState(null);
  const [exitMode, setExitMode] = useState("manual"); // manual or image

  const navigate = useNavigate();

  // Get license plate and parking ID from URL parameters
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

  // Create preview when image file is selected
  const handleImageChange = (e) => {
    const file = e.target.files[0];

    if (file) {
      setImageFile(file);

      // Create file preview
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

  // Form submission for manual exit
  const handleManualSubmit = async (e) => {
    e.preventDefault();

    if (!licensePlate.trim()) {
      setError("License plate is required");
      return;
    }

    setLoading(true);
    setError(null);
    setExitResult(null);

    try {
      const result = await api.vehicleExit(licensePlate.trim(), parkingId);

      if (result.success) {
        setSuccess(`Vehicle exit successful: ${licensePlate}`);
        setExitResult(result);
      } else {
        setError(result.message || "Vehicle exit failed");
      }
    } catch (err) {
      setError("Server error: " + (err.message || "Unknown error"));
    } finally {
      setLoading(false);
    }
  };

  // Form submission for image-based exit
  const handleImageSubmit = async (e) => {
    e.preventDefault();

    if (!imageFile) {
      setError("Please select an image");
      return;
    }

    setLoading(true);
    setError(null);
    setExitResult(null);

    try {
      const result = await api.processPlateForExit(imageFile, parkingId);

      if (result.success) {
        setSuccess(
          `License plate detected from image and exit recorded: ${
            result.license_plate || ""
          }`
        );
        setExitResult(result);
        setImageFile(null);
        setImagePreview(null);
      } else {
        setError(result.message || "Vehicle exit failed");
      }
    } catch (err) {
      setError("Image processing error: " + (err.message || "Unknown error"));
    } finally {
      setLoading(false);
    }
  };

  // Return to home page
  const handleBackToHome = () => {
    navigate("/");
  };

  // Result card showing fee and duration information
  const ResultCard = ({ result }) => {
    if (!result) return null;

    return (
      <div className="result-card">
        <h3>Parking Exit Information</h3>

        <div className="result-info">
          <p>
            <span className="label">License Plate:</span>
            <span className="value">{licensePlate}</span>
          </p>

          <p>
            <span className="label">Entry Time:</span>
            <span className="value">
              {new Date(result.entry_time).toLocaleString("en-US")}
            </span>
          </p>

          <p>
            <span className="label">Exit Time:</span>
            <span className="value">
              {new Date(result.exit_time).toLocaleString("en-US")}
            </span>
          </p>

          <p>
            <span className="label">Parking Duration:</span>
            <span className="value">
              {result.duration_hours.toFixed(2)} hours
            </span>
          </p>

          <p className="fee">
            <span className="label">Fee:</span>
            <span className="value">${result.parking_fee.toFixed(2)}</span>
          </p>
        </div>

        <button
          type="button"
          className="btn btn-primary"
          onClick={handleBackToHome}
        >
          Return to Dashboard
        </button>
      </div>
    );
  };

  return (
    <div className="container vehicle-exit">
      <h2>Vehicle Exit</h2>

      {/* Show result card */}
      {exitResult ? (
        <ResultCard result={exitResult} />
      ) : (
        <>
          <div className="exit-mode-select">
            <button
              className={`mode-btn ${exitMode === "manual" ? "active" : ""}`}
              onClick={() => setExitMode("manual")}
            >
              Manual Exit
            </button>
            <button
              className={`mode-btn ${exitMode === "image" ? "active" : ""}`}
              onClick={() => setExitMode("image")}
            >
              Exit with Image
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
            <label htmlFor="parking-id">Parking:</label>
            <select
              id="parking-id"
              value={parkingId}
              onChange={(e) => setParkingId(parseInt(e.target.value))}
              disabled={loading}
            >
              <option value="35">Kaleici Parking (ID: 35)</option>
              <option value="34">Otopark Antalya Parking (ID: 34)</option>
              <option value="33">Şişli Park Parking (ID: 33)</option>
              <option value="32">Sabiha Gökçen Parking (ID: 32)</option>
            </select>
          </div>

          {exitMode === "manual" ? (
            <form onSubmit={handleManualSubmit} className="exit-form">
              <div className="form-group">
                <label htmlFor="license-plate">License Plate</label>
                <input
                  type="text"
                  id="license-plate"
                  className="form-control"
                  value={licensePlate}
                  onChange={(e) =>
                    setLicensePlate(e.target.value.toUpperCase())
                  }
                  placeholder="e.g. 34ABC123"
                  disabled={loading}
                />
              </div>

              <div className="form-group">
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={loading}
                >
                  {loading ? "Processing..." : "Record Vehicle Exit"}
                </button>
                <button
                  type="button"
                  className="btn"
                  onClick={() => navigate("/")}
                  disabled={loading}
                >
                  Cancel
                </button>
              </div>
            </form>
          ) : (
            <form onSubmit={handleImageSubmit} className="exit-form">
              <div className="form-group">
                <label htmlFor="image-file">License Plate Image</label>
                <input
                  type="file"
                  id="image-file"
                  className="form-control"
                  onChange={handleImageChange}
                  accept="image/*"
                  disabled={loading}
                />
              </div>

              {imagePreview && (
                <div className="image-preview">
                  <img src={imagePreview} alt="License plate preview" />
                </div>
              )}

              <div className="form-group">
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={loading || !imageFile}
                >
                  {loading
                    ? "Processing Image..."
                    : "Process Image & Record Exit"}
                </button>
                <button
                  type="button"
                  className="btn"
                  onClick={() => navigate("/")}
                  disabled={loading}
                >
                  Cancel
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
