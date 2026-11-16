package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.dto.request.CreateTransportAgencyRequest;
import com.dzfashion.fulfillment.dto.request.UpdateTransportAgencyRequest;
import com.dzfashion.fulfillment.dto.response.TransportAgencyResponse;
import com.dzfashion.fulfillment.entity.TransportAgencyEntity;
import com.dzfashion.fulfillment.exception.ResourceConflictException;
import com.dzfashion.fulfillment.exception.ResourceNotFoundException;
import com.dzfashion.fulfillment.repository.TransportAgencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransportAgencyServiceTest {

    @Mock
    private TransportAgencyRepository transportAgencyRepository;

    @InjectMocks
    private TransportAgencyService transportAgencyService;

    private TransportAgencyEntity agencyEntity;
    private UUID agencyId;

    @BeforeEach
    void setUp() {
        agencyId = UUID.randomUUID();
        agencyEntity = new TransportAgencyEntity("Delhivery", "Surat Hub Lead", "+91 9876543210");
        agencyEntity.setId(agencyId);
    }

    @Test
    @DisplayName("Should create transport agency successfully")
    void testCreateAgency_Success() {
        CreateTransportAgencyRequest request = new CreateTransportAgencyRequest("Delhivery", "Surat Hub Lead", "+91 9876543210");
        when(transportAgencyRepository.findByName("Delhivery")).thenReturn(Optional.empty());
        when(transportAgencyRepository.save(any(TransportAgencyEntity.class))).thenAnswer(invocation -> {
            TransportAgencyEntity e = invocation.getArgument(0);
            e.setId(agencyId);
            return e;
        });

        TransportAgencyResponse response = transportAgencyService.createAgency(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(agencyId);
        assertThat(response.name()).isEqualTo("Delhivery");
        assertThat(response.active()).isTrue();
        verify(transportAgencyRepository).save(any(TransportAgencyEntity.class));
    }

    @Test
    @DisplayName("Should reject duplicate transport agency name")
    void testCreateAgency_DuplicateName() {
        CreateTransportAgencyRequest request = new CreateTransportAgencyRequest("Delhivery", "Lead", "+91 9876543210");
        when(transportAgencyRepository.findByName("Delhivery")).thenReturn(Optional.of(agencyEntity));

        assertThatThrownBy(() -> transportAgencyService.createAgency(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should update transport agency details")
    void testUpdateAgency_Success() {
        UpdateTransportAgencyRequest request = new UpdateTransportAgencyRequest("New Lead", "+91 9123456789", false);
        when(transportAgencyRepository.findById(agencyId)).thenReturn(Optional.of(agencyEntity));
        when(transportAgencyRepository.save(any(TransportAgencyEntity.class))).thenReturn(agencyEntity);

        TransportAgencyResponse response = transportAgencyService.updateAgency(agencyId, request);

        assertThat(response.contactPerson()).isEqualTo("New Lead");
        assertThat(response.phoneNumber()).isEqualTo("+91 9123456789");
        assertThat(response.active()).isFalse();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when agency does not exist")
    void testGetAgency_NotFound() {
        UUID unknownId = UUID.randomUUID();
        when(transportAgencyRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transportAgencyService.getAgencyById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should list active transport agencies")
    void testGetActiveAgencies() {
        when(transportAgencyRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(agencyEntity));

        List<TransportAgencyResponse> list = transportAgencyService.getActiveAgencies();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).name()).isEqualTo("Delhivery");
    }
}
