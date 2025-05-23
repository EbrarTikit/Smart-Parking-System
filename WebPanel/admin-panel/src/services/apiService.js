// services/apiService.js
import axios from 'axios';

// Servis URL'leri - Environment değişkenlerinden al
const AUTH_SERVICE_URL = process.env.REACT_APP_AUTH_SERVICE_URL || 'http://localhost:8050/api';
const PARKING_SERVICE_URL = process.env.REACT_APP_PARKING_SERVICE_URL || 'http://localhost:8081/api';

// Axios instance oluştur
const api = axios.create({
    baseURL: AUTH_SERVICE_URL,
    headers: {
        'Content-Type': 'application/json'
    }
});

// API istekleri için interceptor
axios.interceptors.request.use(
  config => {
    console.log('API İsteği:', config.method.toUpperCase(), config.url, config.data);
    // Token yerine kullanıcı bilgilerini header'a ekle
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

axios.interceptors.response.use(
  response => {
    console.log('API Yanıtı:', response.status, response.data);
    return response;
  },
  error => {
    console.error('API Yanıt hatası:', 
      error.response ? 
      `${error.response.status} - ${JSON.stringify(error.response.data)}` : 
      error.message
    );
    return Promise.reject(error);
  }
);

// Auth API çağrıları
export const signIn = async (credentials) => {
    try {
        const response = await api.post('/auth/signin', credentials);
        const userData = {
            id: response.data.id || '1',
            username: response.data.username || credentials.username,
            email: response.data.email || 'admin@example.com',
            token: response.data.token
        };
        return { data: userData };
    } catch (error) {
        console.error('Giriş hatası:', error);
        throw error;
    }
};

export const signUp = async (userData) => {
    try {
        const response = await api.post('/auth/signup', userData);
        return response;
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