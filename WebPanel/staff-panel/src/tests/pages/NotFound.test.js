import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import NotFound from '../../pages/NotFound';

describe('NotFound Component', () => {
  test('renders 404 page with correct content', () => {
    render(
      <MemoryRouter>
        <NotFound />
      </MemoryRouter>
    );

    // Check if main elements are present
    expect(screen.getByText('404')).toBeInTheDocument();
    expect(screen.getByText('Sayfa Bulunamadı')).toBeInTheDocument();
    expect(screen.getByText('Aradığınız sayfa bulunmamaktadır.')).toBeInTheDocument();
  });

  test('renders home link with correct text and href', () => {
    render(
      <MemoryRouter>
        <NotFound />
      </MemoryRouter>
    );

    // Check if home link is present with correct text
    const homeLink = screen.getByText('Ana Sayfaya Dön');
    expect(homeLink).toBeInTheDocument();
    expect(homeLink).toHaveAttribute('href', '/');
  });
});
