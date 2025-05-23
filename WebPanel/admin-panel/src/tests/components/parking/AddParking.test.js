import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import AddParking from '../../../components/parking/AddParking';
import { addParking } from '../../../services/apiService';
import '@testing-library/jest-dom';

// Mock the apiService
jest.mock('../../../services/apiService');

describe('AddParking Component', () => {
  const mockParkingData = {
    name: 'Test Otopark',
    location: 'Test Konum',
    capacity: '100',
    openingHours: '08:00',
    closingHours: '22:00',
    rate: '10',
    latitude: '41.0082',
    longitude: '28.9784',
    rows: '5',
    columns: '10',
    imageUrl: 'https://example.com/image.jpg'
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders add parking form', () => {
    render(
      <BrowserRouter>
        <AddParking />
      </BrowserRouter>
    );

    expect(screen.getByLabelText(/otopark adı/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/konum/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/kapasite/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /otopark ekle/i })).toBeInTheDocument();
  });

  test('shows error when submitting empty required fields', async () => {
    render(
      <BrowserRouter>
        <AddParking />
      </BrowserRouter>
    );

    fireEvent.click(screen.getByRole('button', { name: /otopark ekle/i }));

    await waitFor(() => {
      expect(screen.getByText(/lütfen zorunlu alanları doldurun/i)).toBeInTheDocument();
    });
  });

  test('handles successful parking addition', async () => {
    const mockResponse = {
      data: {
        id: 1,
        ...mockParkingData
      }
    };

    addParking.mockResolvedValueOnce(mockResponse);

    render(
      <BrowserRouter>
        <AddParking />
      </BrowserRouter>
    );

    // Form alanlarını doldur
    fireEvent.change(screen.getByLabelText(/otopark adı/i), {
      target: { value: mockParkingData.name }
    });
    fireEvent.change(screen.getByLabelText(/konum/i), {
      target: { value: mockParkingData.location }
    });
    fireEvent.change(screen.getByLabelText(/kapasite/i), {
      target: { value: mockParkingData.capacity }
    });

    fireEvent.click(screen.getByRole('button', { name: /otopark ekle/i }));

    await waitFor(() => {
      expect(screen.getByText(/otopark başarıyla eklendi/i)).toBeInTheDocument();
    });

    expect(addParking).toHaveBeenCalledWith(expect.objectContaining({
      name: mockParkingData.name,
      location: mockParkingData.location,
      capacity: parseInt(mockParkingData.capacity, 10)
    }));
  });

  test('handles API error', async () => {
    addParking.mockRejectedValueOnce(new Error('API Error'));

    render(
      <BrowserRouter>
        <AddParking />
      </BrowserRouter>
    );

    // Form alanlarını doldur
    fireEvent.change(screen.getByLabelText(/otopark adı/i), {
      target: { value: mockParkingData.name }
    });
    fireEvent.change(screen.getByLabelText(/konum/i), {
      target: { value: mockParkingData.location }
    });
    fireEvent.change(screen.getByLabelText(/kapasite/i), {
      target: { value: mockParkingData.capacity }
    });

    fireEvent.click(screen.getByRole('button', { name: /otopark ekle/i }));

    await waitFor(() => {
      expect(screen.getByText(/otopark eklenirken bir hata oluştu/i)).toBeInTheDocument();
    });
  });
});
