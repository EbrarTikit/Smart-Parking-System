import React, { useState } from 'react';
import { 
  Container, Box, Typography, TextField, Button, 
  Paper, Grid, Link, CircularProgress, Alert 
} from '@mui/material';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import axios from 'axios';

const SignIn = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.username || !formData.password) {
      setError('Kullanıcı adı ve şifre gereklidir');
      return;
    }
    
    setLoading(true);
    setError('');
    
    try {
      // User servisine signin isteği gönderiyoruz
      const response = await axios.post('http://localhost:8050/api/auth/signin', {
        username: formData.username,
        password: formData.password
      });
      
      console.log('Giriş başarılı:', response.data);
      
      // JWT token'ı localStorage'a kaydediyoruz (gerçek uygulamada)
      if (response.data.token) {
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', JSON.stringify(response.data));
      }
      
      // Kullanıcı bilgilerini sakla
      localStorage.setItem('isLoggedIn', 'true');
      
      // Dashboard'a yönlendir
      navigate('/dashboard');
      
    } catch (error) {
      console.error('Giriş hatası:', error);
      if (error.response) {
        setError(error.response.data.message || 'Kullanıcı adı veya şifre hatalı');
      } else {
        setError('Sunucu bağlantısı kurulamadı');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container component="main" maxWidth="xs">
      <Box
        sx={{
          marginTop: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Paper elevation={3} sx={{ padding: 4, width: '100%' }}>
          <Typography component="h1" variant="h5" align="center" gutterBottom>
            Akıllı Otopark Sistemi
          </Typography>
          <Typography component="h2" variant="h5" align="center" gutterBottom>
            Admin Girişi
          </Typography>
          
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          
          <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1 }}>
            <TextField
              margin="normal"
              required
              fullWidth
              label="Kullanıcı Adı"
              name="username"
              autoComplete="username"
              autoFocus
              value={formData.username}
              onChange={handleChange}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              name="password"
              label="Şifre"
              type="password"
              autoComplete="current-password"
              value={formData.password}
              onChange={handleChange}
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
              disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Giriş Yap'}
            </Button>
            <Grid container>
              <Grid item>
                <Link component={RouterLink} to="/signup" variant="body2">
                  {"Hesabınız yok mu? Kayıt olun"}
                </Link>
              </Grid>
            </Grid>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};

export default SignIn;