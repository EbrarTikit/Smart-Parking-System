import React from 'react';
import { Container } from '@mui/material';
import AppHeader from './AppHeader';

const PageLayout = ({ children, title, maxWidth = 'lg' }) => {
  return (
    <>
      <AppHeader title={title} />
      <Container maxWidth={maxWidth} sx={{ mt: 2, mb: 4 }}>
        {children}
      </Container>
    </>
  );
};

export default PageLayout; 