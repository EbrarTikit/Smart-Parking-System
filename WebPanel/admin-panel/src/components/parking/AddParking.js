import React, { useState } from 'react';
import { Container, Paper, TextField, Button, Box, Grid, Alert, CircularProgress } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { addParking } from '../../services/apiService';
import PageHeader from '../common/PageHeader';

const AddParking = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    name: '',
    location: '',
    capacity: '',
    openingHours: '',
    closingHours: '',
    rate: '',
    latitude: '',
    longitude: '',
    rows: '',
    columns: '',
    imageUrl: '',
    description: ''
  });
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Basic validation
    if (!formData.name || !formData.location || !formData.capacity) {
      setError('Please fill in the required fields: Name, Location, and Capacity');
      return;
    }
    
    // Convert numeric fields from string to proper types
    const parkingData = {
      ...formData,
      capacity: parseInt(formData.capacity, 10),
      rate: parseFloat(formData.rate),
      latitude: parseFloat(formData.latitude),
      longitude: parseFloat(formData.longitude),
      rows: parseInt(formData.rows, 10),
      columns: parseInt(formData.columns, 10)
    };
    
    setLoading(true);
    setError('');
    
    try {
      const response = await addParking(parkingData);
      
      console.log('Parking added successfully:', response.data);
      setSuccess('Parking added successfully!');
      
      // Reset form data
      setFormData({
        name: '',
        location: '',
        capacity: '',
        openingHours: '',
        closingHours: '',
        rate: '',
        latitude: '',
        longitude: '',
        rows: '',
        columns: '',
        imageUrl: '',
        description: ''
      });
      
      setTimeout(() => {
        navigate('/dashboard');
      }, 2000);
      
    } catch (error) {
      console.error('Parking addition error:', error);
      setError('An error occurred while adding the parking');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container component="main" maxWidth="md">
      <Box sx={{ marginTop: 4 }}>
        <PageHeader 
          title="Add New Parking" 
          breadcrumbs={[
            { text: 'Parkings', link: '/parkings' },
            { text: 'Add New Parking' }
          ]}
        />
        
        <Paper elevation={3} sx={{ padding: 4, width: '100%' }}>
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
              <Grid item xs={12} sm={6}>
                <TextField
                  required
                  fullWidth
                  label="Parking Name"
                  name="name"
                  value={formData.name}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  required
                  fullWidth
                  label="Location"
                  name="location"
                  value={formData.location}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  required
                  fullWidth
                  label="Capacity"
                  name="capacity"
                  type="number"
                  value={formData.capacity}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  fullWidth
                  label="Opening Hours (e.g. 08:00)"
                  name="openingHours"
                  value={formData.openingHours}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  fullWidth
                  label="Closing Hours (e.g. 22:00)"
                  name="closingHours"
                  value={formData.closingHours}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  fullWidth
                  label="Rate (per hour)"
                  name="rate"
                  type="number"
                  step="0.01"
                  value={formData.rate}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  fullWidth
                  label="Latitude"
                  name="latitude"
                  type="number"
                  step="0.0001"
                  value={formData.latitude}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  fullWidth
                  label="Longitude"
                  name="longitude"
                  type="number"
                  step="0.0001"
                  value={formData.longitude}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Number of Rows"
                  name="rows"
                  type="number"
                  value={formData.rows}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Number of Columns"
                  name="columns"
                  type="number"
                  value={formData.columns}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Image URL"
                  name="imageUrl"
                  value={formData.imageUrl}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Description"
                  name="description"
                  value={formData.description}
                  onChange={handleChange}
                  placeholder="Enter detailed information about the parking..."
                />
              </Grid>
            </Grid>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
              <Button
                variant="outlined"
                onClick={() => navigate('/dashboard')}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                variant="contained"
                disabled={loading}
              >
                {loading ? <CircularProgress size={24} /> : 'Add'}
              </Button>
            </Box>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};

export default AddParking; 