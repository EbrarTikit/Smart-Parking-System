import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import SignIn from '../../../components/auth/SignIn';

describe('SignIn Component', () => {
  test('renders login form', () => {
    render(
      <BrowserRouter>
        <SignIn />
      </BrowserRouter>
    );

    // Form elemanlarının varlığını kontrol et
    expect(screen.getByLabelText(/kullanıcı adı/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/şifre/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /giriş yap/i })).toBeInTheDocument();
  });

  test('form validation works', async () => {
    render(
      <BrowserRouter>
        <SignIn />
      </BrowserRouter>
    );

    // Submit butonuna tıkla
    const submitButton = screen.getByRole('button', { name: /giriş yap/i });
    fireEvent.click(submitButton);

    // Hata mesajlarının görüntülendiğini kontrol et
    expect(await screen.findByText(/kullanıcı adı ve şifre gereklidir/i)).toBeInTheDocument();
  });
}); 