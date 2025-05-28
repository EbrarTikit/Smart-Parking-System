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

const ParkingList = () => {
  const navigate = useNavigate();
  const [parkings, setParkings] = useState([]);
  const [filteredParkings, setFilteredParkings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [viewMode, setViewMode] = useState('card'); // 'card' veya 'table'
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [parkingToDelete, setParkingToDelete] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchParkings();
  }, []);

  // Arama terimi değiştiğinde filtreleme yap
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
      console.log('Otoparklar:', response.data);
      setParkings(response.data);
      setFilteredParkings(response.data);
      setError('');
    } catch (error) {
      console.error('Otopark listesi alınırken hata oluştu:', error);
      setError('Otoparklar yüklenirken bir hata oluştu');
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
  
  // Silme işlevi
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
      
      // Silme işlemi başarılı olduğunda, listeden otoparkı çıkar
      setParkings(prevParkings => prevParkings.filter(p => p.id !== parkingToDelete.id));
      setSuccessMessage(`${parkingToDelete.name} otoparkı başarıyla silindi.`);
      
      // Başarı mesajını 3 saniye sonra otomatik kapat
      setTimeout(() => {
        setSuccessMessage('');
      }, 3000);
    } catch (error) {
      console.error('Otopark silinemedi:', error);
      setError(`Otopark silinemedi: ${error.response?.data?.message || error.message}`);
      
      // Hata mesajını 5 saniye sonra otomatik kapat
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
        title="Otoparklar" 
        breadcrumbs={[
          { text: 'Otoparklar' }
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
      
      {/* Arama çubuğu */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <TextField
          fullWidth
          variant="outlined"
          placeholder="ID, isim veya konum ile otopark ara..."
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
          {filteredParkings.length} otopark bulundu
        </Typography>
        <Box>
          <Button 
            variant="outlined" 
            sx={{ mr: 2 }}
            onClick={handleToggleView}
          >
            {viewMode === 'card' ? 'Tablo Görünümü' : 'Kart Görünümü'}
          </Button>
          <Button 
            variant="contained" 
            onClick={handleAddParking}
          >
            Yeni Otopark Ekle
          </Button>
        </Box>
      </Box>

      {filteredParkings.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          {searchTerm ? (
            <>
              <Typography variant="h6" sx={{ mb: 2 }}>Aramanızla eşleşen otopark bulunamadı</Typography>
              <Button variant="outlined" onClick={handleClearSearch}>Aramayı Temizle</Button>
            </>
          ) : (
            <>
              <Typography variant="h6" sx={{ mb: 2 }}>Henüz hiç otopark bulunmuyor</Typography>
              <Button variant="contained" onClick={handleAddParking}>İlk Otoparkı Ekle</Button>
            </>
          )}
        </Paper>
      ) : viewMode === 'card' ? (
        <Grid container spacing={3}>
          {filteredParkings.map((parking) => (
            <Grid item xs={12} sm={6} md={4} key={parking.id}>
              <Card sx={{ 
                height: '100%', 
                display: 'flex', 
                flexDirection: 'column',
                transition: 'transform 0.3s, box-shadow 0.3s',
                '&:hover': {
                  transform: 'translateY(-5px)',
                  boxShadow: 6,
                }
              }}>
                <CardMedia
                  component="img"
                  height="140"
                  image={parking.imageUrl || "https://source.unsplash.com/random?parking"}
                  alt={parking.name}
                />
                <CardContent sx={{ flexGrow: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                    <Typography gutterBottom variant="h6" component="div" sx={{ flexGrow: 1 }}>
                      {parking.name}
                    </Typography>
                    <Chip label={`ID: ${parking.id}`} color="primary" size="small" />
                  </Box>

                  {/* Description alanını buraya ekleyelim */}
                  {parking.description && (
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1, whiteSpace: 'pre-wrap' }}>
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
                      Kapasite: {parking.capacity} araç
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
                      Saat ücreti: {parking.rate} TL
                    </Typography>
                  </Box>
                </CardContent>
                <CardActions sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Box>
                    <Tooltip title="Detaylar">
                      <IconButton 
                        size="small" 
                        onClick={() => handleViewDetails(parking.id)}
                        color="primary"
                      >
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Düzenle">
                      <IconButton 
                        size="small" 
                        onClick={() => handleEditParking(parking.id)}
                        color="primary"
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Box>
                  <Tooltip title="Sil">
                    <IconButton 
                      size="small" 
                      onClick={() => handleDeleteClick(parking)}
                      color="error"
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      ) : (
        <TableContainer component={Paper}>
          <Table sx={{ minWidth: 650 }}>
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>İsim</TableCell>
                <TableCell>Konum</TableCell>
                <TableCell align="right">Kapasite</TableCell>
                <TableCell>Çalışma Saatleri</TableCell>
                <TableCell align="right">Saat Ücreti</TableCell>
                <TableCell align="center">İşlemler</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredParkings.map((parking) => (
                <TableRow
                  key={parking.id}
                  sx={{ '&:last-child td, &:last-child th': { border: 0 } }}
                >
                  <TableCell>{parking.id}</TableCell>
                  <TableCell component="th" scope="row">
                    {parking.name}
                  </TableCell>
                  <TableCell>{parking.location}</TableCell>
                  <TableCell align="right">{parking.capacity}</TableCell>
                  <TableCell>{parking.openingHours} - {parking.closingHours}</TableCell>
                  <TableCell align="right">{parking.rate} TL</TableCell>
                  <TableCell align="center">
                    <Tooltip title="Detaylar">
                      <IconButton 
                        size="small" 
                        onClick={() => handleViewDetails(parking.id)}
                        color="primary"
                        sx={{ mr: 1 }}
                      >
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Düzenle">
                      <IconButton 
                        size="small" 
                        onClick={() => handleEditParking(parking.id)}
                        color="primary"
                        sx={{ mr: 1 }}
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Sil">
                      <IconButton 
                        size="small" 
                        onClick={() => handleDeleteClick(parking)}
                        color="error"
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
      
      {/* Silme Onay Dialog'u */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title">
          {"Otoparkı silmek istediğinize emin misiniz?"}
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            {parkingToDelete && (
              <>
                <strong>{parkingToDelete.name}</strong> otoparkını silmek üzeresiniz. 
                Bu işlem geri alınamaz ve tüm otopark verileri kalıcı olarak silinecektir.
              </>
            )}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel} disabled={deleteLoading}>
            İptal
          </Button>
          <Button 
            onClick={handleDeleteConfirm} 
            color="error" 
            variant="contained"
            autoFocus
            disabled={deleteLoading}
          >
            {deleteLoading ? <CircularProgress size={24} /> : 'Evet, Sil'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default ParkingList; 