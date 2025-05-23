import React from 'react';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Dashboard from '../../components/Dashboard';

describe('Dashboard Component', () => {
  test('renders dashboard with basic elements', () => {
    render(
      <BrowserRouter>
        <Dashboard />
      </BrowserRouter>
    );

    // Temel dashboard elemanlarının varlığını kontrol et
    expect(screen.getByText(/hoş geldiniz/i)).toBeInTheDocument();
    expect(screen.getByText(/otopark yönetim paneli/i)).toBeInTheDocument();
    expect(screen.getByText(/yeni otopark ekle/i)).toBeInTheDocument();
    expect(screen.getByText(/otoparkları listele/i)).toBeInTheDocument();
  });
}); 