import React, { useState } from 'react';
import { 
  AppBar, Toolbar, Typography, Button, IconButton, 
  Box, Menu, MenuItem, Avatar, Tooltip, Divider 
} from '@mui/material';
import { useNavigate, useLocation } from 'react-router-dom';
import HomeIcon from '@mui/icons-material/Home';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import ExitToAppIcon from '@mui/icons-material/ExitToApp';

const AppHeader = ({ title }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);

  const handleProfileMenuOpen = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    handleClose();
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('authToken');
    navigate('/signin');
  };

  const navigateToDashboard = () => {
    navigate('/dashboard');
  };

  const navigateBack = () => {
    navigate(-1);
  };

  const isAuthenticated = localStorage.getItem('isLoggedIn') === 'true';
  const isDashboard = location.pathname === '/dashboard';

  return (
    <AppBar position="static" color="primary" sx={{ mb: 3 }}>
      <Toolbar>
        {isAuthenticated && !isDashboard && (
          <Tooltip title="Geri">
            <IconButton
              size="large"
              edge="start"
              color="inherit"
              aria-label="back"
              sx={{ mr: 2 }}
              onClick={navigateBack}
            >
              <ArrowBackIcon />
            </IconButton>
          </Tooltip>
        )}

        {isAuthenticated && (
          <Tooltip title="Ana Sayfa">
            <IconButton
              size="large"
              edge="start"
              color="inherit"
              aria-label="home"
              sx={{ mr: isDashboard ? 2 : 0 }}
              onClick={navigateToDashboard}
            >
              <HomeIcon />
            </IconButton>
          </Tooltip>
        )}

        <Typography variant="h6" component="div" sx={{ flexGrow: 1, ml: 2 }}>
          {title || 'Otopark Yönetim Sistemi'}
        </Typography>

        {isAuthenticated ? (
          <>
            <Tooltip title="Kullanıcı Menüsü">
              <IconButton
                onClick={handleProfileMenuOpen}
                size="large"
                sx={{ ml: 2 }}
                aria-controls={open ? 'account-menu' : undefined}
                aria-haspopup="true"
                aria-expanded={open ? 'true' : undefined}
              >
                <AccountCircleIcon sx={{ width: 32, height: 32, color: 'white' }} />
              </IconButton>
            </Tooltip>
            <Menu
              anchorEl={anchorEl}
              id="account-menu"
              open={open}
              onClose={handleClose}
              onClick={handleClose}
              PaperProps={{
                elevation: 0,
                sx: {
                  overflow: 'visible',
                  filter: 'drop-shadow(0px 2px 8px rgba(0,0,0,0.32))',
                  mt: 1.5,
                  '& .MuiAvatar-root': {
                    width: 32,
                    height: 32,
                    ml: -0.5,
                    mr: 1,
                  },
                },
              }}
              transformOrigin={{ horizontal: 'right', vertical: 'top' }}
              anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
            >
              <MenuItem onClick={() => { handleClose(); navigate('/profile'); }}>
                <Avatar /> Profil
              </MenuItem>
              <Divider />
              <MenuItem onClick={handleLogout}>
                <ExitToAppIcon fontSize="small" sx={{ mr: 1 }} /> 
                Çıkış Yap
              </MenuItem>
            </Menu>
          </>
        ) : (
          <Box>
            <Button color="inherit" onClick={() => navigate('/signin')}>Giriş Yap</Button>
            <Button color="inherit" onClick={() => navigate('/signup')}>Kayıt Ol</Button>
          </Box>
        )}
      </Toolbar>
    </AppBar>
  );
};

export default AppHeader; 