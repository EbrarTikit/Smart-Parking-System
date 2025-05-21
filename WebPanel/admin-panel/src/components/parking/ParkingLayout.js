import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Container, Paper, Typography, Box, Grid, Button, CircularProgress, 
  Alert, Divider, ToggleButtonGroup, ToggleButton, FormControl, 
  InputLabel, Select, MenuItem, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Tooltip
} from '@mui/material';
import { getParkingById, updateParkingLayout, clearParkingLayout, getAllSensors, addSensor, updateSpotSensor } from '../../services/apiService';
import PageHeader from '../common/PageHeader';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import SaveIcon from '@mui/icons-material/Save';
import EditIcon from '@mui/icons-material/Edit';
import DirectionsCarIcon from '@mui/icons-material/DirectionsCar';
import StraightIcon from '@mui/icons-material/Straight';
import ApartmentIcon from '@mui/icons-material/Apartment';
import DeleteIcon from '@mui/icons-material/Delete';
import DoDisturbIcon from '@mui/icons-material/DoDisturb';
import Autocomplete from '@mui/material/Autocomplete';

const ParkingLayout = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [parking, setParking] = useState(null);
  const [matrix, setMatrix] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editMode, setEditMode] = useState(false);
  const [selectedTool, setSelectedTool] = useState('spot');
  const [selectedCell, setSelectedCell] = useState(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [newSensorDialogOpen, setNewSensorDialogOpen] = useState(false);
  const [dialogData, setDialogData] = useState({
    type: 'spot',
    identifier: '',
    sensorId: ''
  });
  const [sensors, setSensors] = useState([]);
  const [sensorLoading, setSensorLoading] = useState(false);
  const [showSensorDialog, setShowSensorDialog] = useState(false);
  const [newSensorData, setNewSensorData] = useState({
    parkingId: '',
    controllerId: '',
    echoPin: '',
    trigPin: ''
  });

  // Hücre tipleri için renk ve simgeler
  const cellStyles = {
    empty: { bgcolor: '#f5f5f5', color: '#757575' },
    spot: { bgcolor: '#bbdefb', color: '#1976d2', icon: <DirectionsCarIcon /> },
    road: { bgcolor: '#ffecb3', color: '#ff9800', icon: <StraightIcon /> },
    building: { bgcolor: '#e0e0e0', color: '#616161', icon: <ApartmentIcon /> }
  };

  useEffect(() => {
    fetchParkingDetails();
  }, [id]);

  useEffect(() => {
    const fetchSensors = async () => {
      try {
        const response = await getAllSensors();
        setSensors(response.data);
      } catch (error) {
        console.error('Sensörler yüklenirken hata:', error);
        setError('Sensörler yüklenirken bir hata oluştu');
      }
    };

    if (dialogOpen) {
      fetchSensors();
    }
  }, [dialogOpen]);

  const fetchParkingDetails = async () => {
    setLoading(true);
    try {
      const response = await getParkingById(id);
      const parkingData = response.data;
      setParking(parkingData);
      
      // Matrisi oluştur
      createMatrix(parkingData);
      
      setError('');
    } catch (error) {
      console.error('Otopark detayları alınırken hata oluştu:', error);
      setError('Otopark detayları yüklenirken bir hata oluştu');
    } finally {
      setLoading(false);
    }
  };

  const createMatrix = (parkingData) => {
    if (!parkingData || !parkingData.rows || !parkingData.columns) {
      setError('Otopark satır ve sütun bilgileri bulunamadı');
      return;
    }

    const rows = parkingData.rows;
    const columns = parkingData.columns;
    
    // Boş matrisi oluştur
    const emptyMatrix = Array(rows).fill().map(() => 
      Array(columns).fill().map(() => ({ type: 'empty', data: null }))
    );
    
    // Park yerlerini ekle
    if (parkingData.parkingSpots && parkingData.parkingSpots.length > 0) {
      parkingData.parkingSpots.forEach(spot => {
        if (spot.row >= 0 && spot.row < rows && spot.column >= 0 && spot.column < columns) {
          emptyMatrix[spot.row][spot.column] = {
            type: 'spot',
            data: spot,
            id: spot.id,
            identifier: spot.spotIdentifier,
            sensorId: spot.sensorId,
            occupied: spot.occupied
          };
        }
      });
    }
    
    // Yolları ekle
    if (parkingData.roads && parkingData.roads.length > 0) {
      parkingData.roads.forEach(road => {
        if (road.roadRow >= 0 && road.roadRow < rows && road.roadColumn >= 0 && road.roadColumn < columns) {
          emptyMatrix[road.roadRow][road.roadColumn] = {
            type: 'road',
            data: road,
            id: road.id,
            identifier: road.roadIdentifier
          };
        }
      });
    }
    
    // Binaları ekle
    if (parkingData.buildings && parkingData.buildings.length > 0) {
      parkingData.buildings.forEach(building => {
        if (building.buildingRow >= 0 && building.buildingRow < rows && building.buildingColumn >= 0 && building.buildingColumn < columns) {
          emptyMatrix[building.buildingRow][building.buildingColumn] = {
            type: 'building',
            data: building,
            id: building.id
          };
        }
      });
    }
    
    setMatrix(emptyMatrix);
  };

  const handleToolChange = (event, newTool) => {
    if (newTool !== null) {
      setSelectedTool(newTool);
    }
  };

  const handleCellClick = (rowIndex, colIndex) => {
    if (!editMode) return;
    
    const cell = matrix[rowIndex][colIndex];
    
    if (selectedTool === 'delete') {
      // Hücreyi temizle
      const newMatrix = [...matrix];
      newMatrix[rowIndex][colIndex] = { type: 'empty', data: null };
      setMatrix(newMatrix);
    } else {
      // Seçilen hücreyi kaydet ve dialog'u aç
      setSelectedCell({ row: rowIndex, col: colIndex, current: cell });
      setDialogData({ 
        identifier: cell.type === selectedTool ? (cell.identifier || '') : '',
        sensorId: cell.type === 'spot' && cell.type === selectedTool ? (cell.sensorId || '') : '',
        type: selectedTool 
      });
      setDialogOpen(true);
    }
  };

  const handleDialogClose = () => {
    setDialogOpen(false);
    setSelectedCell(null);
  };

  const handleDialogSave = async () => {
    if (!selectedCell) return;
    
    const { row, col } = selectedCell;
    const newMatrix = [...matrix];
    
    try {
      // Seçilen araca göre hücreyi güncelle
      switch (dialogData.type) {
        case 'spot':
          const spotData = {
            type: 'spot',
            identifier: dialogData.identifier,
            sensorId: dialogData.sensorId,
            id: selectedCell.current.type === 'spot' ? selectedCell.current.id : null,
            occupied: false
          };
          
          // Eğer mevcut bir spot ise ve sensör ID'si değiştiyse, veritabanını güncelle
          if (selectedCell.current.type === 'spot' && 
              selectedCell.current.id && 
              selectedCell.current.sensorId !== dialogData.sensorId) {
            try {
              const response = await updateSpotSensor(id, row, col, dialogData.sensorId);
              if (response.data) {
                setSuccess('Sensör başarıyla güncellendi');
                // Güncellenmiş spot verilerini kullan
                spotData.id = response.data.id;
                spotData.sensorId = response.data.sensorId;
              }
            } catch (error) {
              console.error('Sensör güncelleme hatası:', error);
              const errorMessage = error.response?.data?.message || error.message;
              setError(`Sensör güncellenirken bir hata oluştu: ${errorMessage}`);
              return; // Hata durumunda işlemi durdur
            }
          }
          
          newMatrix[row][col] = spotData;
          break;
          
        case 'road':
          newMatrix[row][col] = {
            type: 'road',
            identifier: dialogData.identifier
          };
          break;
          
        case 'building':
          newMatrix[row][col] = {
            type: 'building'
          };
          break;
      }
      
      setMatrix(newMatrix);
      setDialogOpen(false);
      setSelectedCell(null);
    } catch (error) {
      console.error('Dialog kaydetme hatası:', error);
      setError('İşlem sırasında bir hata oluştu: ' + error.message);
    }
  };

  const handleSaveLayout = async () => {
    // Matristen veri toplama
    const parkingSpots = [];
    const roads = [];
    const buildings = [];
    
    matrix.forEach((row, rowIndex) => {
      row.forEach((cell, colIndex) => {
        if (cell.type === 'spot') {
          parkingSpots.push({
            id: cell.id || null, // Mevcut ID'yi koru
            row: rowIndex,
            column: colIndex,
            spotIdentifier: cell.identifier || `R${rowIndex}C${colIndex}`,
            sensorId: cell.sensorId || null,
            occupied: cell.occupied || false
          });
        } else if (cell.type === 'road') {
          roads.push({
            id: cell.id || null,
            roadRow: rowIndex,
            roadColumn: colIndex,
            roadIdentifier: cell.identifier || 'road'
          });
        } else if (cell.type === 'building') {
          buildings.push({
            id: cell.id || null,
            buildingRow: rowIndex,
            buildingColumn: colIndex
          });
        }
      });
    });
    
    // Layout verisi
    const layoutData = {
      parkingId: id,
      parkingSpots,
      roads,
      buildings
    };
    
    setSaving(true);
    
    try {
      // Önce mevcut layout'u temizliyoruz
      await clearParkingLayout(id);
      
      // Sonra yeni layout'u ekliyoruz
      await updateParkingLayout(id, layoutData);
      
      setSuccess('Otopark düzeni başarıyla güncellendi');
      setEditMode(false);
      
      // Yeni verileri getir
      await fetchParkingDetails();
      
    } catch (error) {
      console.error('Layout güncelleme hatası:', error);
      setError('Layout güncellenirken bir hata oluştu: ' + (error.response?.data?.message || error.message));
    } finally {
      setSaving(false);
    }
  };

  const handleClearLayout = async () => {
    if (!window.confirm('Otopark düzenini tamamen temizlemek istediğinizden emin misiniz? Bu işlem geri alınamaz.')) {
      return;
    }
    
    setSaving(true);
    
    try {
      await clearParkingLayout(id);
      setSuccess('Otopark düzeni başarıyla temizlendi');
      
      // Boş bir matris oluştur
      setMatrix(Array(parking.rows).fill().map(() => 
        Array(parking.columns).fill().map(() => ({ type: 'empty', data: null }))
      ));
      
      setEditMode(false); // Düzenleme modundan çık
    } catch (error) {
      console.error('Düzen temizleme hatası:', error);
      setError('Düzen temizlenirken bir hata oluştu: ' + (error.response?.data?.message || error.message));
    } finally {
      setSaving(false);
    }
  };

  const handleToggleEditMode = () => {
    setEditMode(!editMode);
  };

  const handleBack = () => {
    navigate(`/parking-details/${id}`);
  };
  
  // Hücre içeriğini render et
  const renderCellContent = (cell) => {
    switch (cell.type) {
      case 'spot':
        return (
          <Box sx={{ 
            textAlign: 'center', 
            display: 'flex', 
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100%'
          }}>
            <DirectionsCarIcon sx={{ mb: 0.5, color: cell.occupied ? '#f44336' : '#1976d2' }} />
            <Typography variant="caption" sx={{ fontWeight: 'bold' }}>
              {cell.identifier || '?'}
            </Typography>
            {cell.sensorId && (
              <Typography variant="caption" sx={{ fontSize: '0.65rem', color: 'text.secondary' }}>
                SID: {cell.sensorId}
              </Typography>
            )}
          </Box>
        );
      case 'road':
        return (
          <Box sx={{ 
            textAlign: 'center', 
            display: 'flex', 
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100%'
          }}>
            <StraightIcon sx={{ mb: 0.5 }} />
            <Typography variant="caption">
              {cell.identifier || 'Yol'}
            </Typography>
          </Box>
        );
      case 'building':
        return <ApartmentIcon sx={{ color: '#616161' }} />;
      default:
        return null;
    }
  };

  if (loading) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4, display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
        <CircularProgress />
      </Container>
    );
  }

  if (!parking) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <Alert severity="error">Otopark bulunamadı</Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <PageHeader 
        title={`${parking.name} Otopark Düzeni`} 
        breadcrumbs={[
          { text: 'Otoparklar', link: '/parkings' },
          { text: parking.name, link: `/parking-details/${id}` },
          { text: 'Otopark Düzeni' }
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
      
      <Paper sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">
            Düzen Boyutu: {parking.rows} x {parking.columns}
          </Typography>
          <Box>
            {!editMode && (
              <Button 
                variant="outlined" 
                color="error"
                onClick={handleClearLayout}
                disabled={saving}
                startIcon={<DeleteIcon />}
                sx={{ mr: 2 }}
              >
                Düzeni Temizle
              </Button>
            )}
            <Button 
              variant={editMode ? "outlined" : "contained"} 
              color={editMode ? "warning" : "primary"}
              startIcon={editMode ? <DoDisturbIcon /> : <EditIcon />}
              onClick={handleToggleEditMode}
            >
              {editMode ? 'Düzenlemeyi İptal Et' : 'Düzeni Düzenle'}
            </Button>
          </Box>
        </Box>
        
        {editMode && (
          <Box sx={{ mb: 3, p: 2, bgcolor: '#f8f9fa', borderRadius: 1 }}>
            <Typography variant="subtitle1" gutterBottom>
              Düzenleme Araçları
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <ToggleButtonGroup
                value={selectedTool}
                exclusive
                onChange={handleToolChange}
                aria-label="layout tools"
                size="small"
                sx={{ mr: 2 }}
              >
                <ToggleButton value="spot" aria-label="parking spot">
                  <Tooltip title="Park Yeri Ekle">
                    <DirectionsCarIcon />
                  </Tooltip>
                </ToggleButton>
                <ToggleButton value="road" aria-label="road">
                  <Tooltip title="Yol Ekle">
                    <StraightIcon />
                  </Tooltip>
                </ToggleButton>
                <ToggleButton value="building" aria-label="building">
                  <Tooltip title="Bina Ekle">
                    <ApartmentIcon />
                  </Tooltip>
                </ToggleButton>
                <ToggleButton value="delete" aria-label="delete">
                  <Tooltip title="Sil">
                    <DeleteIcon />
                  </Tooltip>
                </ToggleButton>
              </ToggleButtonGroup>
              
              <Typography variant="body2" color="text.secondary">
                Eklemek istediğiniz öğeyi seçin ve matristeki bir hücreye tıklayın
              </Typography>
            </Box>
          </Box>
        )}
        
        {/* Otopark Matrisi */}
        <Box sx={{ overflowX: 'auto' }}>
          <Box sx={{ 
            display: 'inline-block', 
            border: '1px solid #ddd', 
            borderRadius: 1,
            p: 1,
            bgcolor: '#fff'
          }}>
            {matrix.map((row, rowIndex) => (
              <Box 
                key={`row-${rowIndex}`} 
                sx={{ 
                  display: 'flex', 
                  borderBottom: rowIndex < matrix.length - 1 ? '1px solid #eee' : 'none' 
                }}
              >
                {row.map((cell, colIndex) => (
                  <Box 
                    key={`cell-${rowIndex}-${colIndex}`}
                    onClick={() => handleCellClick(rowIndex, colIndex)}
                    sx={{
                      width: 80,
                      height: 80,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      borderRight: colIndex < row.length - 1 ? '1px solid #eee' : 'none',
                      bgcolor: cellStyles[cell.type].bgcolor,
                      cursor: editMode ? 'pointer' : 'default',
                      transition: 'all 0.2s',
                      '&:hover': {
                        bgcolor: editMode ? cellStyles[cell.type === 'empty' ? 'empty' : cell.type].bgcolor + '80' : 'inherit',
                        boxShadow: editMode ? 'inset 0 0 0 2px #1976d2' : 'none'
                      }
                    }}
                  >
                    {renderCellContent(cell)}
                  </Box>
                ))}
              </Box>
            ))}
          </Box>
        </Box>
        
        {/* Renk açıklamaları */}
        <Box sx={{ mt: 3, display: 'flex', flexWrap: 'wrap', gap: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Box sx={{ 
              width: 20, 
              height: 20, 
              bgcolor: cellStyles.spot.bgcolor, 
              mr: 1, 
              borderRadius: '4px' 
            }} />
            <Typography variant="body2">Park Yeri</Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Box sx={{ 
              width: 20, 
              height: 20, 
              bgcolor: cellStyles.road.bgcolor, 
              mr: 1, 
              borderRadius: '4px' 
            }} />
            <Typography variant="body2">Yol</Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Box sx={{ 
              width: 20, 
              height: 20, 
              bgcolor: cellStyles.building.bgcolor, 
              mr: 1, 
              borderRadius: '4px' 
            }} />
            <Typography variant="body2">Bina</Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Box sx={{ 
              width: 20, 
              height: 20, 
              bgcolor: cellStyles.empty.bgcolor, 
              mr: 1, 
              borderRadius: '4px' 
            }} />
            <Typography variant="body2">Boş</Typography>
          </Box>
        </Box>
      </Paper>
      
      <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <Button 
          variant="outlined" 
          startIcon={<ArrowBackIcon />} 
          onClick={handleBack}
        >
          Geri Dön
        </Button>
        
        {editMode && (
          <Button 
            variant="contained" 
            color="primary" 
            startIcon={<SaveIcon />} 
            onClick={handleSaveLayout}
            disabled={saving}
          >
            {saving ? <CircularProgress size={24} /> : 'Düzeni Kaydet'}
          </Button>
        )}
      </Box>
      
      {/* Dialog - Park Yeri/Yol Ekleme */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)}>
        <DialogTitle>
          {selectedCell?.current?.type === 'spot' ? 'Park Yeri Düzenle' : 'Yeni Park Yeri Ekle'}
        </DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Tanımlayıcı"
            fullWidth
            value={dialogData.identifier}
            onChange={(e) => setDialogData({ ...dialogData, identifier: e.target.value })}
          />
          {dialogData.type === 'spot' && (
            <>
              <Autocomplete
                options={sensors}
                getOptionLabel={(option) => `${option.id} (${option.parkingId})`}
                value={sensors.find(s => s.id === dialogData.sensorId) || null}
                onChange={(event, newValue) => {
                  setDialogData({ ...dialogData, sensorId: newValue?.id || '' });
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    margin="dense"
                    label="Sensör"
                    fullWidth
                  />
                )}
              />
              <Button
                color="primary"
                onClick={() => setNewSensorDialogOpen(true)}
                style={{ marginTop: '8px' }}
              >
                Yeni Sensör Ekle
              </Button>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>İptal</Button>
          <Button onClick={handleDialogSave} color="primary">
            Kaydet
          </Button>
        </DialogActions>
      </Dialog>

      {/* Yeni Sensör Ekleme Dialog'u */}
      <Dialog open={newSensorDialogOpen} onClose={() => setNewSensorDialogOpen(false)}>
        <DialogTitle>Yeni Sensör Ekle</DialogTitle>
        <DialogContent>
          <TextField
            margin="dense"
            label="Parking ID"
            fullWidth
            value={newSensorData.parkingId}
            onChange={(e) => setNewSensorData({ ...newSensorData, parkingId: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Controller ID"
            fullWidth
            value={newSensorData.controllerId}
            onChange={(e) => setNewSensorData({ ...newSensorData, controllerId: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Echo Pin"
            fullWidth
            type="number"
            value={newSensorData.echoPin}
            onChange={(e) => setNewSensorData({ ...newSensorData, echoPin: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Trig Pin"
            fullWidth
            type="number"
            value={newSensorData.trigPin}
            onChange={(e) => setNewSensorData({ ...newSensorData, trigPin: e.target.value })}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setNewSensorDialogOpen(false)}>İptal</Button>
          <Button 
            onClick={async () => {
              try {
                const response = await addSensor(newSensorData);
                setSensors([...sensors, response.data]);
                setDialogData({ ...dialogData, sensorId: response.data.id });
                setNewSensorDialogOpen(false);
                setSuccess('Yeni sensör başarıyla eklendi');
              } catch (error) {
                setError('Sensör eklenirken bir hata oluştu: ' + error.message);
              }
            }} 
            color="primary"
          >
            Ekle
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default ParkingLayout; 