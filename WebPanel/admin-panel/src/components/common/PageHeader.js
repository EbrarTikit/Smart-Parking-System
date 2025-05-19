import React from 'react';
import { Box, Typography, Button, Breadcrumbs, Link } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import HomeIcon from '@mui/icons-material/Home';

const PageHeader = ({ title, breadcrumbs = [] }) => {
  const navigate = useNavigate();

  const handleGoBack = () => {
    navigate(-1); // Tarayıcı geçmişinde bir adım geri git
  };

  const handleGoHome = () => {
    navigate('/dashboard'); // Ana sayfaya git
  };

  return (
    <Box sx={{ mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4" component="h1">
          {title}
        </Typography>
        <Box>
          <Button 
            variant="outlined" 
            startIcon={<ArrowBackIcon />} 
            onClick={handleGoBack}
            sx={{ mr: 1 }}
          >
            Geri Dön
          </Button>
          <Button 
            variant="contained" 
            startIcon={<HomeIcon />} 
            onClick={handleGoHome}
          >
            Ana Sayfa
          </Button>
        </Box>
      </Box>
      
      {breadcrumbs.length > 0 && (
        <Breadcrumbs aria-label="breadcrumb" sx={{ mb: 2 }}>
          <Link
            underline="hover"
            color="inherit"
            onClick={handleGoHome}
            sx={{ cursor: 'pointer', display: 'flex', alignItems: 'center' }}
          >
            <HomeIcon sx={{ mr: 0.5 }} fontSize="inherit" />
            Ana Sayfa
          </Link>
          {breadcrumbs.map((crumb, index) => (
            <Link
              key={index}
              underline="hover"
              color={index === breadcrumbs.length - 1 ? 'text.primary' : 'inherit'}
              onClick={() => crumb.link && navigate(crumb.link)}
              sx={{ cursor: crumb.link ? 'pointer' : 'default' }}
            >
              {crumb.text}
            </Link>
          ))}
        </Breadcrumbs>
      )}
    </Box>
  );
};

export default PageHeader; 