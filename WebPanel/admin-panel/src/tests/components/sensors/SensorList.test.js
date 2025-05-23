import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import SensorList from '../../../components/sensors/SensorList';
import { getAllSensors, addSensor, deleteSensor } from '../../../services/apiService';

// API servislerini mock'la
jest.mock('../../../services/apiService');

// window.confirm'i mock'la
const mockConfirm = jest.fn();
window.confirm = mockConfirm;

describe('SensorList Component', () => {
  const mockSensors = [
    {
      id: 1,
      parkingId: '1',
      controllerId: 'C1',
      echoPin: '2',
      trigPin: '3'
    },
    {
      id: 2,
      parkingId: '1',
      controllerId: 'C2',
      echoPin: '4',
      trigPin: '5'
    }
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    getAllSensors.mockResolvedValue({ data: mockSensors });
  });

  test('renders sensor list successfully', async () => {
    render(
      <BrowserRouter>
        <SensorList />
      </BrowserRouter>
    );

    // Loading durumunu kontrol et
    expect(screen.getByRole('progressbar')).toBeInTheDocument();

    // Sensör listesinin yüklendiğini kontrol et
    await waitFor(() => {
      const table = screen.getByRole('table');
      expect(table).toBeInTheDocument();
      expect(screen.getByText('C1')).toBeInTheDocument(); // Controller ID
      expect(screen.getByText('C2')).toBeInTheDocument(); // Controller ID
    });
  });

  test('handles empty sensor list', async () => {
    getAllSensors.mockResolvedValueOnce({ data: [] });

    render(
      <BrowserRouter>
        <SensorList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });
  });

  test('handles API error', async () => {
    getAllSensors.mockRejectedValueOnce(new Error('API Error'));

    render(
      <BrowserRouter>
        <SensorList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/sensörler yüklenirken bir hata oluştu/i)).toBeInTheDocument();
    });
  });

  test('adds new sensor successfully', async () => {
    const newSensor = {
      parkingId: '1',
      controllerId: 'C3',
      echoPin: '6',
      trigPin: '7'
    };

    addSensor.mockResolvedValueOnce({ data: { id: 3, ...newSensor } });

    render(
      <BrowserRouter>
        <SensorList />
      </BrowserRouter>
    );

    // Yeni sensör ekle butonuna tıkla
    fireEvent.click(screen.getByText(/yeni sensör ekle/i));

    // Form alanlarını doldur
    fireEvent.change(screen.getByLabelText(/otopark id/i), {
      target: { value: newSensor.parkingId }
    });
    fireEvent.change(screen.getByLabelText(/kontrolcü id/i), {
      target: { value: newSensor.controllerId }
    });
    fireEvent.change(screen.getByLabelText(/echo pin/i), {
      target: { value: newSensor.echoPin }
    });
    fireEvent.change(screen.getByLabelText(/trig pin/i), {
      target: { value: newSensor.trigPin }
    });

    // Formu gönder
    const addButton = screen.getAllByText(/ekle/i).find(button => 
      button.closest('button')?.textContent === 'Ekle'
    );
    fireEvent.click(addButton);

    await waitFor(() => {
      expect(screen.getByText(/sensör başarıyla eklendi/i)).toBeInTheDocument();
    });
  });

  test('deletes sensor successfully', async () => {
    deleteSensor.mockResolvedValueOnce({});
    mockConfirm.mockReturnValueOnce(true);

    render(
      <BrowserRouter>
        <SensorList />
      </BrowserRouter>
    );

    // Sil butonuna tıkla
    const deleteButtons = await screen.findAllByText(/sil/i);
    fireEvent.click(deleteButtons[0]);

    // Onay dialogunu kontrol et
    expect(mockConfirm).toHaveBeenCalled();

    await waitFor(() => {
      expect(screen.getByText(/sensör başarıyla silindi/i)).toBeInTheDocument();
    });
  });
});
