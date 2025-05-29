import React, { useState, useEffect } from 'react';
import { 
  Container, Paper, Typography, Box, Grid, Card, CardContent, 
  CardMedia, CardActions, Button, Chip, CircularProgress, Alert, 
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle,
  IconButton, Tooltip, TextField, InputAdornment
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { getParkings, deleteParking } from '../../services/apiService';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import DirectionsCarIcon from '@mui/icons-material/DirectionsCar';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import VisibilityIcon from '@mui/icons-material/Visibility';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';
import PageHeader from '../common/PageHeader';
import GridViewIcon from '@mui/icons-material/GridView';

const ParkingList = () => {
  const navigate = useNavigate();
  const [parkings, setParkings] = useState([]);
  const [filteredParkings, setFilteredParkings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [viewMode, setViewMode] = useState('card'); // 'card' or 'table'
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [parkingToDelete, setParkingToDelete] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchParkings();
  }, []);

  // Filter when search term changes
  useEffect(() => {
    if (!parkings.length) {
      setFilteredParkings([]);
      return;
    }

    if (!searchTerm.trim()) {
      setFilteredParkings(parkings);
      return;
    }

    const lowercasedSearch = searchTerm.toLowerCase();
    const filtered = parkings.filter(parking => 
      parking.id.toString().includes(lowercasedSearch) || 
      parking.name.toLowerCase().includes(lowercasedSearch) ||
      parking.location.toLowerCase().includes(lowercasedSearch)
    );
    
    setFilteredParkings(filtered);
  }, [searchTerm, parkings]);

  const fetchParkings = async () => {
    setLoading(true);
    try {
      const response = await getParkings();
      console.log('Parkings:', response.data);
      setParkings(response.data);
      setFilteredParkings(response.data);
      setError('');
    } catch (error) {
      console.error('Error fetching parking list:', error);
      setError('An error occurred while loading parkings');
    } finally {
      setLoading(false);
    }
  };

  const handleEditParking = (id) => {
    navigate(`/edit-parking/${id}`);
  };

  const handleViewDetails = (id) => {
    navigate(`/parking-details/${id}`);
  };

  const handleAddParking = () => {
    navigate('/add-parking');
  };

  const handleToggleView = () => {
    setViewMode(viewMode === 'card' ? 'table' : 'card');
  };
  
  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const handleClearSearch = () => {
    setSearchTerm('');
  };
  
  // Delete function
  const handleDeleteClick = (parking) => {
    setParkingToDelete(parking);
    setDeleteDialogOpen(true);
  };
  
  const handleDeleteCancel = () => {
    setParkingToDelete(null);
    setDeleteDialogOpen(false);
  };
  
  const handleDeleteConfirm = async () => {
    if (!parkingToDelete) return;
    
    setDeleteLoading(true);
    try {
      await deleteParking(parkingToDelete.id);
      
      // When deletion is successful, remove the parking from the list
      setParkings(prevParkings => prevParkings.filter(p => p.id !== parkingToDelete.id));
      setSuccessMessage(`${parkingToDelete.name} deleted successfully.`);
      
      // Auto-close success message after 3 seconds
      setTimeout(() => {
        setSuccessMessage('');
      }, 3000);
    } catch (error) {
      console.error('Could not delete parking:', error);
      setError(`Could not delete parking: ${error.response?.data?.message || error.message}`);
      
      // Auto-close error message after 5 seconds
      setTimeout(() => {
        setError('');
      }, 5000);
    } finally {
      setDeleteLoading(false);
      setDeleteDialogOpen(false);
      setParkingToDelete(null);
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
        title="Parkings" 
        breadcrumbs={[
          { text: 'Parkings' }
        ]}
      />
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      
      {successMessage && (
        <Alert severity="success" sx={{ mb: 2 }}>
          {successMessage}
        </Alert>
      )}
      
      {/* Search bar */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <TextField
          fullWidth
          variant="outlined"
          placeholder="Search parking by ID, name, or location..."
          value={searchTerm}
          onChange={handleSearchChange}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
            endAdornment: searchTerm && (
              <InputAdornment position="end">
                <IconButton onClick={handleClearSearch} size="small">
                  <ClearIcon />
                </IconButton>
              </InputAdornment>
            )
          }}
        />
      </Paper>
      
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="body2">
          {filteredParkings.length} parkings found
        </Typography>
        <Box>
          <Button 
            variant="outlined" 
            sx={{ mr: 2 }}
            onClick={handleToggleView}
          >
            {viewMode === 'card' ? 'Table View' : 'Card View'}
          </Button>
          <Button 
            variant="contained" 
            onClick={handleAddParking}
          >
            Add New Parking
          </Button>
        </Box>
      </Box>

      {filteredParkings.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          {searchTerm ? (
            <>
              <Typography variant="h6" sx={{ mb: 2 }}>No parkings found matching your search</Typography>
              <Button variant="outlined" onClick={handleClearSearch}>Clear Search</Button>
            </>
          ) : (
            <>
              <Typography variant="h6" sx={{ mb: 2 }}>No parkings available yet</Typography>
              <Button variant="contained" onClick={handleAddParking}>Add First Parking</Button>
            </>
          )}
        </Paper>
      ) : viewMode === 'card' ? (
        <Grid container spacing={3}>
          {filteredParkings.map((parking) => (
            <Grid item key={parking.id} xs={12} sm={6} md={4} lg={4}>
              <Card
                sx={{ height: '100%', display: 'flex', flexDirection: 'column', cursor: 'pointer' }}
                onClick={() => handleViewDetails(parking.id)}
              >
                {parking.imageUrl && (
                  <CardMedia
                    component="img"
                    height="140"
                    image={parking.imageUrl}
                    alt={parking.name}
                    sx={{ objectFit: 'cover' }}
                  />
                )}
                <CardContent sx={{ flexGrow: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                    <Typography gutterBottom variant="h6" component="div" sx={{ flexGrow: 1 }}>
                      {parking.name}
                    </Typography>
                    <Chip label={`ID: ${parking.id}`} color="primary" size="small" />
                  </Box>

                  {parking.description && (
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1, whiteSpace: 'pre-wrap',  display: '-webkit-box', WebkitLineClamp: 3, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                      {parking.description}
                    </Typography>
                  )}

                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
                    <LocationOnIcon fontSize="small" color="action" sx={{ mr: 1 }} />
                    <Typography variant="body2" color="text.secondary">
                      {parking.location}
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
                    <DirectionsCarIcon fontSize="small" color="action" sx={{ mr: 1 }} />
                    <Typography variant="body2" color="text.secondary">
                      Capacity: {parking.capacity} vehicles
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
                    <AccessTimeIcon fontSize="small" color="action" sx={{ mr: 1 }} />
                    <Typography variant="body2" color="text.secondary">
                      {parking.openingHours} - {parking.closingHours}
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <AttachMoneyIcon fontSize="small" color="action" sx={{ mr: 1 }} />
                    <Typography variant="body2" color="text.secondary">
                      Hourly rate: {parking.rate} TL
                    </Typography>
                  </Box>
                </CardContent>
                <CardActions>
                  <Box sx={{ flexGrow: 1 }}>
                    <Tooltip title="Edit">
                      <IconButton onClick={(e) => { e.stopPropagation(); handleEditParking(parking.id); }} color="info">
                        <EditIcon />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="View Layout">
                      <IconButton onClick={(e) => { e.stopPropagation(); navigate(`/parking-layout/${parking.id}`); }} color="success">
                        <GridViewIcon />
                      </IconButton>
                    </Tooltip>
                  </Box>
                  <Tooltip title="Delete">
                    <IconButton onClick={(e) => { e.stopPropagation(); handleDeleteClick(parking); }} color="error">
                      <DeleteIcon />
                    </IconButton>
                  </Tooltip>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Location</TableCell>
                <TableCell>Capacity</TableCell>
                <TableCell>Opening Hours</TableCell>
                <TableCell>Hourly Rate</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredParkings.map((parking) => (
                <TableRow key={parking.id}>
                  <TableCell>{parking.id}</TableCell>
                  <TableCell>{parking.name}</TableCell>
                  <TableCell>{parking.location}</TableCell>
                  <TableCell>{parking.capacity}</TableCell>
                  <TableCell>{`${parking.openingHours} - ${parking.closingHours}`}</TableCell>
                  <TableCell>{`${parking.rate} TL`}</TableCell>
                  <TableCell>
                    <Tooltip title="View Details">
                      <IconButton onClick={() => handleViewDetails(parking.id)} size="small">
                        <VisibilityIcon color="primary" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Edit">
                      <IconButton onClick={() => handleEditParking(parking.id)} size="small" sx={{ ml: 1 }}>
                        <EditIcon color="action" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Delete">
                      <IconButton onClick={() => handleDeleteClick(parking)} size="small" sx={{ ml: 1 }}>
                        <DeleteIcon color="error" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
      
      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
        aria-labelledby="delete-dialog-title"
        aria-describedby="delete-dialog-description"
      >
        <DialogTitle id="delete-dialog-title">{"Delete Parking"}</DialogTitle>
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            {`Are you sure you want to delete the parking "${parkingToDelete?.name}"? This action cannot be undone.`}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel} disabled={deleteLoading}>Cancel</Button>
          <Button onClick={handleDeleteConfirm} color="error" disabled={deleteLoading}>
            {deleteLoading ? <CircularProgress size={24} /> : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default ParkingList; 