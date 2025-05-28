// services/apiService.js
import axios from 'axios';

// Servis URL'leri
const AUTH_SERVICE_URL = 'http://localhost:8050/api';
const PARKING_SERVICE_URL = 'http://localhost:8081/api';

// Axios instance oluştur
const api = axios.create({
    baseURL: AUTH_SERVICE_URL,
    headers: {
        'Content-Type': 'application/json'
    },
    timeout: 10000 // 10 saniye timeout
});

// API istekleri için interceptor
api.interceptors.request.use(
  config => {
    console.log('API İsteği:', config.method.toUpperCase(), config.url, config.data);
    const userId = localStorage.getItem('userId');
    const username = localStorage.getItem('username');
    
    if (userId && username) {
      config.headers['X-User-Id'] = userId;
      config.headers['X-Username'] = username;
    }
    
    return config;
  },
  error => {
    console.error('API İstek hatası:', error);
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  response => {
    console.log('API Yanıtı:', response.status, response.data);
    return response;
  },
  error => {
    if (error.code === 'ERR_NETWORK') {
      console.error('Ağ hatası: Backend servisine ulaşılamıyor');
      return Promise.reject(new Error('Sunucuya ulaşılamıyor. Lütfen daha sonra tekrar deneyin.'));
    }
    
    if (error.response) {
      console.error('API Yanıt hatası:', error.response.status, error.response.data);
      return Promise.reject(error);
    }
    
    console.error('Beklenmeyen hata:', error);
    return Promise.reject(new Error('Beklenmeyen bir hata oluştu'));
  }
);

// Auth API çağrıları
export const signIn = async (credentials) => {
    try {
        const response = await api.post('/auth/signin', credentials);
        return response;
    } catch (error) {
        console.error('Giriş hatası:', error);
        throw error;
    }
};

export const signUp = async (userData) => {
    try {
        const response = await api.post('/auth/signup', userData);
        return response.data;
    } catch (error) {
        console.error('Kayıt hatası:', error);
        throw error;
    }
};

// Otopark API çağrıları
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
  console.log(`Otopark güncelleniyor, ID: ${id}`, parkingData);
  return axios.put(`${PARKING_SERVICE_URL}/admin/parkings/${id}`, parkingData, {
    headers: {
      'Content-Type': 'application/json'
    }
  });
};

export const deleteParking = (id) => {
  console.log(`Otopark siliniyor, ID: ${id}`);
  return axios.delete(`${PARKING_SERVICE_URL}/admin/parkings/${id}`);
};

// Layout API çağrıları
export const getParkingLayout = (parkingId) => {
  return axios.get(`${PARKING_SERVICE_URL}/${parkingId}/layout`);
};

export const updateParkingLayout = (parkingId, layoutData) => {
  return axios.post(`${PARKING_SERVICE_URL}/${parkingId}/layout`, layoutData);
};

export const clearParkingLayout = (parkingId) => {
  return axios.put(`${PARKING_SERVICE_URL}/${parkingId}/clear-layout`);
};

// Sensör API çağrıları
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

// Sensör durumu güncelleme
export const updateSensorStatus = (sensorData) => {
  return axios.post(`${PARKING_SERVICE_URL}/iot/update/spot`, sensorData);
};

// Yeni fonksiyon ekleyin
export const updateSpotSensor = (parkingId, row, column, sensorId) => {
  return axios.put(`${PARKING_SERVICE_URL}/parkings/${parkingId}/spots/sensor`, {
    row: row,
    column: column,
    sensorId: sensorId
  });
};