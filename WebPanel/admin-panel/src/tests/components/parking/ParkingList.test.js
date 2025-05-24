import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import ParkingList from '../../../components/parking/ParkingList';
import { getParkings } from '../../../services/apiService';
import '@testing-library/jest-dom';

// Mock the apiService
jest.mock('../../../services/apiService');

describe('ParkingList Component', () => {
  const mockParkings = [
    { id: 1, name: 'Test Parking 1', totalSpots: 50, availableSpots: 30 },
    { id: 2, name: 'Test Parking 2', totalSpots: 100, availableSpots: 75 }
  ];

  beforeEach(() => {
    // Her test öncesi mock'ları temizle
    jest.clearAllMocks();
  });

  test('renders parking list successfully', async () => {
    getParkings.mockResolvedValueOnce({ data: mockParkings });

    render(
      <BrowserRouter>
        <ParkingList />
      </BrowserRouter>
    );

    // Loading durumunu kontrol et
    expect(screen.getByRole('progressbar')).toBeInTheDocument();

    // Verilerin yüklendiğini kontrol et
    await waitFor(() => {
      expect(screen.getByText('Test Parking 1')).toBeInTheDocument();
      expect(screen.getByText('Test Parking 2')).toBeInTheDocument();
    });
  });

  test('handles empty parking list', async () => {
    getParkings.mockResolvedValueOnce({ data: [] });

    render(
      <BrowserRouter>
        <ParkingList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Henüz hiç otopark bulunmuyor')).toBeInTheDocument();
    });
  });

  test('handles API error', async () => {
    getParkings.mockRejectedValueOnce(new Error('API Error'));

    render(
      <BrowserRouter>
        <ParkingList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Otoparklar yüklenirken bir hata oluştu')).toBeInTheDocument();
    });
  });
});
