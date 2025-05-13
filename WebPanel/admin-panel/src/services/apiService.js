// services/apiService.js
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8081/api';

// Otopark API çağrıları
export const getParkings = () => {
  return axios.get(`${API_BASE_URL}/parkings`);
};

export const getParkingById = (id) => {
  return axios.get(`${API_BASE_URL}/parkings/${id}`);
};

export const createParking = (parkingData) => {
  return axios.post(`${API_BASE_URL}/parkings`, parkingData);
};

export const updateParking = (id, parkingData) => {
  return axios.put(`${API_BASE_URL}/parkings/${id}`, parkingData);
};

export const deleteParking = (id) => {
  return axios.delete(`${API_BASE_URL}/parkings/${id}`);
};

// Layout API çağrıları
export const getParkingLayout = (parkingId) => {
  return axios.get(`${API_BASE_URL}/${parkingId}/layout`);
};

export const updateParkingLayout = (parkingId, layoutData) => {
  return axios.post(`${API_BASE_URL}/${parkingId}/layout`, layoutData);
};

export const clearParkingLayout = (parkingId) => {
  return axios.put(`${API_BASE_URL}/${parkingId}/clear-layout`);
};