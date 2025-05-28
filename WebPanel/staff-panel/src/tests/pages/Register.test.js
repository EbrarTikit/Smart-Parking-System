import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, useNavigate } from 'react-router-dom';
import Register from '../../pages/Register';
import { api } from '../../services/api';

// Mock react-router-dom
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: jest.fn(),
}));

// Mock api service
jest.mock('../../services/api', () => ({
  api: {
    register: jest.fn(),
  },
}));

describe('Register Component', () => {
  const mockNavigate = jest.fn();

  beforeEach(() => {
    // Reset all mocks before each test
    jest.clearAllMocks();
    useNavigate.mockReturnValue(mockNavigate);
  });

  test('renders register form with all fields', () => {
    render(
      <MemoryRouter>
        <Register isAuthenticated={false} />
      </MemoryRouter>
    );

    // Check if all form elements are present
    expect(screen.getByLabelText(/kullanıcı adı/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^🔒\s*şifre$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^🔐\s*şifre tekrar$/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /kayıt ol/i })).toBeInTheDocument();
  });

  test('shows error message when form is submitted empty', async () => {
    render(
      <MemoryRouter>
        <Register isAuthenticated={false} />
      </MemoryRouter>
    );

    // Submit empty form
    fireEvent.click(screen.getByRole('button', { name: /kayıt ol/i }));

    // Check for error message
    expect(await screen.findByText('Tüm alanları doldurunuz')).toBeInTheDocument();
  });

  test('shows error message when passwords do not match', async () => {
    render(
      <MemoryRouter>
        <Register isAuthenticated={false} />
      </MemoryRouter>
    );

    // Fill form with mismatched passwords
    fireEvent.change(screen.getByLabelText(/kullanıcı adı/i), { target: { value: 'testuser' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByLabelText(/^🔒\s*şifre$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/^🔐\s*şifre tekrar$/i), { target: { value: 'password456' } });

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: /kayıt ol/i }));

    // Check for error message
    expect(await screen.findByText('Şifreler eşleşmiyor')).toBeInTheDocument();
  });

  test('shows error message for invalid email format', async () => {
    render(
      <MemoryRouter>
        <Register isAuthenticated={false} />
      </MemoryRouter>
    );

    // Fill form with invalid email
    fireEvent.change(screen.getByLabelText(/kullanıcı adı/i), { target: { value: 'testuser' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'invalid-email' } });
    fireEvent.change(screen.getByLabelText(/^🔒\s*şifre$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/^🔐\s*şifre tekrar$/i), { target: { value: 'password123' } });

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: /kayıt ol/i }));

    // Check for error message
    expect(await screen.findByText('Geçerli bir email adresi giriniz')).toBeInTheDocument();
  });

  test('shows error message for short password', async () => {
    render(
      <MemoryRouter>
        <Register isAuthenticated={false} />
      </MemoryRouter>
    );

    // Fill form with short password
    fireEvent.change(screen.getByLabelText(/kullanıcı adı/i), { target: { value: 'testuser' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByLabelText(/^🔒\s*şifre$/i), { target: { value: '12345' } });
    fireEvent.change(screen.getByLabelText(/^🔐\s*şifre tekrar$/i), { target: { value: '12345' } });

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: /kayıt ol/i }));

    // Check for error message
    expect(await screen.findByText('Şifre en az 6 karakter olmalıdır')).toBeInTheDocument();
  });

  test('successfully registers and redirects to login page', async () => {
    // Mock successful API call
    api.register.mockResolvedValueOnce();

    render(
      <MemoryRouter>
        <Register isAuthenticated={false} />
      </MemoryRouter>
    );

    // Fill form with valid data
    fireEvent.change(screen.getByLabelText(/kullanıcı adı/i), { target: { value: 'testuser' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByLabelText(/^🔒\s*şifre$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/^🔐\s*şifre tekrar$/i), { target: { value: 'password123' } });

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: /kayıt ol/i }));

    // Wait for API call and navigation
    await waitFor(() => {
      expect(api.register).toHaveBeenCalledWith('testuser', 'test@example.com', 'password123');
      expect(mockNavigate).toHaveBeenCalledWith('/login', {
        state: { message: 'Kayıt başarılı! Şimdi giriş yapabilirsiniz.' },
      });
    });
  });

  test('shows error message when registration fails', async () => {
    // Mock failed API call
    api.register.mockRejectedValueOnce(new Error('Registration failed'));

    render(
      <MemoryRouter>
        <Register isAuthenticated={false} />
      </MemoryRouter>
    );

    // Fill form with valid data
    fireEvent.change(screen.getByLabelText(/kullanıcı adı/i), { target: { value: 'testuser' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByLabelText(/^🔒\s*şifre$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/^🔐\s*şifre tekrar$/i), { target: { value: 'password123' } });

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: /kayıt ol/i }));

    // Check for error message
    expect(await screen.findByText('Registration failed')).toBeInTheDocument();
  });

  test('redirects to dashboard if already authenticated', () => {
    render(
      <MemoryRouter>
        <Register isAuthenticated={true} />
      </MemoryRouter>
    );

    expect(mockNavigate).toHaveBeenCalledWith('/');
  });
});
