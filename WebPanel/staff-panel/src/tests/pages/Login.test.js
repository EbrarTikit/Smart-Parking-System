import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Login from '../../../src/pages/Login';
import * as api from '../../../src/services/api';

// useApi hook'unu mocklayalım ki gerçek API çağrısı yapılmasın
jest.mock('../../../src/services/api', () => ({
  api: {
    login: jest.fn(),
  },
}));

// useNavigate ve useLocation hook'larını mock'layalım
const mockNavigate = jest.fn();
// useLocation mock'unu describe bloğunun dışında tanımlayalım
const mockUseLocation = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useLocation: () => mockUseLocation(), // Çağrılarak güncel mock değeri alınacak
}));

describe('Login Component', () => {
  const setIsAuthenticatedMock = jest.fn();

  beforeEach(() => {
    // Clear all mocks before each test
    jest.clearAllMocks();
    // Mock useLocation'ı her testten önce resetle ve varsayılan boş state döndür
    mockUseLocation.mockReturnValue({ state: null });
    // Default olarak login fonksiyonu başarılı dönsün
    require('../../../src/services/api').api.login.mockResolvedValue({}); // Adjust path
  });

  test('renders login form with required elements', () => {
    render(
      <MemoryRouter>
        <Login setIsAuthenticated={() => {}} />
      </MemoryRouter>
    );

    // Başlıkları ve metinleri kontrol et
    expect(screen.getByRole('heading', { name: 'Smart Parking Sistemi' })).toBeInTheDocument();
    expect(screen.getByText('Yönetim Paneline Giriş Yapın')).toBeInTheDocument();

    // Input alanlarını etiketleriyle kontrol et
    expect(screen.getByLabelText(/Kullanıcı Adı/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Şifre/i)).toBeInTheDocument();

    // Giriş butonunu kontrol et
    expect(screen.getByRole('button', { name: 'Giriş Yap' })).toBeInTheDocument();
  });

  // --- Eklenecek Testler ---

  test('displays error message when username and password are empty', async () => {
    render(
      <MemoryRouter>
        <Login setIsAuthenticated={setIsAuthenticatedMock} />
      </MemoryRouter>
    );

    // Giriş yap butonuna tıkla
    fireEvent.click(screen.getByRole('button', { name: /giriş yap/i }));

    // Hata mesajının görüntülenmesini bekle ve kontrol et
    await waitFor(() => {
      expect(screen.getByText('Kullanıcı adı ve şifre gereklidir')).toBeInTheDocument();
    });

    // login fonksiyonunun çağrılmadığını kontrol et
    const { api } = require('../../../src/services/api'); // Adjust path
    expect(api.login).not.toHaveBeenCalled();
  });


  test('calls api.login and navigates to dashboard on successful login', async () => {
    render(
      <MemoryRouter>
        <Login setIsAuthenticated={setIsAuthenticatedMock} />
      </MemoryRouter>
    );

    // Kullanıcı adı ve şifre gir
    fireEvent.change(screen.getByLabelText(/kullanıcı adı/i), { target: { value: 'testuser' } });
    fireEvent.change(screen.getByLabelText(/şifre/i), { target: { value: 'password123' } });

    // Giriş yap butonuna tıkla
    fireEvent.click(screen.getByRole('button', { name: /giriş yap/i }));

    // login fonksiyonunun doğru parametrelerle çağrıldığını bekle ve kontrol et
    const { api } = require('../../../src/services/api'); // Adjust path
    await waitFor(() => {
      expect(api.login).toHaveBeenCalledWith('testuser', 'password123');
    });

    // setIsAuthenticated(true) çağrıldığını kontrol et
    expect(setIsAuthenticatedMock).toHaveBeenCalledWith(true);

    // Anasayfaya yönlendirildiğini kontrol et
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  test('displays error message on failed login', async () => {
    // login fonksiyonunun hata fırlatmasını sağla
    const { api } = require('../../../src/services/api'); // Adjust path
    api.login.mockRejectedValue(new Error('Invalid credentials'));

    render(
      <MemoryRouter>
        <Login setIsAuthenticated={setIsAuthenticatedMock} />
      </MemoryRouter>
    );

    // Kullanıcı adı ve şifre gir
    fireEvent.change(screen.getByLabelText(/kullanıcı adı/i), { target: { value: 'testuser' } });
    fireEvent.change(screen.getByLabelText(/şifre/i), { target: { value: 'password123' } });

    // Giriş yap butonuna tıkla
    fireEvent.click(screen.getByRole('button', { name: /giriş yap/i }));

    // Hata mesajının görüntülenmesini bekle ve kontrol et
    await waitFor(() => {
      expect(screen.getByText('Invalid credentials')).toBeInTheDocument();
    });

    // setIsAuthenticated(true) çağrılmadığını kontrol et
    expect(setIsAuthenticatedMock).not.toHaveBeenCalled();
    // Yönlendirme yapılmadığını kontrol et
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  test('disables inputs and button while loading', async () => {
     // login fonksiyonunun gecikmeli çözülmesini sağla (loading durumunu test etmek için)
    const { api } = require('../../../src/services/api'); // Adjust path
    api.login.mockImplementation(() => {
      return new Promise(resolve => setTimeout(() => resolve({}), 100));
    });

    render(
      <MemoryRouter>
        <Login setIsAuthenticated={setIsAuthenticatedMock} />
      </MemoryRouter>
    );

    // Kullanıcı adı ve şifre gir
    fireEvent.change(screen.getByLabelText(/kullanıcı adı/i), { target: { value: 'testuser' } });
    fireEvent.change(screen.getByLabelText(/şifre/i), { target: { value: 'password123' } });

    // Giriş yap butonuna tıkla
    fireEvent.click(screen.getByRole('button', { name: /giriş yap/i }));

    // Yükleme durumunda inputların ve butonun disabled olduğunu kontrol et
    await waitFor(() => {
      expect(screen.getByLabelText(/kullanıcı adı/i)).toBeDisabled();
      expect(screen.getByLabelText(/şifre/i)).toBeDisabled();
      // Buton metni yükleme durumunda değişiyor, bu metni kontrol et
      expect(screen.getByRole('button', { name: /giriş yapılıyor.../i })).toBeDisabled();
    });

     // Yükleme tamamlandıktan sonra inputların ve butonun enabled olduğunu kontrol et
    await waitFor(() => {
       expect(screen.getByLabelText(/kullanıcı adı/i)).not.toBeDisabled();
       expect(screen.getByLabelText(/şifre/i)).not.toBeDisabled();
       // Buton metni tekrar değiştiği için doğru metni kontrol et
       expect(screen.getByRole('button', { name: /giriş yap/i })).not.toBeDisabled();
     });
  });

  test('displays error message from location state', async () => {
    // Bu test özelinde useLocation mock'unun state ile hata mesajı döndürmesini sağla
    mockUseLocation.mockReturnValueOnce({ state: { message: 'Yetkilendirme gerekli.' } });

     await act(async () => { // render işlemini act içine al
        render(
            <MemoryRouter>
              <Login setIsAuthenticated={setIsAuthenticatedMock} />
            </MemoryRouter>
          );
     });

    // Hata mesajının görüntülenmesini bekle ve kontrol et
    await waitFor(() => {
      expect(screen.getByText('Yetkilendirme gerekli.')).toBeInTheDocument();
    });
  });
});
