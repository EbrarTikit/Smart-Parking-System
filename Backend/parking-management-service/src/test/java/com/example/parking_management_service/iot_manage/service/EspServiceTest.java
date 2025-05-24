package com.example.parking_management_service.iot_manage.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.parking_management_service.iot_manage.dto.EspDTO;
import com.example.parking_management_service.iot_manage.model.Esp;
import com.example.parking_management_service.iot_manage.repository.IEspRepository;
import com.example.parking_management_service.iot_manage.service.impl.EspServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EspServiceTest {

    @Mock
    private IEspRepository espRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EspServiceImpl espService;

    private EspDTO testEspDTO;
    private Esp testEsp;

    @BeforeEach
    void setUp() throws Exception {
        testEspDTO = new EspDTO("0001");
        testEspDTO.setEchoPins(Arrays.asList(36, 39, 34, 35));
        testEspDTO.setTriggerPins(Arrays.asList(23, 22, 21, 19));

        testEsp = new Esp();
        testEsp.setId("0001");
        testEsp.setEchoPinsJson("[36,39,34,35]");
        testEsp.setTriggerPinsJson("[23,22,21,19]");

        // Mock ObjectMapper behavior
        when(objectMapper.writeValueAsString(any())).thenReturn("[36,39,34,35]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(Arrays.asList(36, 39, 34, 35));
    }

    @Test
    void addEsp_ShouldSaveEsp() {
        when(espRepository.save(any(Esp.class))).thenReturn(testEsp);

        EspDTO result = espService.addEsp(testEspDTO);

        assertNotNull(result);
        assertEquals("0001", result.getId());
        assertNotNull(result.getEchoPins());
        assertNotNull(result.getTriggerPins());
        assertEquals(4, result.getEchoPins().size());
        assertEquals(4, result.getTriggerPins().size());
        verify(espRepository).save(any(Esp.class));
    }

    @Test
    void updateEsp_ShouldUpdateEsp() {
        when(espRepository.existsById("0001")).thenReturn(true);
        when(espRepository.save(any(Esp.class))).thenReturn(testEsp);

        EspDTO result = espService.updateEsp(testEspDTO);

        assertNotNull(result);
        assertEquals("0001", result.getId());
        assertNotNull(result.getEchoPins());
        assertNotNull(result.getTriggerPins());
        assertEquals(4, result.getEchoPins().size());
        assertEquals(4, result.getTriggerPins().size());
        verify(espRepository).save(any(Esp.class));
    }

    @Test
    void updateEsp_ShouldThrowException_WhenEspNotFound() {
        when(espRepository.existsById("0001")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> espService.updateEsp(testEspDTO));
    }

    @Test
    void deleteEsp_ShouldDeleteEsp() {
        doNothing().when(espRepository).deleteById("0001");

        espService.deleteEsp("0001");

        verify(espRepository).deleteById("0001");
    }

    @Test
    void getEsp_ShouldReturnEsp() {
        when(espRepository.findById("0001")).thenReturn(Optional.of(testEsp));

        EspDTO result = espService.getEsp("0001");

        assertNotNull(result);
        assertEquals("0001", result.getId());
        assertNotNull(result.getEchoPins());
        assertNotNull(result.getTriggerPins());
        assertEquals(4, result.getEchoPins().size());
        assertEquals(4, result.getTriggerPins().size());
    }

    @Test
    void getEsp_ShouldThrowException_WhenEspNotFound() {
        when(espRepository.findById("0001")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> espService.getEsp("0001"));
    }

    @Test
    void getAllEsps_ShouldReturnEspList() {
        List<Esp> esps = Arrays.asList(testEsp);
        when(espRepository.findAll()).thenReturn(esps);

        List<EspDTO> result = espService.getAllEsps();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("0001", result.get(0).getId());
        assertNotNull(result.get(0).getEchoPins());
        assertNotNull(result.get(0).getTriggerPins());
        assertEquals(4, result.get(0).getEchoPins().size());
        assertEquals(4, result.get(0).getTriggerPins().size());
    }
} 