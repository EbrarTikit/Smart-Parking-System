import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import VehicleExit from '../../pages/VehicleExit';
import { api } from '../../services/api';

// API servisini mock'la
jest.mock('../../services/api', () => ({
  api: {
    vehicleExit: jest.fn(),
    processPlateForExit: jest.fn()
  }
}));

// React Router'ı mock'la
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => jest.fn(),
  useSearchParams: () => [new URLSearchParams()]
}));

describe('VehicleExit Component', () => {
  beforeEach(() => {
    // Her testten önce mock'ları temizle
    jest.clearAllMocks();
  });

  test('renders vehicle exit form with default mode (manual)', () => {
    render(
      <MemoryRouter>
        <VehicleExit />
      </MemoryRouter>
    );

    expect(screen.getByText('Araç Çıkışı')).toBeInTheDocument();
    expect(screen.getByLabelText('Plaka')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /araç çıkışı yap/i })).toBeInTheDocument();
  });

  test('switches between manual and image exit modes', async () => {
    render(
      <MemoryRouter>
        <VehicleExit />
      </MemoryRouter>
    );

    // Görüntü moduna geç
    fireEvent.click(screen.getByText('Görüntü ile Çıkış'));
    
    // Mod değişiminin tamamlanmasını bekle
    await waitFor(() => {
      expect(screen.getByLabelText('Plaka Görüntüsü')).toBeInTheDocument();
    });
  });

  test('shows error message when submitting empty manual form', async () => {
    render(
      <MemoryRouter>
        <VehicleExit />
      </MemoryRouter>
    );

    const submitButton = screen.getByRole('button', { name: /araç çıkışı yap/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Plaka bilgisi gereklidir')).toBeInTheDocument();
    });
  });

  test('successfully submits manual exit form', async () => {
    const mockResult = {
      success: true,
      entry_time: '2024-03-20T10:00:00',
      exit_time: '2024-03-20T12:00:00',
      duration_hours: 2,
      parking_fee: 20
    };

    api.vehicleExit.mockResolvedValueOnce(mockResult);

    render(
      <MemoryRouter>
        <VehicleExit />
      </MemoryRouter>
    );

    // Plaka girişi yap
    const plateInput = screen.getByLabelText('Plaka');
    fireEvent.change(plateInput, { target: { value: '34ABC123' } });

    // Formu gönder
    const submitButton = screen.getByRole('button', { name: /araç çıkışı yap/i });
    fireEvent.click(submitButton);

    // API çağrısını ve sonuç kartını kontrol et
    await waitFor(() => {
      expect(api.vehicleExit).toHaveBeenCalledWith('34ABC123', 23);
      expect(screen.getByText('Otopark Çıkış Bilgileri')).toBeInTheDocument();
    });
  });

  test('shows error message when manual exit fails', async () => {
    api.vehicleExit.mockResolvedValueOnce({
      success: false,
      message: 'Araç bulunamadı'
    });

    render(
      <MemoryRouter>
        <VehicleExit />
      </MemoryRouter>
    );

    // Plaka girişi yap
    const plateInput = screen.getByLabelText('Plaka');
    fireEvent.change(plateInput, { target: { value: '34ABC123' } });

    // Formu gönder
    const submitButton = screen.getByRole('button', { name: /araç çıkışı yap/i });
    fireEvent.click(submitButton);

    // Hata mesajını kontrol et
    await waitFor(() => {
      expect(screen.getByText('Araç bulunamadı')).toBeInTheDocument();
    });
  });

  test('successfully processes image exit', async () => {
    const mockResult = {
      success: true,
      license_plate: '34ABC123',
      entry_time: '2024-03-20T10:00:00',
      exit_time: '2024-03-20T12:00:00',
      duration_hours: 2,
      parking_fee: 20
    };

    api.processPlateForExit.mockResolvedValueOnce(mockResult);

    render(
      <MemoryRouter>
        <VehicleExit />
      </MemoryRouter>
    );

    // Görüntü moduna geç
    fireEvent.click(screen.getByText('Görüntü ile Çıkış'));

    // Dosya seç
    const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
    const fileInput = screen.getByLabelText('Plaka Görüntüsü');
    fireEvent.change(fileInput, { target: { files: [file] } });

    // Formu gönder
    const submitButton = screen.getByRole('button', { name: /görüntüden plaka tanı ve çıkış/i });
    fireEvent.click(submitButton);

    // API çağrısını ve sonuç kartını kontrol et
    await waitFor(() => {
      expect(api.processPlateForExit).toHaveBeenCalled();
      expect(screen.getByText('Otopark Çıkış Bilgileri')).toBeInTheDocument();
    });
  });

  test('shows error message when image processing fails', async () => {
    api.processPlateForExit.mockResolvedValueOnce({
      success: false,
      message: 'Plaka tespit edilemedi'
    });

    render(
      <MemoryRouter>
        <VehicleExit />
      </MemoryRouter>
    );

    // Görüntü moduna geç
    fireEvent.click(screen.getByText('Görüntü ile Çıkış'));

    // Dosya seç
    const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
    const fileInput = screen.getByLabelText('Plaka Görüntüsü');
    fireEvent.change(fileInput, { target: { files: [file] } });

    // Formu gönder
    const submitButton = screen.getByRole('button', { name: /görüntüden plaka tanı ve çıkış/i });
    fireEvent.click(submitButton);

    // Hata mesajını kontrol et
    await waitFor(() => {
      expect(screen.getByText('Plaka tespit edilemedi')).toBeInTheDocument();
    });
  });

  test('loads parking ID from URL parameters', () => {
    const searchParams = new URLSearchParams();
    searchParams.set('parking', '17');

    jest.spyOn(URLSearchParams.prototype, 'get').mockImplementation((param) => {
      if (param === 'parking') return '17';
      return null;
    });

    render(
      <MemoryRouter>
        <VehicleExit />
      </MemoryRouter>
    );

    const parkingSelect = screen.getByLabelText('Otopark:');
    expect(parkingSelect).toHaveValue('17');
  });
});
