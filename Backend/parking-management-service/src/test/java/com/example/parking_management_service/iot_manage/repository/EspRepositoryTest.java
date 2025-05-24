package com.example.parking_management_service.iot_manage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.parking_management_service.iot_manage.model.Esp;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@DataJpaTest
@ActiveProfiles("test")
class EspRepositoryTest {

    @Autowired
    private IEspRepository espRepository;

    // ObjectMapper'ı test sınıfında oluştur
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void saveEsp_ShouldSaveEsp() throws JsonProcessingException {
        // Given
        Esp esp = new Esp();
        esp.setId("ESP-001");
        esp.setEchoPinsJson(objectMapper.writeValueAsString(Arrays.asList(36, 39, 34, 35)));
        esp.setTriggerPinsJson(objectMapper.writeValueAsString(Arrays.asList(23, 22, 21, 19)));

        // When
        Esp savedEsp = espRepository.save(esp);

        // Then
        assertThat(savedEsp).isNotNull();
        assertThat(savedEsp.getId()).isEqualTo("ESP-001");
    }

    @Test
    void findEspById_ShouldReturnEsp() throws JsonProcessingException {
        // Given
        Esp esp = new Esp();
        esp.setId("ESP-001");
        esp.setEchoPinsJson(objectMapper.writeValueAsString(Arrays.asList(36, 39, 34, 35)));
        esp.setTriggerPinsJson(objectMapper.writeValueAsString(Arrays.asList(23, 22, 21, 19)));
        espRepository.save(esp);

        // When
        Optional<Esp> foundEsp = espRepository.findById("ESP-001");

        // Then
        assertThat(foundEsp).isPresent();
        assertThat(foundEsp.get().getId()).isEqualTo("ESP-001");
    }
} 