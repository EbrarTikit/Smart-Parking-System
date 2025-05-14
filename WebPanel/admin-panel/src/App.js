import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { createTheme, ThemeProvider } from '@mui/material/styles';
import { CssBaseline, Container, Paper, Typography, TextField, Button, Box, Grid, Link, CircularProgress, Alert } from '@mui/material';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { signIn, signUp } from './services/apiService';

// Parking components import
import AddParking from './components/parking/AddParking';
import ParkingList from './components/parking/ParkingList';
import ParkingDetails from './components/parking/ParkingDetails';
import EditParking from './components/parking/EditParking';

// Tüm bileşenleri doğrudan App.js içinde tanımlayalım
const SignIn = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = React.useState({
    username: '',
    password: ''
  });
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState('');

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
      // apiService'i kullan
      const response = await signIn({
        username: formData.username,
        password: formData.password
      });
      
      console.log('Giriş başarılı:', response.data);
      
      // Kullanıcı bilgilerini localStorage'a kaydet
      localStorage.setItem('isLoggedIn', 'true');
      localStorage.setItem('userId', response.data.id);
      localStorage.setItem('username', response.data.username);
      
      // Dashboard'a yönlendir
      navigate('/dashboard');
      
    } catch (error) {
      console.error('Giriş hatası:', error);
      
      if (error.response) {
        // Sunucudan gelen hata mesajını göster
        setError(`Hata: ${error.response.status} - ${error.response.data.message || 'Giriş başarısız'}`);
      } else if (error.request) {
        // İstek yapıldı ama cevap alınamadı
        setError('Sunucuya ulaşılamıyor. Lütfen daha sonra tekrar deneyin.');
      } else {
        // İstek yapılırken bir hata oluştu
        setError('Bir hata oluştu: ' + error.message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container component="main" maxWidth="xs">
      <Box sx={{ marginTop: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Paper elevation={3} sx={{ padding: 4, width: '100%' }}>
          <Typography component="h1" variant="h5" align="center" gutterBottom>
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

const SignUp = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = React.useState({
    username: '',
    email: '',
    password: ''
  });
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState('');
  const [success, setSuccess] = React.useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.username || !formData.email || !formData.password) {
      setError('Lütfen tüm alanları doldurun');
      return;
    }
    
    setLoading(true);
    setError('');
    
    try {
      // apiService'i kullan
      const response = await signUp({
        username: formData.username,
        email: formData.email,
        password: formData.password
      });
      
      console.log('Kayıt başarılı:', response.data);
      setSuccess('Kayıt başarılı! Giriş sayfasına yönlendiriliyorsunuz...');
      
      setTimeout(() => {
        navigate('/signin');
      }, 2000);
      
    } catch (error) {
      console.error('Kayıt hatası:', error);
      
      if (error.response) {
        // Sunucudan gelen hata mesajını göster
        setError(`Hata: ${error.response.status} - ${error.response.data.message || 'Kayıt başarısız'}`);
      } else if (error.request) {
        // İstek yapıldı ama cevap alınamadı
        setError('Sunucuya ulaşılamıyor. Lütfen daha sonra tekrar deneyin.');
      } else {
        // İstek yapılırken bir hata oluştu
        setError('Bir hata oluştu: ' + error.message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container component="main" maxWidth="xs">
      <Box sx={{ marginTop: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Paper elevation={3} sx={{ padding: 4, width: '100%' }}>
          <Typography component="h1" variant="h5" align="center" gutterBottom>
            Admin Kaydı
          </Typography>
          
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          
          {success && (
            <Alert severity="success" sx={{ mb: 2 }}>
              {success}
            </Alert>
          )}
          
          <Box component="form" onSubmit={handleSubmit} sx={{ mt: 3 }}>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  label="Kullanıcı Adı"
                  name="username"
                  value={formData.username}
                  onChange={handleChange}
                  autoFocus
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  label="Email Adresi"
                  name="email"
                  type="email"
                  value={formData.email}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  name="password"
                  label="Şifre"
                  type="password"
                  value={formData.password}
                  onChange={handleChange}
                />
              </Grid>
            </Grid>
            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
              disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Kayıt Ol'}
            </Button>
            <Grid container justifyContent="flex-end">
              <Grid item>
                <Link component={RouterLink} to="/signin" variant="body2">
                  Zaten bir hesabınız var mı? Giriş yapın
                </Link>
              </Grid>
            </Grid>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};

const Dashboard = () => {
  const navigate = useNavigate();
  
  const handleLogout = () => {
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('authToken');
    navigate('/signin');
  };
  
  const saveCheckpoint = () => {
    localStorage.setItem('lastCheckpoint', new Date().toISOString());
    alert('Checkpoint kaydedildi!');
  };
  
  const restoreCheckpoint = () => {
    const lastCheckpoint = localStorage.getItem('lastCheckpoint');
    if (lastCheckpoint) {
      alert(`Son checkpoint: ${new Date(lastCheckpoint).toLocaleString()}`);
    } else {
      alert('Kaydedilmiş checkpoint bulunamadı!');
    }
  };
  
  return (
    <Container maxWidth="md">
      <Box sx={{ my: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Otopark Yönetim Paneli
        </Typography>
        
        <Grid container spacing={3} sx={{ mt: 2 }}>
          <Grid item xs={12} md={6}>
            <Paper 
              elevation={3} 
              sx={{ 
                p: 3, 
                textAlign: 'center',
                cursor: 'pointer',
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
        
        <Box sx={{ mt: 4, display: 'flex', justifyContent: 'space-between' }}>
          <Box>
            <Button 
              variant="outlined" 
              color="primary" 
              onClick={saveCheckpoint}
              sx={{ mr: 2 }}
            >
              Checkpoint Kaydet
            </Button>
            <Button 
              variant="outlined" 
              color="secondary" 
              onClick={restoreCheckpoint}
            >
              Checkpoint Görüntüle
            </Button>
          </Box>
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

// Tema oluştur
const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
});

// Protected route component
const ProtectedRoute = ({ children }) => {
  const navigate = useNavigate();
  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';
  const username = localStorage.getItem('username');
  
  React.useEffect(() => {
    if (!isLoggedIn || !username) {
      navigate('/signin');
    }
  }, [isLoggedIn, username, navigate]);
  
  if (!isLoggedIn || !username) {
    return null;
  }
  
  return children;
};

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <Routes>
          <Route path="/signin" element={<SignIn />} />
          <Route path="/signup" element={<SignUp />} />
          <Route 
            path="/dashboard" 
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/add-parking" 
            element={
              <ProtectedRoute>
                <AddParking />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/parkings" 
            element={
              <ProtectedRoute>
                <ParkingList />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/parking-details/:id" 
            element={
              <ProtectedRoute>
                <ParkingDetails />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/edit-parking/:id" 
            element={
              <ProtectedRoute>
                <EditParking />
              </ProtectedRoute>
            } 
          />
          <Route path="/" element={<Navigate to="/signin" />} />
          <Route path="*" element={<Navigate to="/signin" />} />
        </Routes>
      </Router>
    </ThemeProvider>
  );
}

export default App;