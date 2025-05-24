import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, useNavigate, useSearchParams } from 'react-router-dom';
import VehicleEntry from '../../pages/VehicleEntry';
import { api } from '../../services/api';

// Mock react-router-dom
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: jest.fn(),
  useSearchParams: jest.fn(),
}));

// Mock api service
jest.mock('../../services/api', () => ({
  api: {
    vehicleEntry: jest.fn(),
    processPlateForEntry: jest.fn(),
  },
}));

describe('VehicleEntry Component', () => {
  const mockNavigate = jest.fn();
  const mockSearchParams = new URLSearchParams();

  beforeEach(() => {
    jest.clearAllMocks();
    useNavigate.mockReturnValue(mockNavigate);
    useSearchParams.mockReturnValue([mockSearchParams]);
  });

  test('renders vehicle entry form with default mode (manual)', () => {
    render(
      <MemoryRouter>
        <VehicleEntry />
      </MemoryRouter>
    );

    expect(screen.getByText('Araç Girişi')).toBeInTheDocument();
    expect(screen.getByText('Manuel Giriş')).toBeInTheDocument();
    expect(screen.getByText('Görüntü ile Giriş')).toBeInTheDocument();
    expect(screen.getByLabelText('Plaka')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /araç girişi kaydet/i })).toBeInTheDocument();
  });

  test('switches between manual and image entry modes', async () => {
    render(
      <MemoryRouter>
        <VehicleEntry />
      </MemoryRouter>
    );

    // Initially in manual mode
    expect(screen.getByLabelText('Plaka')).toBeInTheDocument();
    expect(screen.queryByLabelText('Plaka Görüntüsü')).not.toBeInTheDocument();

    // Switch to image mode
    fireEvent.click(screen.getByText('Görüntü ile Giriş'));
    
    // Wait for the mode switch to complete
    await waitFor(() => {
      expect(screen.queryByLabelText('Plaka')).not.toBeInTheDocument();
      expect(screen.getByLabelText('Plaka Görüntüsü')).toBeInTheDocument();
    });

    // Switch back to manual mode
    fireEvent.click(screen.getByText('Manuel Giriş'));
    
    // Wait for the mode switch to complete
    await waitFor(() => {
      expect(screen.getByLabelText('Plaka')).toBeInTheDocument();
      expect(screen.queryByLabelText('Plaka Görüntüsü')).not.toBeInTheDocument();
    });
  });

  test('shows error message when submitting empty manual form', async () => {
    render(
      <MemoryRouter>
        <VehicleEntry />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByRole('button', { name: /araç girişi kaydet/i }));
    expect(await screen.findByText('Plaka bilgisi gereklidir')).toBeInTheDocument();
  });

  test('successfully submits manual entry form', async () => {
    api.vehicleEntry.mockResolvedValueOnce({ success: true });

    render(
      <MemoryRouter>
        <VehicleEntry />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText('Plaka'), { target: { value: '34ABC123' } });
    fireEvent.click(screen.getByRole('button', { name: /araç girişi kaydet/i }));

    await waitFor(() => {
      expect(api.vehicleEntry).toHaveBeenCalledWith('34ABC123', 17);
      expect(screen.getByText(/araç girişi başarılı/i)).toBeInTheDocument();
    });

    // Check navigation after success
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/');
    }, { timeout: 2500 });
  });

  test('shows error message when manual entry fails', async () => {
    api.vehicleEntry.mockRejectedValueOnce(new Error('API Error'));

    render(
      <MemoryRouter>
        <VehicleEntry />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText('Plaka'), { target: { value: '34ABC123' } });
    fireEvent.click(screen.getByRole('button', { name: /araç girişi kaydet/i }));

    expect(await screen.findByText(/sunucu hatası/i)).toBeInTheDocument();
  });

  test('successfully processes image entry', async () => {
    api.processPlateForEntry.mockResolvedValueOnce({
      success: true,
      vehicle: { license_plate: '34ABC123' },
    });

    render(
      <MemoryRouter>
        <VehicleEntry />
      </MemoryRouter>
    );

    // Switch to image mode
    fireEvent.click(screen.getByText('Görüntü ile Giriş'));
    
    // Wait for the mode switch to complete
    await waitFor(() => {
      expect(screen.getByLabelText('Plaka Görüntüsü')).toBeInTheDocument();
    });

    // Create a fake file
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    const input = screen.getByLabelText('Plaka Görüntüsü');

    // Simulate file upload
    Object.defineProperty(input, 'files', {
      value: [file],
    });
    fireEvent.change(input);

    // Submit the form
    fireEvent.click(screen.getByRole('button', { name: /görüntüden plaka tanı ve kaydet/i }));

    await waitFor(() => {
      expect(api.processPlateForEntry).toHaveBeenCalledWith(file, 17);
      expect(screen.getByText(/görüntüden plaka tespit edildi/i)).toBeInTheDocument();
    });

    // Check navigation after success
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/');
    }, { timeout: 2500 });
  });

  test('shows error message when image processing fails', async () => {
    api.processPlateForEntry.mockRejectedValueOnce(new Error('Processing Error'));

    render(
      <MemoryRouter>
        <VehicleEntry />
      </MemoryRouter>
    );

    // Switch to image mode
    fireEvent.click(screen.getByText('Görüntü ile Giriş'));
    
    // Wait for the mode switch to complete
    await waitFor(() => {
      expect(screen.getByLabelText('Plaka Görüntüsü')).toBeInTheDocument();
    });

    // Create a fake file
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    const input = screen.getByLabelText('Plaka Görüntüsü');

    // Simulate file upload
    Object.defineProperty(input, 'files', {
      value: [file],
    });
    fireEvent.change(input);

    // Submit the form
    fireEvent.click(screen.getByRole('button', { name: /görüntüden plaka tanı ve kaydet/i }));

    expect(await screen.findByText(/görüntü işleme hatası/i)).toBeInTheDocument();
  });

  test('loads parking ID from URL parameters', () => {
    mockSearchParams.set('parking', '23');
    render(
      <MemoryRouter>
        <VehicleEntry />
      </MemoryRouter>
    );

    expect(screen.getByLabelText('Otopark:')).toHaveValue('23');
  });
});
