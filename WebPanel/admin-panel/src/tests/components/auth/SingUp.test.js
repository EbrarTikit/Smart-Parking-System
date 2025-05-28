import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import SignUp from '../../../components/auth/SignUp';
import axios from 'axios';

// axios'u mock'la
jest.mock('axios');

describe('SignUp Component', () => {
  const mockNavigate = jest.fn();

  beforeEach(() => {
    // Her test öncesi mock'ları temizle
    jest.clearAllMocks();
    // useNavigate hook'unu mock'la
    jest.mock('react-router-dom', () => ({
      ...jest.requireActual('react-router-dom'),
      useNavigate: () => mockNavigate
    }));
  });

  test('renders signup form with all fields', () => {
    render(
      <BrowserRouter>
        <SignUp />
      </BrowserRouter>
    );

    // Form elemanlarının varlığını kontrol et
    expect(screen.getByLabelText(/kullanıcı adı/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email adresi/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/şifre/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /kayıt ol/i })).toBeInTheDocument();
  });

  test('shows error when submitting empty form', async () => {
    render(
      <BrowserRouter>
        <SignUp />
      </BrowserRouter>
    );

    // Submit butonuna tıkla
    fireEvent.click(screen.getByRole('button', { name: /kayıt ol/i }));

    // Hata mesajının görüntülendiğini kontrol et
    expect(await screen.findByText(/lütfen tüm alanları doldurun/i)).toBeInTheDocument();
  });

  test('handles successful signup', async () => {
    const mockResponse = {
      data: {
        id: 1,
        username: 'testuser',
        email: 'test@example.com'
      }
    };

    axios.post.mockResolvedValueOnce(mockResponse);

    render(
      <BrowserRouter>
        <SignUp />
      </BrowserRouter>
    );

    // Form alanlarını doldur
    fireEvent.change(screen.getByLabelText(/kullanıcı adı/i), {
      target: { value: 'testuser' }
    });
    fireEvent.change(screen.getByLabelText(/email adresi/i), {
      target: { value: 'test@example.com' }
    });
    fireEvent.change(screen.getByLabelText(/şifre/i), {
      target: { value: 'password123' }
    });

    // Formu gönder
    fireEvent.click(screen.getByRole('button', { name: /kayıt ol/i }));

    // Başarı mesajını kontrol et
    await waitFor(() => {
      expect(screen.getByText(/kayıt başarılı/i)).toBeInTheDocument();
    });

    // API çağrısının doğru parametrelerle yapıldığını kontrol et
    expect(axios.post).toHaveBeenCalledWith(
      'http://localhost:8050/api/auth/signup',
      {
        username: 'testuser',
        email: 'test@example.com',
        password: 'password123'
      }
    );
  });

  test('handles API error', async () => {
    const errorMessage = 'Bu kullanıcı adı zaten kullanılıyor';
    axios.post.mockRejectedValueOnce({
      response: {
        data: {
          message: errorMessage
        }
      }
    });

    render(
      <BrowserRouter>
        <SignUp />
      </BrowserRouter>
    );

    // Form alanlarını doldur
    fireEvent.change(screen.getByLabelText(/kullanıcı adı/i), {
      target: { value: 'testuser' }
    });
    fireEvent.change(screen.getByLabelText(/email adresi/i), {
      target: { value: 'test@example.com' }
    });
    fireEvent.change(screen.getByLabelText(/şifre/i), {
      target: { value: 'password123' }
    });

    // Formu gönder
    fireEvent.click(screen.getByRole('button', { name: /kayıt ol/i }));

    // Hata mesajını kontrol et
    await waitFor(() => {
      expect(screen.getByText(errorMessage)).toBeInTheDocument();
    });
  });
});