import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Container, Paper, Typography, Box, Grid, Chip, CircularProgress, Alert, 
  Card, CardContent, Divider, Table, TableBody, TableCell, TableContainer, 
  TableHead, TableRow, Button, Tabs, Tab
} from '@mui/material';
import { getParkingById } from '../../services/apiService';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import DirectionsCarIcon from '@mui/icons-material/DirectionsCar';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import GridViewIcon from '@mui/icons-material/GridView';
import MapIcon from '@mui/icons-material/Map';
import EditIcon from '@mui/icons-material/Edit';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import PageHeader from '../common/PageHeader';

// TabPanel bileşeni
function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
}

const ParkingDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [parking, setParking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tabValue, setTabValue] = useState(0);

  useEffect(() => {
    fetchParkingDetails();
  }, [id]);

  const fetchParkingDetails = async () => {
    setLoading(true);
    try {
      const response = await getParkingById(id);
      console.log('Otopark detayları:', response.data);
      setParking(response.data);
      setError('');
    } catch (error) {
      console.error('Otopark detayları alınırken hata oluştu:', error);
      setError('Otopark detayları yüklenirken bir hata oluştu');
    } finally {
      setLoading(false);
    }
  };

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  const handleEditClick = () => {
    navigate(`/edit-parking/${id}`);
  };

  const handleBackClick = () => {
    navigate('/parkings');
  };

  if (loading) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4, display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
        <CircularProgress />
      </Container>
    );
  }

  if (error) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <PageHeader 
          title="Otopark Detayları" 
          breadcrumbs={[
            { text: 'Otoparklar', link: '/parkings' },
            { text: 'Otopark Detayları' }
          ]}
        />
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
        <Button 
          variant="outlined" 
          startIcon={<ArrowBackIcon />} 
          onClick={handleBackClick}
        >
          Geri Dön
        </Button>
      </Container>
    );
  }

  if (!parking) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <PageHeader 
          title="Otopark Detayları" 
          breadcrumbs={[
            { text: 'Otoparklar', link: '/parkings' },
            { text: 'Otopark Detayları' }
          ]}
        />
        <Alert severity="warning" sx={{ mb: 2 }}>
          Otopark bulunamadı
        </Alert>
        <Button 
          variant="outlined" 
          startIcon={<ArrowBackIcon />} 
          onClick={handleBackClick}
        >
          Geri Dön
        </Button>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <PageHeader 
        title={`${parking.name} Detayları`} 
        breadcrumbs={[
          { text: 'Otoparklar', link: '/parkings' },
          { text: `${parking.name} Detayları` }
        ]}
      />

      {/* Üst Bilgi Kartı */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
          <Box>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <Typography variant="h5" component="h1">
                {parking.name}
              </Typography>
              <Chip 
                label={`ID: ${parking.id}`} 
                color="primary" 
                size="small" 
                sx={{ ml: 2 }} 
              />
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <LocationOnIcon fontSize="small" color="action" sx={{ mr: 1 }} />
              <Typography variant="body1" color="text.secondary">
                {parking.location}
              </Typography>
            </Box>
          </Box>
          <Button 
            variant="contained" 
            startIcon={<EditIcon />} 
            onClick={handleEditClick}
          >
            Düzenle
          </Button>
        </Box>

        <Divider sx={{ my: 2 }} />

        <Grid container spacing={3}>
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary">
                  Kapasite
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
                  <DirectionsCarIcon color="primary" sx={{ mr: 1 }} />
                  <Typography variant="h6">
                    {parking.capacity} araç
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary">
                  Çalışma Saatleri
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
                  <AccessTimeIcon color="primary" sx={{ mr: 1 }} />
                  <Typography variant="h6">
                    {parking.openingHours} - {parking.closingHours}
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary">
                  Saat Ücreti
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
                  <AttachMoneyIcon color="primary" sx={{ mr: 1 }} />
                  <Typography variant="h6">
                    {parking.rate} TL
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary">
                  Boyut
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
                  <GridViewIcon color="primary" sx={{ mr: 1 }} />
                  <Typography variant="h6">
                    {parking.rows} x {parking.columns}
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Paper>

      {/* Harita */}
      <Paper sx={{ mb: 3 }}>
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Konum
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <MapIcon color="primary" sx={{ mr: 1 }} />
            <Typography variant="body1">
              Enlem: {parking.latitude}, Boylam: {parking.longitude}
            </Typography>
          </Box>
          <Box 
            sx={{ 
              width: '100%', 
              height: '300px', 
              bgcolor: 'grey.200', 
              display: 'flex', 
              justifyContent: 'center', 
              alignItems: 'center' 
            }}
          >
            <Typography variant="body2" color="text.secondary">
              Harita burada gösterilecek (Google Maps API entegrasyonu gerekli)
            </Typography>
          </Box>
        </Box>
      </Paper>

      {/* Sekmeler */}
      <Paper sx={{ mb: 3 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={handleTabChange} aria-label="parking details tabs">
            <Tab label="Park Yerleri" />
            <Tab label="Yollar" />
            <Tab label="Binalar" />
          </Tabs>
        </Box>

        {/* Park Yerleri Sekmesi */}
        <TabPanel value={tabValue} index={0}>
          {parking.parkingSpots && parking.parkingSpots.length > 0 ? (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>
                    <TableCell>Tanımlayıcı</TableCell>
                    <TableCell>Satır</TableCell>
                    <TableCell>Sütun</TableCell>
                    <TableCell>Sensör ID</TableCell>
                    <TableCell>Durum</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {parking.parkingSpots.map((spot) => (
                    <TableRow key={spot.id}>
                      <TableCell>{spot.id}</TableCell>
                      <TableCell>{spot.spotIdentifier}</TableCell>
                      <TableCell>{spot.row}</TableCell>
                      <TableCell>{spot.column}</TableCell>
                      <TableCell>{spot.sensorId || "Yok"}</TableCell>
                      <TableCell>
                        <Chip 
                          label={spot.occupied ? "Dolu" : "Boş"} 
                          color={spot.occupied ? "error" : "success"} 
                          size="small" 
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <Typography variant="body1" sx={{ py: 2 }}>
              Bu otoparkta tanımlı park yeri bulunmamaktadır.
            </Typography>
          )}
        </TabPanel>

        {/* Yollar Sekmesi */}
        <TabPanel value={tabValue} index={1}>
          {parking.roads && parking.roads.length > 0 ? (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>
                    <TableCell>Tanımlayıcı</TableCell>
                    <TableCell>Satır</TableCell>
                    <TableCell>Sütun</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {parking.roads.map((road) => (
                    <TableRow key={road.id}>
                      <TableCell>{road.id}</TableCell>
                      <TableCell>{road.roadIdentifier}</TableCell>
                      <TableCell>{road.roadRow}</TableCell>
                      <TableCell>{road.roadColumn}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <Typography variant="body1" sx={{ py: 2 }}>
              Bu otoparkta tanımlı yol bulunmamaktadır.
            </Typography>
          )}
        </TabPanel>

        {/* Binalar Sekmesi */}
        <TabPanel value={tabValue} index={2}>
          {parking.buildings && parking.buildings.length > 0 ? (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>
                    <TableCell>Satır</TableCell>
                    <TableCell>Sütun</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {parking.buildings.map((building) => (
                    <TableRow key={building.id}>
                      <TableCell>{building.id}</TableCell>
                      <TableCell>{building.buildingRow}</TableCell>
                      <TableCell>{building.buildingColumn}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <Typography variant="body1" sx={{ py: 2 }}>
              Bu otoparkta tanımlı bina bulunmamaktadır.
            </Typography>
          )}
        </TabPanel>
      </Paper>

      {/* Resim */}
      {parking.imageUrl && (
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Otopark Görünümü
          </Typography>
          <Box sx={{ width: '100%', textAlign: 'center' }}>
            <img 
              src={parking.imageUrl} 
              alt={parking.name} 
              style={{ 
                maxWidth: '100%', 
                maxHeight: '400px', 
                objectFit: 'contain' 
              }} 
              onError={(e) => {
                e.target.onerror = null;
                e.target.src = "https://via.placeholder.com/800x400?text=Otopark+Resmi+Bulunamadı";
              }}
            />
          </Box>
        </Paper>
      )}

      <Box sx={{ mt: 3, display: 'flex', justifyContent: 'space-between' }}>
        <Button 
          variant="outlined" 
          startIcon={<ArrowBackIcon />} 
          onClick={handleBackClick}
        >
          Otoparklar Listesine Dön
        </Button>
        <Button 
          variant="contained" 
          startIcon={<EditIcon />} 
          onClick={handleEditClick}
        >
          Otoparkı Düzenle
        </Button>
      </Box>
    </Container>
  );
};

export default ParkingDetails; 