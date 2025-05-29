// services/apiService.js
import axios from 'axios';

// Service URLs
const AUTH_SERVICE_URL = 'http://localhost:8050/api';
const PARKING_SERVICE_URL = 'http://localhost:8081/api';

// Create Axios instance
const api = axios.create({
    baseURL: AUTH_SERVICE_URL,
    headers: {
        'Content-Type': 'application/json'
    },
    timeout: 10000 // 10 seconds timeout
});

// API request interceptor
api.interceptors.request.use(
  config => {
    console.log('API Request:', config.method.toUpperCase(), config.url, config.data);
    const userId = localStorage.getItem('userId');
    const username = localStorage.getItem('username');
    
    if (userId && username) {
      config.headers['X-User-Id'] = userId;
      config.headers['X-Username'] = username;
    }
    
    return config;
  },
  error => {
    console.error('API Request error:', error);
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  response => {
    console.log('API Response:', response.status, response.data);
    return response;
  },
  error => {
    if (error.code === 'ERR_NETWORK') {
      console.error('Network error: Cannot reach backend service');
      return Promise.reject(new Error('Cannot reach server. Please try again later.'));
    }
    
    if (error.response) {
      console.error('API Response error:', error.response.status, error.response.data);
      return Promise.reject(error);
    }
    
    console.error('Unexpected error:', error);
    return Promise.reject(new Error('An unexpected error occurred'));
  }
);

// Auth API calls
export const signIn = async (credentials) => {
    try {
        const response = await api.post('/auth/signin', credentials);
        return response;
    } catch (error) {
        console.error('Login error:', error);
        throw error;
    }
};

export const signUp = async (userData) => {
    try {
        const response = await api.post('/auth/signup', userData);
        return response.data;
    } catch (error) {
        console.error('Registration error:', error);
        throw error;
    }
};

// Parking API calls
export const addParking = (parkingData) => {
    return axios.post(`${PARKING_SERVICE_URL}/admin/parkings`, parkingData);
};

export const getParkings = () => {
    return axios.get(`${PARKING_SERVICE_URL}/parkings`);
};

export const getParkingById = (id) => {
  return axios.get(`${PARKING_SERVICE_URL}/parkings/${id}`);
};

export const updateParking = (id, parkingData) => {
  console.log(`Updating parking, ID: ${id}`, parkingData);
  return axios.put(`${PARKING_SERVICE_URL}/admin/parkings/${id}`, parkingData, {
    headers: {
      'Content-Type': 'application/json'
    }
  });
};

export const deleteParking = (id) => {
  console.log(`Deleting parking, ID: ${id}`);
  return axios.delete(`${PARKING_SERVICE_URL}/admin/parkings/${id}`);
};

// Layout API calls
export const getParkingLayout = (parkingId) => {
  return axios.get(`${PARKING_SERVICE_URL}/${parkingId}/layout`);
};

export const updateParkingLayout = (parkingId, layoutData) => {
  return axios.post(`${PARKING_SERVICE_URL}/${parkingId}/layout`, layoutData);
};

export const clearParkingLayout = (parkingId) => {
  return axios.put(`${PARKING_SERVICE_URL}/${parkingId}/clear-layout`);
};

// Sensor API calls
export const addSensor = (sensorData) => {
  return axios.post(`${PARKING_SERVICE_URL}/iot/sensors/add`, sensorData);
};

export const deleteSensor = (id) => {
  return axios.delete(`${PARKING_SERVICE_URL}/iot/sensors/delete/${id}`);
};

export const getSensor = (id) => {
  return axios.get(`${PARKING_SERVICE_URL}/iot/sensors/get/${id}`);
};

export const getAllSensors = () => {
  return axios.get(`${PARKING_SERVICE_URL}/iot/sensors/get/all`);
};

// Update sensor status
export const updateSensorStatus = (sensorData) => {
  return axios.post(`${PARKING_SERVICE_URL}/iot/update/spot`, sensorData);
};

// Add new function
export const updateSpotSensor = (parkingId, row, column, sensorId) => {
  return axios.put(`${PARKING_SERVICE_URL}/parkings/${parkingId}/spots/sensor`, {
    row: row,
    column: column,
    sensorId: sensorId
  });
};