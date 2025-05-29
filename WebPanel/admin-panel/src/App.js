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
import ParkingLayout from './components/parking/ParkingLayout';
import Dashboard from './components/Dashboard';
import SensorList from './components/sensors/SensorList';

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
      setError('Username and password are required');
      return;
    }
    
    setLoading(true);
    setError('');
    
    try {
      const response = await signIn({
        username: formData.username,
        password: formData.password
      });
      
      if (!response || !response.data) {
        throw new Error('Invalid API response');
      }

      // Token ve kullanıcı bilgilerini localStorage'a kaydet
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('userId', response.data.userId);
      localStorage.setItem('username', formData.username);
      localStorage.setItem('isLoggedIn', 'true');
      
      // API isteklerinde token'ı kullan
      axios.defaults.headers.common['Authorization'] = `Bearer ${response.data.token}`;
      
      // Yönlendirme
      navigate('/dashboard', { replace: true });
      
    } catch (error) {
      console.error('Login error:', error);
      
      if (error.response && error.response.data) {
        setError(`Error: ${error.response.status} - ${error.response.data.message || 'Login failed'}`);
      } else if (error.request) {
        setError('Could not reach server. Please try again later.');
      } else {
        setError('An error occurred: ' + (error.message || 'Unknown error'));
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
            Admin Login
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
              label="Username"
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
              label="Password"
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
              {loading ? <CircularProgress size={24} /> : 'Login'}
            </Button>
            <Grid container>
              <Grid item>
                <Link component={RouterLink} to="/signup" variant="body2">
                  {"Don't have an account? Sign Up"}
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
      setError('Please fill in all fields');
      return;
    }
    
    setLoading(true);
    setError('');
    
    try {
      const response = await signUp({
        username: formData.username,
        email: formData.email,
        password: formData.password
      });
      
      console.log('Registration successful:', response);
      
      // Kullanıcı bilgilerini localStorage'a kaydet
      if (response && response.id) {
        localStorage.setItem('userId', response.id);
        localStorage.setItem('username', response.username || formData.username);
        localStorage.setItem('userEmail', response.email || formData.email);
      }
      
      setSuccess('Registration successful! Redirecting to login page...');
      
      setTimeout(() => {
        navigate('/signin');
      }, 2000);
      
    } catch (error) {
      console.error('Registration error:', error);
      
      if (error.response) {
        setError(`Error: ${error.response.status} - ${error.response.data.message || 'Registration failed'}`);
      } else if (error.request) {
        setError('Could not reach server. Please try again later.');
      } else {
        setError('An error occurred: ' + error.message);
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
            Admin Registration
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
                  label="Username"
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
                  label="Email Address"
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
                  label="Password"
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
              {loading ? <CircularProgress size={24} /> : 'Sign Up'}
            </Button>
            <Grid container justifyContent="flex-end">
              <Grid item>
                <Link component={RouterLink} to="/signin" variant="body2">
                  {"Already have an account? Sign In"}
                </Link>
              </Grid>
            </Grid>
          </Box>
        </Paper>
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
  const token = localStorage.getItem('token');
  const username = localStorage.getItem('username');
  
  React.useEffect(() => {
    if (!token || !username) {
      navigate('/signin', { replace: true });
    }
  }, [token, username, navigate]);
  
  if (!token || !username) {
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
          <Route 
            path="/parking-layout/:id" 
            element={
              <ProtectedRoute>
                <ParkingLayout />
              </ProtectedRoute>
            } 
          />
          <Route path="/sensors" element={<SensorList />} />
          <Route path="/" element={<Navigate to="/signin" />} />
          <Route path="*" element={<Navigate to="/signin" />} />
        </Routes>
      </Router>
    </ThemeProvider>
  );
}

export default App;