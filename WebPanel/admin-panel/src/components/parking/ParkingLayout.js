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
    road: { bgcolor: '#ffecb3', color: '#ff9800' },
    building: { bgcolor: '#e0e0e0', color: '#616161', icon: <ApartmentIcon /> },
  };

  useEffect(() => {
    fetchParkingDetails();
  }, [id]);

  useEffect(() => {
    const fetchSensors = async () => {
      setSensorLoading(true);
      try {
        const response = await getAllSensors();
        const allSensors = response.data;

        const parkingIdString = String(id).padStart(4, '0');

        const relevantSensors = allSensors.filter(sensor =>
          sensor.id && String(sensor.id).startsWith(parkingIdString)
        );

        setSensors(relevantSensors);
        setError('');
      } catch (error) {
        console.error('Error loading sensors:', error);
        setError('An error occurred while loading sensors');
        setSensors([]);
      } finally {
        setSensorLoading(false);
      }
    };

    if (dialogOpen && id) {
      fetchSensors();
    } else if (!dialogOpen) {
      setSensors([]);
    }
  }, [dialogOpen, id]);

  const fetchParkingDetails = async () => {
    setLoading(true);
    try {
      const response = await getParkingById(id);
      const parkingData = response.data;
      setParking(parkingData);
      
      createMatrix(parkingData);
      
      setError('');
    } catch (error) {
      console.error('Error fetching parking details:', error);
      setError('An error occurred while loading parking details');
    } finally {
      setLoading(false);
    }
  };

  const createMatrix = (parkingData) => {
    if (!parkingData || !parkingData.rows || !parkingData.columns) {
      setError('Parking row and column information not found');
      return;
    }

    const rows = parkingData.rows;
    const columns = parkingData.columns;

    const emptyMatrix = Array(rows).fill().map(() =>
      Array(columns).fill().map(() => ({ type: 'empty', data: null }))
    );

    // roadSet değişkenini burada, her iki yol işleme bloğunun dışında tanımlayalım
    const roadSet = new Set();

    // Tüm yol hücrelerini setini oluştur (Bu blok roadSet'i doldurur)
    if (parkingData.roads && parkingData.roads.length > 0) {
      parkingData.roads.forEach(road => {
        if (road.roadRow >= 0 && road.roadRow < rows && road.roadColumn >= 0 && road.roadColumn < columns) {
          emptyMatrix[road.roadRow][road.roadColumn] = {
            type: 'road',
            data: road,
            id: road.id,
            identifier: road.roadIdentifier,
            orientation: 'vertical', // Varsayılan yön
          };
          roadSet.add(`${road.roadRow},${road.roadColumn}`);
        }
      });
    }

    // Yolların yönünü belirle (Bu blok roadSet'i kullanır)
    if (parkingData.roads && parkingData.roads.length > 0) {
      parkingData.roads.forEach(road => {
        const row = road.roadRow;
        const col = road.roadColumn;
        const pos = `${row},${col}`;

        const leftPos = `${row},${col - 1}`;
        const rightPos = `${row},${col + 1}`;
        const topPos = `${row - 1},${col}`;
        const bottomPos = `${row + 1},${col}`;

        // Komşu yol hücrelerini kontrol et ve yönü belirle
        let orientation = 'vertical'; // Varsayılan
        if (roadSet.has(leftPos) || roadSet.has(rightPos)) {
          orientation = 'horizontal';
        } else if (roadSet.has(topPos) || roadSet.has(bottomPos)) {
          orientation = 'vertical';
        }

        // Matristeki hücrenin yön bilgisini güncelle
        if (emptyMatrix[row] && emptyMatrix[row][col] && emptyMatrix[row][col].type === 'road') {
          emptyMatrix[row][col].orientation = orientation;
        }

      });
    }

    // Park yerlerini ekle (mevcut mantık aynı kalabilir)
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

    // Binaları ekle (mevcut mantık aynı kalabilir)
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
      const newMatrix = [...matrix];
      newMatrix[rowIndex][colIndex] = { type: 'empty', data: null };
      setMatrix(newMatrix);
    } else {
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
    setError('');
  };

  const handleNewSensorDialogClose = () => {
    setNewSensorDialogOpen(false);
    setNewSensorData({
      parkingId: '',
      controllerId: '',
      echoPin: '',
      trigPin: ''
    });
    setError('');
  };

  const handleAddSensor = async () => {
    if (!newSensorData.controllerId || newSensorData.echoPin === '' || newSensorData.trigPin === '') {
      setError('Please fill in all new sensor fields.');
      return;
    }

    setSaving(true);
    setError('');
    try {
      const sensorDataToSend = {
        ...newSensorData,
        parkingId: String(id).padStart(4, '0')
      };
      const response = await addSensor(sensorDataToSend);
      console.log('Sensor added successfully:', response.data);
      setSuccess('New sensor added successfully!');
      
      // Yeni sensör ekleme penceresini kapat
      setNewSensorDialogOpen(false);
      
      // Sensör listesini güncelle
      const sensorsResponse = await getAllSensors();
      const allSensors = sensorsResponse.data;
      const parkingIdString = String(id).padStart(4, '0');
      const relevantSensors = allSensors.filter(sensor =>
        sensor.id && String(sensor.id).startsWith(parkingIdString)
      );
      setSensors(relevantSensors);

      // Yeni eklenen sensörü seç
      const newlyAddedSensorId = response.data.id;
      if (newlyAddedSensorId) {
        setDialogData(prevData => ({
          ...prevData,
          sensorId: newlyAddedSensorId
        }));
      }

    } catch (error) {
      console.error('Error adding sensor:', error);
      setError('An error occurred while adding the new sensor.');
    } finally {
      setSaving(false);
    }
  };

  const handleDialogSave = async () => {
    if (!selectedCell) return;
    
    const { row, col } = selectedCell;
    const newMatrix = [...matrix];
    
    if (dialogData.type === 'spot' && !dialogData.sensorId) {
      setError('Sensor selection is required for parking spot.');
      return;
    }

    try {
      switch (dialogData.type) {
        case 'spot':
          const spotData = {
            type: 'spot',
            identifier: dialogData.identifier,
            sensorId: dialogData.sensorId,
            row: row,
            column: col,
            parkingId: parseInt(id)
          };
          const updateSpotResponse = await updateSpotSensor(id, row, col, dialogData.sensorId);
          console.log('Spot updated with sensor:', updateSpotResponse.data);

          newMatrix[row][col] = {
            ...newMatrix[row][col],
            type: 'spot',
            identifier: dialogData.identifier,
            sensorId: dialogData.sensorId,
            occupied: newMatrix[row][col]?.occupied || false
          };
          setSuccess('Parking spot updated successfully!');
          
          break;
        case 'road':
          const roadData = {
             type: 'road',
             roadIdentifier: dialogData.identifier,
             roadRow: row,
             roadColumn: col,
             parkingId: parseInt(id)
          };
          const addRoadResponse = await updateParkingLayout(id, { roads: [roadData] });
          console.log('Road added/updated:', addRoadResponse.data);
          
          newMatrix[row][col] = {
             ...newMatrix[row][col],
             type: 'road',
             identifier: dialogData.identifier,
          };
          setSuccess('Road updated successfully!');
          break;
        case 'building':
           const buildingData = {
              type: 'building',
              buildingRow: row,
              buildingColumn: col,
              parkingId: parseInt(id)
           };
           const addBuildingResponse = await updateParkingLayout(id, { buildings: [buildingData] });
           console.log('Building added:', addBuildingResponse.data);

           newMatrix[row][col] = {
              ...newMatrix[row][col],
              type: 'building',
           };
           setSuccess('Building added successfully!');
           break;
        default:
          break;
      }

      setMatrix(newMatrix);
      setDialogOpen(false);
      setSelectedCell(null);
      
      setTimeout(() => {
        setSuccess('');
      }, 3000);

    } catch (error) {
      console.error('Error saving layout:', error);
      setError(`Error saving layout: ${error.response?.data?.message || error.message}`);
      
      setTimeout(() => {
        setError('');
      }, 5000);
    } finally {
      setSaving(false);
    }
  };

  const handleSaveLayout = async () => {
    const parkingSpots = [];
    const roads = [];
    const buildings = [];
    
    matrix.forEach((row, rowIndex) => {
      row.forEach((cell, colIndex) => {
        if (cell.type === 'spot') {
          parkingSpots.push({
            id: cell.id || null,
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
    
    const layoutData = {
      parkingId: id,
      parkingSpots,
      roads,
      buildings
    };
    
    setSaving(true);
    
    try {
      await clearParkingLayout(id);
      
      await updateParkingLayout(id, layoutData);
      
      setSuccess('Parking layout updated successfully');
      setEditMode(false);
      
      await fetchParkingDetails();
      
    } catch (error) {
      console.error('Layout update error:', error);
      setError('An error occurred while updating the layout: ' + (error.response?.data?.message || error.message));
    } finally {
      setSaving(false);
    }
  };

  const handleClearLayout = async () => {
    if (!window.confirm('Are you sure you want to completely clear the parking layout? This action cannot be undone.')) {
      return;
    }
    
    setSaving(true);
    
    try {
      await clearParkingLayout(id);
      setSuccess('Parking layout cleared successfully');
      
      setMatrix(Array(parking.rows).fill().map(() => 
        Array(parking.columns).fill().map(() => ({ type: 'empty', data: null }))
      ));
      
      setEditMode(false);
    } catch (error) {
      console.error('Layout clearing error:', error);
      setError('An error occurred while clearing the layout: ' + (error.response?.data?.message || error.message));
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
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100%',
            width: '100%',
            bgcolor: cellStyles.road.bgcolor,
            position: 'relative',
          }}>
            <Typography
              variant="caption"
              sx={{
                position: 'relative',
                zIndex: 1,
                bgcolor: cellStyles.road.bgcolor,
                px: 0.5,
                color: cellStyles.road.color,
                fontWeight: 'bold'
              }}
            >
              {cell.identifier || 'Road'}
            </Typography>
          </Box>
        );
      case 'building':
        return <ApartmentIcon sx={{ color: cellStyles.building.color }} />;
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
        <Alert severity="error">Parking not found</Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <PageHeader 
        title={`${parking.name} Parking Layout`} 
        breadcrumbs={[
          { text: 'Parkings', link: '/parkings' },
          { text: parking.name, link: `/parking-details/${id}` },
          { text: 'Parking Layout' }
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
            Layout Size: {parking.rows} x {parking.columns}
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
                Clear Layout
              </Button>
            )}
            <Button 
              variant={editMode ? "outlined" : "contained"} 
              color={editMode ? "warning" : "primary"}
              startIcon={editMode ? <DoDisturbIcon /> : <EditIcon />}
              onClick={handleToggleEditMode}
            >
              {editMode ? 'Cancel Editing' : 'Edit Layout'}
            </Button>
          </Box>
        </Box>
        
        {editMode && (
          <Box sx={{ mb: 3, p: 2, bgcolor: '#f8f9fa', borderRadius: 1 }}>
            <Typography variant="subtitle1" gutterBottom>
              Editing Tools
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
                  <Tooltip title="Add Parking Spot">
                    <DirectionsCarIcon />
                  </Tooltip>
                </ToggleButton>
                <ToggleButton value="road" aria-label="road">
                  <Tooltip title="Add Road">
                    <StraightIcon />
                  </Tooltip>
                </ToggleButton>
                <ToggleButton value="building" aria-label="building">
                  <Tooltip title="Add Building">
                    <ApartmentIcon />
                  </Tooltip>
                </ToggleButton>
                <ToggleButton value="delete" aria-label="delete">
                  <Tooltip title="Delete">
                    <DeleteIcon />
                  </Tooltip>
                </ToggleButton>
              </ToggleButtonGroup>
              
              <Typography variant="body2" color="text.secondary">
                Select the item you want to add and click on a cell in the matrix
              </Typography>
            </Box>
          </Box>
        )}
        
        <Box sx={{ overflowX: 'auto', maxWidth: '100%' }}>
          <Grid container spacing={0.5} sx={{ display: 'inline-flex', flexDirection: 'column' }}>
            {matrix.map((row, rowIndex) => (
              <Grid container item key={rowIndex} spacing={0.5} sx={{ flexWrap: 'nowrap', display: 'inline-flex' }}>
                {row.map((cell, colIndex) => (
                  <Grid
                    item
                    key={colIndex}
                    sx={{
                      flexShrink: 0,
                      flexGrow: 0,
                      flexBasis: '120px',
                      width: '120px',
                      height: '120px',
                    }}
                  >
                    <Paper
                      onClick={() => handleCellClick(rowIndex, colIndex)}
                      sx={{
                        width: '100%',
                        height: '100%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        cursor: editMode ? 'pointer' : 'default',
                        transition: 'background-color 0.2s ease',
                        '&:hover': {
                          bgcolor: editMode ? 'rgba(0, 0, 0, 0.1)' : undefined,
                        },
                        bgcolor: cell.type !== 'road' ? cellStyles[cell.type].bgcolor : undefined,
                      }}
                      elevation={editMode ? 1 : 0}
                      square
                    >
                      {renderCellContent(cell)}
                    </Paper>
                  </Grid>
                ))}
              </Grid>
            ))}
          </Grid>
        </Box>
        
        <Box sx={{ mt: 3, display: 'flex', flexWrap: 'wrap', gap: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Box sx={{ 
              width: 20, 
              height: 20, 
              bgcolor: cellStyles.spot.bgcolor, 
              mr: 1, 
              borderRadius: '4px' 
            }} />
            <Typography variant="body2">Parking Spot</Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Box sx={{ 
              width: 20, 
              height: 20, 
              bgcolor: cellStyles.road.bgcolor, 
              mr: 1, 
              borderRadius: '4px' 
            }} />
            <Typography variant="body2">Road</Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Box sx={{ 
              width: 20, 
              height: 20, 
              bgcolor: cellStyles.building.bgcolor, 
              mr: 1, 
              borderRadius: '4px' 
            }} />
            <Typography variant="body2">Building</Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Box sx={{ 
              width: 20, 
              height: 20, 
              bgcolor: cellStyles.empty.bgcolor, 
              mr: 1, 
              borderRadius: '4px' 
            }} />
            <Typography variant="body2">Empty</Typography>
          </Box>
        </Box>
      </Paper>
      
      <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <Button 
          variant="outlined" 
          startIcon={<ArrowBackIcon />} 
          onClick={handleBack}
        >
          Go Back
        </Button>
        
        {editMode && (
          <Button 
            variant="contained" 
            color="primary" 
            startIcon={<SaveIcon />} 
            onClick={handleSaveLayout}
            disabled={saving}
          >
            {saving ? <CircularProgress size={24} /> : 'Save Layout'}
          </Button>
        )}
      </Box>
      
      <Dialog open={dialogOpen} onClose={handleDialogClose}>
        <DialogTitle>{selectedTool === 'spot' ? 'Add New Parking Spot' : selectedTool === 'road' ? 'Add/Edit Road' : 'Add Building'}</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          {dialogData.type === 'spot' && (
            <>
              <TextField
                autoFocus
                margin="dense"
                label="Identifier"
                fullWidth
                value={dialogData.identifier}
                onChange={(e) => setDialogData({ ...dialogData, identifier: e.target.value })}
              />
              <FormControl fullWidth margin="dense">
                <Autocomplete
                  options={sensors}
                  getOptionLabel={(option) => option.id ? String(option.id) : ''}
                  value={sensors.find(s => s.id === dialogData.sensorId) || null}
                  onChange={(event, newValue) => {
                    setDialogData({ ...dialogData, sensorId: newValue ? newValue.id : '' });
                    setError('');
                  }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Sensor *"
                      error={!!error && error.includes('Sensor selection is required')}
                      helperText={error && error.includes('Sensor selection is required') ? error : ''}
                    />
                  )}
                  disabled={sensorLoading}
                />
              </FormControl>
              <Box sx={{ mt: 1, textAlign: 'right' }}>
                <Button 
                  variant="outlined" 
                  size="small" 
                  onClick={() => { 
                    setNewSensorDialogOpen(true); 
                  }}
                >
                  Add New Sensor
                </Button>
              </Box>
            </>
          )}
          {dialogData.type === 'road' && (
            <TextField
              autoFocus
              margin="dense"
              label="Identifier"
              fullWidth
              value={dialogData.identifier}
              onChange={(e) => setDialogData({ ...dialogData, identifier: e.target.value })}
            />
          )}
          {dialogData.type === 'building' && (
            <Typography variant="body1">
              Click Save to add a building at this location.
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDialogClose}>Cancel</Button>
          <Button onClick={handleDialogSave} variant="contained" disabled={saving}>
            {saving ? <CircularProgress size={24} /> : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={newSensorDialogOpen} onClose={handleNewSensorDialogClose}>
        <DialogTitle>Add New Sensor for Parking {id}</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <TextField
            autoFocus
            margin="dense"
            label="Controller ID"
            fullWidth
            value={newSensorData.controllerId}
            onChange={(e) => setNewSensorData({ ...newSensorData, controllerId: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Echo Pin"
            type="number"
            fullWidth
            value={newSensorData.echoPin}
            onChange={(e) => setNewSensorData({ ...newSensorData, echoPin: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Trig Pin"
            type="number"
            fullWidth
            value={newSensorData.trigPin}
            onChange={(e) => setNewSensorData({ ...newSensorData, trigPin: e.target.value })}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleNewSensorDialogClose} disabled={saving}>Cancel</Button>
          <Button onClick={handleAddSensor} variant="contained" disabled={saving}>
            {saving ? <CircularProgress size={24} /> : 'Add Sensor'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default ParkingLayout; 