import React, { useState, useEffect } from 'react';
import {
  Container,
  Paper,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
  CircularProgress
} from '@mui/material';
import { getAllSensors, addSensor, deleteSensor } from '../../services/apiService';
import PageHeader from '../common/PageHeader';

const SensorList = () => {
  const [sensors, setSensors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [newSensor, setNewSensor] = useState({
    parkingId: '',
    controllerId: '',
    echoPin: '',
    trigPin: ''
  });

  useEffect(() => {
    fetchSensors();
  }, []);

  const fetchSensors = async () => {
    try {
      const response = await getAllSensors();
      setSensors(response.data);
      setError('');
    } catch (error) {
      console.error('Sensörler alınırken hata oluştu:', error);
      setError('Sensörler yüklenirken bir hata oluştu');
    } finally {
      setLoading(false);
    }
  };

  const handleAddSensor = async () => {
    try {
      await addSensor(newSensor);
      setSuccess('Sensör başarıyla eklendi');
      setDialogOpen(false);
      fetchSensors();
      // Formu temizle
      setNewSensor({
        parkingId: '',
        controllerId: '',
        echoPin: '',
        trigPin: ''
      });
    } catch (error) {
      console.error('Sensör eklenirken hata oluştu:', error);
      setError('Sensör eklenirken bir hata oluştu');
    }
  };

  const handleDeleteSensor = async (id) => {
    if (window.confirm('Bu sensörü silmek istediğinizden emin misiniz?')) {
      try {
        await deleteSensor(id);
        setSuccess('Sensör başarıyla silindi');
        fetchSensors();
      } catch (error) {
        console.error('Sensör silinirken hata oluştu:', error);
        setError('Sensör silinirken bir hata oluştu');
      }
    }
  };

  return (
    <Container maxWidth="lg">
      <PageHeader title="Sensör Yönetimi" />
      
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}
      
      <Button
        variant="contained"
        color="primary"
        onClick={() => setDialogOpen(true)}
        sx={{ mb: 2 }}
      >
        Yeni Sensör Ekle
      </Button>

      {loading ? (
        <CircularProgress />
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Otopark ID</TableCell>
                <TableCell>Kontrolcü ID</TableCell>
                <TableCell>Echo Pin</TableCell>
                <TableCell>Trig Pin</TableCell>
                <TableCell>İşlemler</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {sensors.map((sensor) => (
                <TableRow key={sensor.id}>
                  <TableCell>{sensor.id}</TableCell>
                  <TableCell>{sensor.parkingId}</TableCell>
                  <TableCell>{sensor.controllerId}</TableCell>
                  <TableCell>{sensor.echoPin}</TableCell>
                  <TableCell>{sensor.trigPin}</TableCell>
                  <TableCell>
                    <Button
                      variant="outlined"
                      color="error"
                      onClick={() => handleDeleteSensor(sensor.id)}
                    >
                      Sil
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)}>
        <DialogTitle>Yeni Sensör Ekle</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Otopark ID"
            fullWidth
            value={newSensor.parkingId}
            onChange={(e) => setNewSensor({ ...newSensor, parkingId: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Kontrolcü ID"
            fullWidth
            value={newSensor.controllerId}
            onChange={(e) => setNewSensor({ ...newSensor, controllerId: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Echo Pin"
            type="number"
            fullWidth
            value={newSensor.echoPin}
            onChange={(e) => setNewSensor({ ...newSensor, echoPin: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Trig Pin"
            type="number"
            fullWidth
            value={newSensor.trigPin}
            onChange={(e) => setNewSensor({ ...newSensor, trigPin: e.target.value })}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>İptal</Button>
          <Button onClick={handleAddSensor} variant="contained">Ekle</Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default SensorList;
