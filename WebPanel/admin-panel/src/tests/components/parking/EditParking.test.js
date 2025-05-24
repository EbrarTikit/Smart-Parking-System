import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import EditParking from '../../../components/parking/EditParking';
import { getParkingById, updateParking } from '../../../services/apiService';

// API servislerini mock'la
jest.mock('../../../services/apiService');

// useParams hook'unu mock'la
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useParams: () => ({ id: '1' }),
  useNavigate: () => jest.fn()
}));

// Test timeout süresini artır
jest.setTimeout(15000);

describe('EditParking Component', () => {
  const mockParkingData = {
    id: 1,
    name: 'Test Otopark',
    location: 'Test Konum',
    capacity: 100,
    openingHours: '08:00',
    closingHours: '22:00',
    rate: 10,
    latitude: 41.0082,
    longitude: 28.9784,
    rows: 5,
    columns: 10,
    imageUrl: 'https://example.com/image.jpg'
  };

  beforeEach(() => {
    // Her testten önce mock'ları temizle
    jest.clearAllMocks();
    // Varsayılan olarak başarılı yanıt döndür
    getParkingById.mockResolvedValue({ data: mockParkingData });
  });

  test('renders edit parking form with initial data', async () => {
    render(
      <BrowserRouter>
        <EditParking />
      </BrowserRouter>
    );

    // Loading durumunu kontrol et
    expect(screen.getByRole('progressbar')).toBeInTheDocument();

    // Form alanlarının yüklendiğini kontrol et
    await waitFor(() => {
      expect(screen.getByLabelText(/otopark adı/i)).toHaveValue(mockParkingData.name);
      expect(screen.getByLabelText(/konum/i)).toHaveValue(mockParkingData.location);
      expect(screen.getByLabelText(/kapasite/i)).toHaveValue(mockParkingData.capacity);
    });
  });

  test('handles successful parking update', async () => {
    const updatedData = {
      ...mockParkingData,
      name: 'Updated Otopark',
      capacity: 150
    };

    updateParking.mockResolvedValueOnce({ data: updatedData });

    render(
      <BrowserRouter>
        <EditParking />
      </BrowserRouter>
    );

    // Form alanlarını güncelle
    await waitFor(() => {
      const nameInput = screen.getByLabelText(/otopark adı/i);
      const capacityInput = screen.getByLabelText(/kapasite/i);
      
      fireEvent.change(nameInput, { target: { value: updatedData.name } });
      fireEvent.change(capacityInput, { target: { value: updatedData.capacity.toString() } });
    });

    // Formu gönder
    const submitButton = screen.getByRole('button', { name: /değişiklikleri kaydet/i });
    fireEvent.click(submitButton);

    // updateParking fonksiyonunun doğru parametrelerle çağrıldığını kontrol et
    await waitFor(() => {
      expect(updateParking).toHaveBeenCalledWith(
        '1', // useParams'dan gelen id değeri
        expect.objectContaining({
          name: updatedData.name,
          capacity: updatedData.capacity,
          location: mockParkingData.location,
          openingHours: mockParkingData.openingHours,
          closingHours: mockParkingData.closingHours,
          rate: mockParkingData.rate,
          latitude: mockParkingData.latitude,
          longitude: mockParkingData.longitude,
          rows: mockParkingData.rows,
          columns: mockParkingData.columns,
          imageUrl: mockParkingData.imageUrl
        })
      );
    });

    // Başarı mesajını kontrol et
    expect(await screen.findByText(/otopark başarıyla güncellendi/i)).toBeInTheDocument();
  });

  test('handles API error during update', async () => {
    updateParking.mockRejectedValueOnce(new Error('API Error'));

    render(
      <BrowserRouter>
        <EditParking />
      </BrowserRouter>
    );

    // Form alanlarını güncelle
    await waitFor(() => {
      const nameInput = screen.getByLabelText(/otopark adı/i);
      fireEvent.change(nameInput, { target: { value: 'Updated Name' } });
    });

    // Formu gönder
    const submitButton = screen.getByRole('button', { name: /değişiklikleri kaydet/i });
    fireEvent.click(submitButton);

    // Hata mesajını kontrol et
    expect(await screen.findByText(/otopark güncellenirken bir hata oluştu/i)).toBeInTheDocument();
  });

  test('handles API error during initial data fetch', async () => {
    // getParkingById'yi hata fırlatacak şekilde mock'la
    getParkingById.mockRejectedValueOnce(new Error('API Error'));

    render(
      <BrowserRouter>
        <EditParking />
      </BrowserRouter>
    );

    // Hata mesajını kontrol et
    await waitFor(() => {
      expect(screen.getByText(/otopark detayları yüklenirken bir hata oluştu/i)).toBeInTheDocument();
    });
  });
});