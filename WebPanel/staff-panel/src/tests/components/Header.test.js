import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Header from '../../components/Header';

// Mock localStorage
const mockLocalStorage = {
  getItem: jest.fn().mockReturnValue('123'),
  setItem: jest.fn(),
  removeItem: jest.fn(),
};
Object.defineProperty(window, 'localStorage', { value: mockLocalStorage });

// Mock the api service
jest.mock('../../services/api', () => ({
  api: {
    getUserInfo: jest.fn(),
    logout: jest.fn(),
  },
}));

describe('Header Component', () => {
  beforeEach(() => {
    // Clear all mocks before each test
    jest.clearAllMocks();
  });

  test('renders header with navigation links', () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    );

    // Check if main navigation links are present
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Araç Girişi')).toBeInTheDocument();
    expect(screen.getByText('Araç Çıkışı')).toBeInTheDocument();
  });

  test('handles logout correctly', () => {
    // Mock window.location
    delete window.location;
    window.location = { href: '' };

    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    );

    // Click logout button
    const logoutButton = screen.getByText('Çıkış Yap');
    fireEvent.click(logoutButton);

    // Check if api.logout was called
    const { api } = require('../../services/api');
    expect(api.logout).toHaveBeenCalled();
    
    // Check if redirected to login page
    expect(window.location.href).toBe('/login');
  });

  test('displays active navigation link based on current path', () => {
    render(
      <MemoryRouter initialEntries={['/vehicle-entry']}>
        <Header />
      </MemoryRouter>
    );

    // Check if the correct link has active class
    const activeLink = screen.getByText('Araç Girişi').closest('li');
    expect(activeLink).toHaveClass('active');
  });
});
