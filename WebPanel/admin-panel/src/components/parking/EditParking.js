import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Container, Paper, Typography, TextField, Button, Box, Grid, Alert, 
  CircularProgress, Divider
} from '@mui/material';
import { getParkingById, updateParking } from '../../services/apiService';
import PageHeader from '../common/PageHeader';
import SaveIcon from '@mui/icons-material/Save';
import CancelIcon from '@mui/icons-material/Cancel';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';

const EditParking = () => {
  const { id } = useParams();
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
  
  const [originalData, setOriginalData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [hasChanges, setHasChanges] = useState(false);

  useEffect(() => {
    fetchParkingDetails();
  }, [id]);

  // Track form changes
  useEffect(() => {
    if (originalData) {
      const changed = Object.keys(formData).some(key => 
        formData[key] !== originalData[key]
      );
      setHasChanges(changed);
    }
  }, [formData, originalData]);

  const fetchParkingDetails = async () => {
    setLoading(true);
    try {
      const response = await getParkingById(id);
      console.log('Parking details:', response.data);
      
      // Convert API data to form data
      const parkingData = {
        name: response.data.name || '',
        location: response.data.location || '',
        capacity: response.data.capacity || '',
        openingHours: response.data.openingHours || '',
        closingHours: response.data.closingHours || '',
        rate: response.data.rate || '',
        latitude: response.data.latitude || '',
        longitude: response.data.longitude || '',
        rows: response.data.rows || '',
        columns: response.data.columns || '',
        imageUrl: response.data.imageUrl || '',
        description: response.data.description || ''
      };
      
      setFormData(parkingData);
      setOriginalData(parkingData);
      setError('');
    } catch (error) {
      console.error('Error fetching parking details:', error);
      setError('An error occurred while loading parking details');
    } finally {
      setLoading(false);
    }
  };

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
    
    setSaving(true);
    setError('');
    
    try {
      const response = await updateParking(id, parkingData);
      
      console.log('Parking update successful:', response.data);
      setSuccess('Parking updated successfully!');
      
      // Update original data
      setOriginalData({...formData});
      setHasChanges(false);
      
      // Redirect to details page after showing success message
      setTimeout(() => {
        navigate(`/parking-details/${id}`);
      }, 2000);
      
    } catch (error) {
      console.error('Parking update error:', error);
      
      if (error.response) {
        setError(`Update error: ${error.response.status} - ${error.response.data.message || 'Unknown error'}`);
      } else {
        setError('An error occurred while updating the parking: ' + error.message);
      }
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    if (hasChanges) {
      if (window.confirm('You have unsaved changes. Are you sure you want to leave?')) {
        navigate(`/parking-details/${id}`);
      }
    } else {
      navigate(`/parking-details/${id}`);
    }
  };

  const handleReset = () => {
    if (window.confirm('Are you sure you want to revert all changes?')) {
      setFormData({...originalData});
    }
  };

  if (loading) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4, display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
        <CircularProgress />
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <PageHeader 
        title={`Edit ${formData.name}`} 
        breadcrumbs={[
          { text: 'Parkings', link: '/parkings' },
          { text: `${formData.name}`, link: `/parking-details/${id}` },
          { text: 'Edit' }
        ]}
      />
      
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
      
      <Paper elevation={3} sx={{ p: 4 }}>
        <Box component="form" onSubmit={handleSubmit}>
          <Typography variant="h6" gutterBottom>
            Basic Information
          </Typography>
          
          <Grid container spacing={3}>
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
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Description"
                name="description"
                multiline
                rows={4}
                value={formData.description}
                onChange={handleChange}
                placeholder="Enter detailed information about the parking..."
              />
            </Grid>
          </Grid>
          
          <Divider sx={{ my: 3 }} />
          
          <Typography variant="h6" gutterBottom>
            Additional Information
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Hourly Rate (TRY)"
                name="rate"
                type="number"
                step="0.01"
                value={formData.rate}
                onChange={handleChange}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Image URL"
                name="imageUrl"
                value={formData.imageUrl}
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
            <Grid item xs={12} sm={2}>
              <TextField
                fullWidth
                label="Rows"
                name="rows"
                type="number"
                value={formData.rows}
                onChange={handleChange}
              />
            </Grid>
            <Grid item xs={12} sm={2}>
              <TextField
                fullWidth
                label="Columns"
                name="columns"
                type="number"
                value={formData.columns}
                onChange={handleChange}
              />
            </Grid>
          </Grid>
          
          <Box sx={{ mt: 4, display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
            <Button
              variant="outlined"
              startIcon={<CancelIcon />}
              onClick={handleCancel}
            >
              Cancel
            </Button>
            <Button
              variant="outlined"
              onClick={handleReset}
              disabled={!hasChanges}
            >
              Reset
            </Button>
            <Button
              type="submit"
              variant="contained"
              startIcon={<SaveIcon />}
              disabled={!hasChanges || saving}
            >
              {saving ? 'Saving...' : 'Save Changes'}
            </Button>
          </Box>
        </Box>
      </Paper>
    </Container>
  );
};

export default EditParking; 