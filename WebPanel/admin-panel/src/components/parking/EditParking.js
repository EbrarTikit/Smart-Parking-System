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

  // Form değişikliklerini izle
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
      console.log('Otopark detayları:', response.data);
      
      // API'den gelen verileri form verilerine dönüştür
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
      console.error('Otopark detayları alınırken hata oluştu:', error);
      setError('Otopark detayları yüklenirken bir hata oluştu');
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
      setError('Lütfen zorunlu alanları doldurun: İsim, Konum ve Kapasite');
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
      
      console.log('Otopark güncelleme başarılı:', response.data);
      setSuccess('Otopark başarıyla güncellendi!');
      
      // Orijinal verileri güncelle
      setOriginalData({...formData});
      setHasChanges(false);
      
      // Başarı mesajını gösterdikten sonra detay sayfasına yönlendir
      setTimeout(() => {
        navigate(`/parking-details/${id}`);
      }, 2000);
      
    } catch (error) {
      console.error('Otopark güncelleme hatası:', error);
      
      if (error.response) {
        setError(`Güncelleme hatası: ${error.response.status} - ${error.response.data.message || 'Bilinmeyen hata'}`);
      } else {
        setError('Otopark güncellenirken bir hata oluştu: ' + error.message);
      }
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    if (hasChanges) {
      if (window.confirm('Kaydedilmemiş değişiklikler var. Çıkmak istediğinizden emin misiniz?')) {
        navigate(`/parking-details/${id}`);
      }
    } else {
      navigate(`/parking-details/${id}`);
    }
  };

  const handleReset = () => {
    if (window.confirm('Tüm değişiklikleri geri almak istediğinizden emin misiniz?')) {
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
        title={`${formData.name} Düzenle`} 
        breadcrumbs={[
          { text: 'Otoparklar', link: '/parkings' },
          { text: `${formData.name}`, link: `/parking-details/${id}` },
          { text: 'Düzenle' }
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
            Temel Bilgiler
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField
                required
                fullWidth
                label="Otopark Adı"
                name="name"
                value={formData.name}
                onChange={handleChange}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                required
                fullWidth
                label="Konum"
                name="location"
                value={formData.location}
                onChange={handleChange}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                required
                fullWidth
                label="Kapasite"
                name="capacity"
                type="number"
                value={formData.capacity}
                onChange={handleChange}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="Açılış Saati (ÖR: 08:00)"
                name="openingHours"
                value={formData.openingHours}
                onChange={handleChange}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="Kapanış Saati (ÖR: 22:00)"
                name="closingHours"
                value={formData.closingHours}
                onChange={handleChange}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Açıklama"
                name="description"
                multiline
                rows={4}
                value={formData.description}
                onChange={handleChange}
                placeholder="Otopark hakkında detaylı bilgi giriniz..."
              />
            </Grid>
          </Grid>
          
          <Divider sx={{ my: 3 }} />
          
          <Typography variant="h6" gutterBottom>
            Fiyatlandırma
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="Ücret (Saat Başı)"
                name="rate"
                type="number"
                step="0.01"
                value={formData.rate}
                onChange={handleChange}
              />
            </Grid>
          </Grid>
          
          <Divider sx={{ my: 3 }} />
          
          <Typography variant="h6" gutterBottom>
            Konum Bilgileri
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Enlem (Latitude)"
                name="latitude"
                type="number"
                step="0.0001"
                value={formData.latitude}
                onChange={handleChange}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Boylam (Longitude)"
                name="longitude"
                type="number"
                step="0.0001"
                value={formData.longitude}
                onChange={handleChange}
              />
            </Grid>
          </Grid>
          
          <Divider sx={{ my: 3 }} />
          
          <Typography variant="h6" gutterBottom>
            Yerleşim Bilgileri
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Satır Sayısı"
                name="rows"
                type="number"
                value={formData.rows}
                onChange={handleChange}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Sütun Sayısı"
                name="columns"
                type="number"
                value={formData.columns}
                onChange={handleChange}
              />
            </Grid>
          </Grid>
          
          <Divider sx={{ my: 3 }} />
          
          <Typography variant="h6" gutterBottom>
            Görsel
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Resim URL"
                name="imageUrl"
                value={formData.imageUrl}
                onChange={handleChange}
              />
            </Grid>
            {formData.imageUrl && (
              <Grid item xs={12}>
                <Box sx={{ width: '100%', textAlign: 'center', mt: 2 }}>
                  <img 
                    src={formData.imageUrl} 
                    alt="Otopark Önizleme" 
                    style={{ 
                      maxWidth: '100%', 
                      maxHeight: '200px', 
                      objectFit: 'contain' 
                    }} 
                    onError={(e) => {
                      e.target.onerror = null;
                      e.target.src = "https://via.placeholder.com/400x200?text=Resim+Yüklenemedi";
                    }}
                  />
                </Box>
              </Grid>
            )}
          </Grid>
          
          <Box sx={{ mt: 4, display: 'flex', justifyContent: 'space-between' }}>
            <Box>
              <Button
                variant="outlined"
                color="secondary"
                startIcon={<ArrowBackIcon />}
                onClick={handleCancel}
                sx={{ mr: 2 }}
              >
                İptal
              </Button>
              <Button
                variant="outlined"
                color="warning"
                onClick={handleReset}
                disabled={!hasChanges}
              >
                Değişiklikleri Geri Al
              </Button>
            </Box>
            <Button
              type="submit"
              variant="contained"
              startIcon={<SaveIcon />}
              disabled={saving || !hasChanges}
            >
              {saving ? <CircularProgress size={24} /> : 'Değişiklikleri Kaydet'}
            </Button>
          </Box>
        </Box>
      </Paper>
    </Container>
  );
};

export default EditParking; 