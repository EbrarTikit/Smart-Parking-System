import React, { useEffect, useState } from 'react';
import { Container, Typography, Button, Box, Grid, Paper } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import PageHeader from './common/PageHeader';

const Dashboard = () => {
  const navigate = useNavigate();
  const [userData, setUserData] = useState({
    username: 'Kullanıcı',
    userId: '1',
    userEmail: 'admin@example.com'
  });

  useEffect(() => {
    // Sayfa yüklendiğinde localStorage'dan kullanıcı bilgilerini al
    const username = localStorage.getItem('username');
    const userId = localStorage.getItem('userId');
    const userEmail = localStorage.getItem('userEmail');
    
    // Eğer kullanıcı bilgileri varsa state'i güncelle
    if (username || userId || userEmail) {
      setUserData({
        username: username || 'Kullanıcı',
        userId: userId || '1',
        userEmail: userEmail || 'admin@example.com'
      });
    }
  }, []);
  
  const handleLogout = () => {
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('userEmail');
    navigate('/signin');
  };
  
  return (
    <Container maxWidth="md">
      <Box sx={{ my: 4 }}>
        <PageHeader title={`Hoş Geldiniz, ${userData.username}`} />
        
        <Box sx={{ mb: 3, p: 2, bgcolor: 'background.paper', borderRadius: 1 }}>
          <Typography variant="subtitle1" gutterBottom>
            Kullanıcı Bilgileri:
          </Typography>
          <Typography variant="body2">
            ID: {userData.userId}
          </Typography>
          <Typography variant="body2">
            Kullanıcı Adı: {userData.username}
          </Typography>
          <Typography variant="body2">
            E-posta: {userData.userEmail}
          </Typography>
        </Box>
        
        <Typography variant="h6" gutterBottom sx={{ mb: 3 }}>
          Otopark Yönetim Paneli
        </Typography>
        
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Paper
              elevation={3}
              sx={{
                p: 3,
                textAlign: 'center',
                cursor: 'pointer',
                height: '100%',
                '&:hover': { backgroundColor: '#f5f5f5' }
              }}
              onClick={() => navigate('/add-parking')}
            >
              <Typography variant="h6" sx={{ mt: 1 }}>
                Yeni Otopark Ekle
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Sisteme yeni bir otopark eklemek için tıklayın
              </Typography>
            </Paper>
          </Grid>
          
          <Grid item xs={12} md={6}>
            <Paper
              elevation={3}
              sx={{
                p: 3,
                textAlign: 'center',
                cursor: 'pointer',
                height: '100%',
                '&:hover': { backgroundColor: '#f5f5f5' }
              }}
              onClick={() => navigate('/parkings')}
            >
              <Typography variant="h6" sx={{ mt: 1 }}>
                Otoparkları Listele
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Mevcut otoparkları görüntülemek ve yönetmek için tıklayın
              </Typography>
            </Paper>
          </Grid>
        </Grid>
        
        <Box sx={{ mt: 4, textAlign: 'right' }}>
          <Button 
            variant="outlined" 
            color="error" 
            onClick={handleLogout}
          >
            Çıkış Yap
          </Button>
        </Box>
      </Box>
    </Container>
  );
};

export default Dashboard;