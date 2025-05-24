import React from 'react';
import { render, screen, within } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import PageHeader from '../../../components/common/PageHeader';

describe('PageHeader Component', () => {
  test('renders page header with title and breadcrumbs', () => {
    const title = 'Test Başlık';
    const breadcrumbs = [
      { label: 'Ana Sayfa', path: '/' },
      { label: 'Test Sayfa', path: '/test' }
    ];

    render(
      <BrowserRouter>
        <PageHeader title={title} breadcrumbs={breadcrumbs} />
      </BrowserRouter>
    );

    // Başlık ve breadcrumb'ların doğru render edildiğini kontrol et
    expect(screen.getByText(title)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /ana sayfa/i })).toBeInTheDocument();
    
    // Breadcrumb'ları kontrol et
    const breadcrumbNav = screen.getByRole('navigation', { name: /breadcrumb/i });
    expect(breadcrumbNav).toBeInTheDocument();
    
    // Breadcrumb içindeki metinleri kontrol et
    expect(within(breadcrumbNav).getByText('Ana Sayfa')).toBeInTheDocument();
    
    // Breadcrumb ayraçlarını kontrol et
    const separators = within(breadcrumbNav).getAllByText('/');
    expect(separators).toHaveLength(2);
  });
}); 