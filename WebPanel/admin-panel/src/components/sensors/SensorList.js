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
      console.error('Error fetching sensors:', error);
      setError('An error occurred while loading sensors');
    } finally {
      setLoading(false);
    }
  };

  const handleAddSensor = async () => {
    try {
      await addSensor(newSensor);
      setSuccess('Sensor added successfully');
      setDialogOpen(false);
      fetchSensors();
      setNewSensor({
        parkingId: '',
        controllerId: '',
        echoPin: '',
        trigPin: ''
      });
    } catch (error) {
      console.error('Error adding sensor:', error);
      setError('An error occurred while adding the sensor');
    }
  };

  const handleDeleteSensor = async (id) => {
    if (window.confirm('Are you sure you want to delete this sensor?')) {
      try {
        await deleteSensor(id);
        setSuccess('Sensor deleted successfully');
        fetchSensors();
      } catch (error) {
        console.error('Error deleting sensor:', error);
        setError('An error occurred while deleting the sensor');
      }
    }
  };

  return (
    <Container maxWidth="lg">
      <PageHeader title="Sensor Management" />
      
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}
      
      <Button
        variant="contained"
        color="primary"
        onClick={() => setDialogOpen(true)}
        sx={{ mb: 2 }}
      >
        Add New Sensor
      </Button>

      {loading ? (
        <CircularProgress />
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Parking ID</TableCell>
                <TableCell>Controller ID</TableCell>
                <TableCell>Echo Pin</TableCell>
                <TableCell>Trig Pin</TableCell>
                <TableCell>Actions</TableCell>
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
                      Delete
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)}>
        <DialogTitle>Add New Sensor</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Parking ID"
            fullWidth
            value={newSensor.parkingId}
            onChange={(e) => setNewSensor({ ...newSensor, parkingId: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Controller ID"
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
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleAddSensor} variant="contained">Add</Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default SensorList;
