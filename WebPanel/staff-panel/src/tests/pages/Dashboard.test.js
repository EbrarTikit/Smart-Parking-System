import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Dashboard from '../../pages/Dashboard';
import * as api from '../../services/api';

// API mock'ları
jest.mock('../../services/api', () => ({
  api: {
    getActiveVehicles: jest.fn(),
    getRecentActivities: jest.fn(),
    getParkingList: jest.fn().mockResolvedValue([
      { id: 23, name: 'Milas Otopark' },
      { id: 17, name: 'Merkez Otopark' }
    ])
  }
}));

// WebSocket hook mock'u
const mockWebSocket = {
  isConnected: true,
  messages: [],
  lastMessage: null,
  error: null,
  sendMessage: jest.fn()
};

jest.mock('../../hooks/useWebSocket', () => ({
  __esModule: true,
  default: () => mockWebSocket
}));

describe('Dashboard Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    api.api.getActiveVehicles.mockResolvedValue([]);
    api.api.getRecentActivities.mockResolvedValue([]);
  });

  test('renders dashboard with default state', async () => {
    await act(async () => {
      render(
        <MemoryRouter>
          <Dashboard />
        </MemoryRouter>
      );
    });

    expect(screen.getByText('Otopark Yönetim Paneli')).toBeInTheDocument();
    expect(screen.getByLabelText(/otopark/i)).toBeInTheDocument();
    expect(screen.getByText('Aktif Araçlar')).toBeInTheDocument();
    expect(screen.getByText('Son Aktiviteler')).toBeInTheDocument();
  });

  test('displays active vehicles when data is loaded', async () => {
    const mockVehicle = {
      id: 1,
      license_plate: '34ABC123',
      entry_time: '2024-03-20T10:00:00',
      parking_record_id: 101
    };

    api.api.getActiveVehicles.mockResolvedValueOnce([mockVehicle]);

    await act(async () => {
      render(
        <MemoryRouter>
          <Dashboard />
        </MemoryRouter>
      );
    });

    await waitFor(() => {
      expect(screen.getByText('34ABC123')).toBeInTheDocument();
    }, { timeout: 3000 });
  });

  test('displays recent activities when data is loaded', async () => {
    const mockActivity = {
      id: 1,
      license_plate: '34ABC123',
      action: 'entry',
      entry_time: '2024-03-20T10:00:00',
      message: '34ABC123 plakalı araç otoparka giriş yaptı'
    };

    api.api.getRecentActivities.mockResolvedValueOnce([mockActivity]);

    await act(async () => {
      render(
        <MemoryRouter>
          <Dashboard />
        </MemoryRouter>
      );
    });

    await waitFor(() => {
      expect(screen.getByText('34ABC123 plakalı araç otoparka giriş yaptı')).toBeInTheDocument();
    }, { timeout: 3000 });
  });

  test('displays error message when data loading fails', async () => {
    api.api.getActiveVehicles.mockRejectedValueOnce(new Error('API Error'));

    await act(async () => {
      render(
        <MemoryRouter>
          <Dashboard />
        </MemoryRouter>
      );
    });

    await waitFor(() => {
      expect(screen.getByText(/başlangıç verileri yüklenirken bir hata oluştu/i)).toBeInTheDocument();
    });
  });

  test('updates parking ID and sends WebSocket message when parking changes', async () => {
    await act(async () => {
      render(
        <MemoryRouter>
          <Dashboard />
        </MemoryRouter>
      );
    });

    const parkingSelect = screen.getByLabelText(/otopark/i);
    
    await act(async () => {
      fireEvent.change(parkingSelect, { target: { value: '17' } });
    });

    await waitFor(() => {
      expect(mockWebSocket.sendMessage).toHaveBeenCalledWith(
        JSON.stringify({
          type: 'parking_change',
          data: { parking_id: 17 }
        })
      );
    }, { timeout: 3000 });
  });
});
