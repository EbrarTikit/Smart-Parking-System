import axios from 'axios';
import { signIn, signUp, getParkings, getParkingDetails } from '../../services/apiService';

// Mock axios
jest.mock('axios');

describe('API Service', () => {
  beforeEach(() => {
    // Her test öncesi mock'ları temizle
    jest.clearAllMocks();
  });

  describe('signIn', () => {
    const mockCredentials = {
      username: 'testuser',
      password: 'password123'
    };

    test('should successfully sign in user', async () => {
      const mockResponse = {
        data: {
          id: 1,
          username: 'testuser',
          email: 'test@example.com'
        }
      };

      axios.post.mockResolvedValueOnce(mockResponse);

      const result = await signIn(mockCredentials);
      expect(result.data).toEqual(mockResponse.data);
      expect(axios.post).toHaveBeenCalledWith('http://localhost:8050/api/auth/signin', mockCredentials);
    });

    test('should handle sign in error', async () => {
      const errorMessage = 'Invalid credentials';
      axios.post.mockRejectedValueOnce(new Error(errorMessage));

      await expect(signIn(mockCredentials)).rejects.toThrow(errorMessage);
    });
  });

  describe('getParkings', () => {
    test('should fetch parkings successfully', async () => {
      const mockParkings = [
        { id: 1, name: 'Parking 1' },
        { id: 2, name: 'Parking 2' }
      ];

      axios.get.mockResolvedValueOnce({ data: mockParkings });

      const result = await getParkings();
      expect(result.data).toEqual(mockParkings);
      expect(axios.get).toHaveBeenCalledWith('http://localhost:8081/api/parkings');
    });
  });
});
