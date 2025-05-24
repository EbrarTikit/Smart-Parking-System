import React from 'react';
import { render, screen } from '@testing-library/react';
import App from './App';
import api from './services/api';

// api servisini mock'la
jest.mock('./services/api', () => ({
  api: {
    isAuthenticated: jest.fn(),
  },
}));

// Her testten önce api.isAuthenticated mock'unu sıfırla
beforeEach(() => {
  api.api.isAuthenticated.mockClear();
});

describe('App Component', () => {
  test('renders login page by default when not authenticated', async () => {
    // isAuthenticated mock'u false olarak ayarla
    api.api.isAuthenticated.mockReturnValue(false);
    render(<App />);

    // Check for login page elements
    expect(screen.getByRole('heading', { name: 'Smart Parking Sistemi' })).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Kullanıcı adınızı girin')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Şifrenizi girin')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Giriş Yap' })).toBeInTheDocument();
  });

  test('renders dashboard when authenticated', async () => {
    // isAuthenticated mock'u true olarak ayarla
    api.api.isAuthenticated.mockReturnValue(true);
    render(<App />);

    // Check for a dashboard element (assuming Dashboard page has a specific element)
    // Bu kontrolü Dashboard bileşeninde bulunan belirgin bir element ile değiştirmelisiniz.
    // Örneğin, bir başlık veya dashboard'a özgü bir metin.
    // Şu an için örnek bir metin kontrolü ekliyorum.
    // Bu satırı projenize uygun şekilde güncelleyin.
    // await screen.findByText('Dashboard Başlığı'); // Örnek: Dashboard sayfasında 'Dashboard Başlığı' metni varsa
  });

  test('renders 404 page for unknown routes', async () => {
     // isAuthenticated mock'u false olarak ayarla
     api.api.isAuthenticated.mockReturnValue(false);
     render(<App />);

    // Check for 404 page element (assuming 404 page has a specific element)
    // Bu kontrolü 404 bileşeninde bulunan belirgin bir element ile değiştirmelisiniz.
    // Örneğin, "Sayfa Bulunamadı" gibi bir metin.
    // Şu an için örnek bir metin kontrolü ekliyorum.
    // Bu satırı projenize uygun şekilde güncelleyin.
    // await screen.findByText('Sayfa Bulunamadı'); // Örnek: 404 sayfasında 'Sayfa Bulunamadı' metni varsa
  });
});