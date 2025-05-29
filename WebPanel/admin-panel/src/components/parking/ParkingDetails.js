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
      console.log('Parking details:', response.data);
      setParking(response.data);
      setError('');
    } catch (error) {
      console.error('Error fetching parking details:', error);
      setError('An error occurred while loading parking details');
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
          title="Parking Details" 
          breadcrumbs={[
            { text: 'Parkings', link: '/parkings' },
            { text: 'Parking Details' }
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
          Go Back
        </Button>
      </Container>
    );
  }

  if (!parking) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <PageHeader 
          title="Parking Details" 
          breadcrumbs={[
            { text: 'Parkings', link: '/parkings' },
            { text: 'Parking Details' }
          ]}
        />
        <Alert severity="warning" sx={{ mb: 2 }}>
          Parking not found
        </Alert>
        <Button 
          variant="outlined" 
          startIcon={<ArrowBackIcon />} 
          onClick={handleBackClick}
        >
          Go Back
        </Button>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <PageHeader 
        title={`${parking.name} Details`} 
        breadcrumbs={[
          { text: 'Parkings', link: '/parkings' },
          { text: `${parking.name} Details` }
        ]}
      />

      {/* Top Information Card */}
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
            Edit
          </Button>
        </Box>

        <Divider sx={{ my: 2 }} />

        <Grid container spacing={3}>
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary">
                  Capacity
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
                  <DirectionsCarIcon color="primary" sx={{ mr: 1 }} />
                  <Typography variant="h6">
                    {parking.capacity} vehicles
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary">
                  Opening Hours
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
                  Hourly Rate
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
                  <AttachMoneyIcon color="primary" sx={{ mr: 1 }} />
                  <Typography variant="h6">
                    {parking.rate} TRY
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary">
                  Size
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

        <Divider sx={{ my: 2 }} />
        
        <Typography variant="h6" gutterBottom>
          Description
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
          {parking.description || 'No description available.'}
        </Typography>
      </Paper>

      {/* Harita */}
      <Paper sx={{ mb: 3 }}>
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Location
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <MapIcon color="primary" sx={{ mr: 1 }} />
            <Typography variant="body1">
              Latitude: {parking.latitude}, Longitude: {parking.longitude}
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
              Map will be shown here (Google Maps API integration required)
            </Typography>
          </Box>
        </Box>
      </Paper>

      {/* Sekmeler */}
      <Paper sx={{ mb: 3 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={handleTabChange} aria-label="parking details tabs">
            <Tab label="Parking Spots" />
            <Tab label="Roads" />
            <Tab label="Buildings" />
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
                    <TableCell>Identifier</TableCell>
                    <TableCell>Row</TableCell>
                    <TableCell>Column</TableCell>
                    <TableCell>Sensor ID</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {parking.parkingSpots.map((spot) => (
                    <TableRow key={spot.id}>
                      <TableCell>{spot.id}</TableCell>
                      <TableCell>{spot.spotIdentifier}</TableCell>
                      <TableCell>{spot.row}</TableCell>
                      <TableCell>{spot.column}</TableCell>
                      <TableCell>{spot.sensorId || "None"}</TableCell>
                      <TableCell>
                        <Chip 
                          label={spot.occupied ? "Occupied" : "Empty"} 
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
              No parking spots defined for this parking.
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
                    <TableCell>Identifier</TableCell>
                    <TableCell>Row</TableCell>
                    <TableCell>Column</TableCell>
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
              No roads defined for this parking.
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
                    <TableCell>Row</TableCell>
                    <TableCell>Column</TableCell>
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
              No buildings defined for this parking.
            </Typography>
          )}
        </TabPanel>
      </Paper>

      {/* Resim */}
      {parking.imageUrl && (
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Parking View
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
                e.target.src = "https://via.placeholder.com/800x400?text=Parking+Image+Not+Found";
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
          Back to Parking List
        </Button>
        <Box>
          <Button 
            variant="outlined" 
            startIcon={<GridViewIcon />} 
            onClick={() => navigate(`/parking-layout/${id}`)}
            sx={{ mr: 2 }}
          >
            View Parking Layout
          </Button>
          <Button 
            variant="contained" 
            startIcon={<EditIcon />} 
            onClick={handleEditClick}
          >
            Edit Parking
          </Button>
        </Box>
      </Box>
    </Container>
  );
};

export default ParkingDetails; 