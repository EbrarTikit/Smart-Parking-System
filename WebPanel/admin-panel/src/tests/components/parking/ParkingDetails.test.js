import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import ParkingDetails from '../../../components/parking/ParkingDetails';
import * as apiService from '../../../services/apiService';

jest.mock('../../../services/apiService', () => ({
  getParkingById: jest.fn(),
  updateParkingStatus: jest.fn()
}));

describe('ParkingDetails Component', () => {
  const mockParking = {
    id: 1,
    name: 'Test Otopark',
    location: 'Test Konum',
    capacity: 100,
    availableSpots: 50,
    openingHours: '08:00',
    closingHours: '22:00',
    rate: 10,
    status: 'active',
    imageUrl: 'https://example.com/image.jpg',
    rows: 5,
    columns: 10,
    parkingSpots: [],
    roads: [],
    buildings: []
  };

  beforeEach(() => {
    jest.clearAllMocks();
    apiService.getParkingById.mockResolvedValue({ data: mockParking });
  });

  test('renders parking details successfully', async () => {
    render(
      <BrowserRouter>
        <ParkingDetails />
      </BrowserRouter>
    );

    // Loading durumunu kontrol et
    expect(screen.getByRole('progressbar')).toBeInTheDocument();

    // Otopark detaylarının yüklendiğini kontrol et
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Test Otopark Detayları' })).toBeInTheDocument();
      expect(screen.getByText('Test Konum')).toBeInTheDocument();
    });
  });

  test('handles API error', async () => {
    apiService.getParkingById.mockRejectedValueOnce(new Error('API Error'));

    render(
      <BrowserRouter>
        <ParkingDetails />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Otopark detayları yüklenirken bir hata oluştu')).toBeInTheDocument();
    });
  });
});