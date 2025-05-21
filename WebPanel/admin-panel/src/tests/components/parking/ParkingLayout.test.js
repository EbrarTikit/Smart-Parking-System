import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import ParkingLayout from '../../../components/parking/ParkingLayout';
import * as apiService from '../../../services/apiService';

// API servislerini mock'la
jest.mock('../../../services/apiService', () => ({
  getParkingById: jest.fn(),
  updateParkingLayout: jest.fn(),
  clearParkingLayout: jest.fn(),
  getAllSensors: jest.fn(),
  addSensor: jest.fn(),
  updateSpotSensor: jest.fn()
}));

describe('ParkingLayout Component', () => {
  const mockParking = {
    id: 1,
    name: 'Test Otopark',
    rows: 5,
    columns: 10,
    parkingSpots: [
      { id: 1, row: 0, column: 0, spotIdentifier: 'A1', status: 'available' },
      { id: 2, row: 0, column: 1, spotIdentifier: 'A2', status: 'occupied' },
      { id: 3, row: 0, column: 2, spotIdentifier: 'A3', status: 'reserved' }
    ]
  };

  beforeEach(() => {
    jest.clearAllMocks();
    apiService.getParkingById.mockResolvedValue({ data: mockParking });
  });

  test('renders parking layout successfully', async () => {
    render(
      <BrowserRouter>
        <ParkingLayout />
      </BrowserRouter>
    );

    // Loading durumunu kontrol et
    expect(screen.getByRole('progressbar')).toBeInTheDocument();

    // Otopark düzeninin yüklendiğini kontrol et
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Test Otopark Otopark Düzeni' })).toBeInTheDocument();
    });
  });

  test('handles API error', async () => {
    apiService.getParkingById.mockRejectedValueOnce(new Error('API Error'));

    render(
      <BrowserRouter>
        <ParkingLayout />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Otopark bulunamadı')).toBeInTheDocument();
    });
  });
});
