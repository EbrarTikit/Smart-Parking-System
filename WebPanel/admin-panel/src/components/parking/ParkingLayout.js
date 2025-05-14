import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Container, Paper, Typography, Box, Grid, Button, CircularProgress, 
  Alert, Divider, ToggleButtonGroup, ToggleButton, FormControl, 
  InputLabel, Select, MenuItem, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Tooltip
} from '@mui/material';
import { getParkingById, updateParkingLayout, clearParkingLayout } from '../../services/apiService';
import PageHeader from '../common/PageHeader';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import SaveIcon from '@mui/icons-material/Save';
import EditIcon from '@mui/icons-material/Edit';
import DirectionsCarIcon from '@mui/icons-material/DirectionsCar';
import StraightIcon from '@mui/icons-material/Straight';
import ApartmentIcon from '@mui/icons-material/Apartment';
import DeleteIcon from '@mui/icons-material/Delete';
import DoDisturbIcon from '@mui/icons-material/DoDisturb';

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
  const [dialogData, setDialogData] = useState({ 
    identifier: '', 
    sensorId: '',
    type: 'spot' 
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

  const handleDialogSave = () => {
    if (!selectedCell) return;
    
    const { row, col } = selectedCell;
    const newMatrix = [...matrix];
    
    // Seçilen araca göre hücreyi güncelle
    switch (dialogData.type) {
      case 'spot':
        newMatrix[row][col] = {
          type: 'spot',
          identifier: dialogData.identifier,
          sensorId: dialogData.sensorId,
          id: selectedCell.current.type === 'spot' ? selectedCell.current.id : null,
          occupied: false
        };
        break;
      case 'road':
        newMatrix[row][col] = {
          type: 'road',
          identifier: dialogData.identifier,
          id: selectedCell.current.type === 'road' ? selectedCell.current.id : null
        };
        break;
      case 'building':
        newMatrix[row][col] = {
          type: 'building',
          id: selectedCell.current.type === 'building' ? selectedCell.current.id : null
        };
        break;
      default:
        break;
    }
    
    setMatrix(newMatrix);
    setDialogOpen(false);
    setSelectedCell(null);
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
            id: null, // ID'leri null olarak ayarlıyoruz, yeni kayıt oluşması için
            row: rowIndex,
            column: colIndex,
            spotIdentifier: cell.identifier || `R${rowIndex}C${colIndex}`,
            sensorId: cell.sensorId || null, // Sensor ID'yi ekliyoruz
            occupied: cell.occupied || false
          });
        } else if (cell.type === 'road') {
          roads.push({
            id: null, // ID'leri null olarak ayarlıyoruz, yeni kayıt oluşması için
            roadRow: rowIndex,
            roadColumn: colIndex,
            roadIdentifier: cell.identifier || 'road'
          });
        } else if (cell.type === 'building') {
          buildings.push({
            id: null, // ID'leri null olarak ayarlıyoruz, yeni kayıt oluşması için
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
      <Dialog open={dialogOpen} onClose={handleDialogClose}>
        <DialogTitle>
          {dialogData.type === 'spot' ? 'Park Yeri' : 
           dialogData.type === 'road' ? 'Yol' : 'Bina'} Ekle/Düzenle
        </DialogTitle>
        <DialogContent>
          {dialogData.type !== 'building' && (
            <TextField
              autoFocus
              margin="dense"
              label={dialogData.type === 'spot' ? 'Park Yeri Tanımlayıcısı (A1, B2, vb.)' : 'Yol Tanımlayıcısı'}
              fullWidth
              variant="outlined"
              value={dialogData.identifier}
              onChange={(e) => setDialogData({ ...dialogData, identifier: e.target.value })}
              sx={{ mt: 2 }}
            />
          )}
          
          {/* Park yerleri için Sensor ID alanı */}
          {dialogData.type === 'spot' && (
            <TextField
              margin="dense"
              label="Sensör ID"
              fullWidth
              variant="outlined"
              value={dialogData.sensorId || ''}
              onChange={(e) => setDialogData({ ...dialogData, sensorId: e.target.value })}
              sx={{ mt: 2 }}
              helperText="Eğer bu park yerine bir sensör atandıysa, burada belirtin."
            />
          )}
          
          {dialogData.type === 'building' && (
            <Typography variant="body2" sx={{ mt: 2 }}>
              Bina eklemek için onaylayın. Binalar için özel bir tanımlayıcı gerekmez.
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDialogClose}>İptal</Button>
          <Button onClick={handleDialogSave} variant="contained">Ekle</Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default ParkingLayout; 