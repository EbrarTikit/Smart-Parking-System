import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../services/api";
import "./VehicleEntry.css";

const VehicleEntry = () => {
  const [searchParams] = useSearchParams();
  const [licensePlate, setLicensePlate] = useState("");
  const [parkingId, setParkingId] = useState(32); // Default parking ID (Sabiha Gökçen Parking)
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [entryMode, setEntryMode] = useState("manual"); // manual or image

  const navigate = useNavigate();

  // Get parking ID from URL parameters
  useEffect(() => {
    const parkingFromUrl = searchParams.get("parking");

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

  // Form submission for manual entry
  const handleManualSubmit = async (e) => {
    e.preventDefault();

    if (!licensePlate.trim()) {
      setError("License plate is required");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const result = await api.vehicleEntry(licensePlate.trim(), parkingId);

      if (result.success) {
        setSuccess(`Vehicle entry successful: ${licensePlate}`);
        setLicensePlate("");

        // Redirect to home page after 2 seconds
        setTimeout(() => {
          navigate("/");
        }, 2000);
      } else {
        setError(result.message || "Vehicle entry failed");
      }
    } catch (err) {
      setError("Server error: " + (err.message || "Unknown error"));
    } finally {
      setLoading(false);
    }
  };

  // Form submission for image-based entry
  const handleImageSubmit = async (e) => {
    e.preventDefault();

    if (!imageFile) {
      setError("Please select an image");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const result = await api.processPlateForEntry(imageFile, parkingId);

      if (result.success) {
        setSuccess(
          `License plate detected from image and entry recorded: ${result.vehicle.license_plate}`
        );
        setImageFile(null);
        setImagePreview(null);

        // Redirect to home page after 2 seconds
        setTimeout(() => {
          navigate("/");
        }, 2000);
      } else {
        setError(result.message || "Vehicle entry failed");
      }
    } catch (err) {
      setError("Image processing error: " + (err.message || "Unknown error"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container vehicle-entry">
      <h2>Vehicle Entry</h2>

      <div className="entry-mode-select">
        <button
          className={`mode-btn ${entryMode === "manual" ? "active" : ""}`}
          onClick={() => setEntryMode("manual")}
        >
          Manual Entry
        </button>
        <button
          className={`mode-btn ${entryMode === "image" ? "active" : ""}`}
          onClick={() => setEntryMode("image")}
        >
          Entry with Image
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

      {entryMode === "manual" ? (
        <form onSubmit={handleManualSubmit} className="entry-form">
          <div className="form-group">
            <label htmlFor="license-plate">License Plate</label>
            <input
              type="text"
              id="license-plate"
              className="form-control"
              value={licensePlate}
              onChange={(e) => setLicensePlate(e.target.value.toUpperCase())}
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
              {loading ? "Processing..." : "Record Vehicle Entry"}
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
        <form onSubmit={handleImageSubmit} className="entry-form">
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
              {loading ? "Processing Image..." : "Process Image & Record Entry"}
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
    </div>
  );
};

export default VehicleEntry;
