import React from 'react';
import { Button, Box, Tooltip } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import HomeIcon from '@mui/icons-material/Home';
import { useNavigate } from 'react-router-dom';

const BackButton = ({ 
  variant = 'outlined', 
  toDashboard = false, 
  dashboardPath = '/dashboard',
  position = 'start',
  sx = {}
}) => {
  const navigate = useNavigate();

  const handleBack = () => {
    if (toDashboard) {
      navigate(dashboardPath);
    } else {
      navigate(-1); // Go back to previous page
    }
  };

  return (
    <Box sx={{ 
      mb: 3, 
      display: 'flex', 
      justifyContent: position === 'start' ? 'flex-start' : 
                      position === 'end' ? 'flex-end' : 'center',
      ...sx
    }}>
      <Tooltip title={toDashboard ? "Return to Home Page" : "Go Back to Previous Page"}>
        <Button
          variant={variant}
          color="primary"
          startIcon={toDashboard ? <HomeIcon /> : <ArrowBackIcon />}
          onClick={handleBack}
        >
          {toDashboard ? "Home Page" : "Go Back"}
        </Button>
      </Tooltip>
    </Box>
  );
};

export default BackButton; 