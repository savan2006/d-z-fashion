package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.dto.request.CreateTransportAgencyRequest;
import com.dzfashion.fulfillment.dto.request.UpdateTransportAgencyRequest;
import com.dzfashion.fulfillment.dto.response.TransportAgencyResponse;
import com.dzfashion.fulfillment.entity.TransportAgencyEntity;
import com.dzfashion.fulfillment.exception.ResourceConflictException;
import com.dzfashion.fulfillment.exception.ResourceNotFoundException;
import com.dzfashion.fulfillment.repository.TransportAgencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TransportAgencyService {

    private static final Logger log = LoggerFactory.getLogger(TransportAgencyService.class);

    private final TransportAgencyRepository transportAgencyRepository;

    public TransportAgencyService(TransportAgencyRepository transportAgencyRepository) {
        this.transportAgencyRepository = transportAgencyRepository;
    }

    public TransportAgencyResponse createAgency(CreateTransportAgencyRequest request) {
        String name = request.name().trim();
        if (transportAgencyRepository.findByName(name).isPresent()) {
            throw new ResourceConflictException("Transport agency with name '" + name + "' already exists");
        }

        TransportAgencyEntity entity = new TransportAgencyEntity(
                name,
                request.contactPerson() != null ? request.contactPerson().trim() : null,
                request.phoneNumber().trim()
        );

        TransportAgencyEntity saved = transportAgencyRepository.save(entity);
        log.info("Created transport agency {} (ID: {})", saved.getName(), saved.getId());
        return mapToResponse(saved);
    }

    public TransportAgencyResponse updateAgency(UUID id, UpdateTransportAgencyRequest request) {
        TransportAgencyEntity entity = transportAgencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transport agency not found with ID: " + id));

        entity.setContactPerson(request.contactPerson() != null ? request.contactPerson().trim() : null);
        entity.setPhoneNumber(request.phoneNumber().trim());
        entity.setActive(request.active());

        TransportAgencyEntity updated = transportAgencyRepository.save(entity);
        log.info("Updated transport agency {} (ID: {})", updated.getName(), updated.getId());
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<TransportAgencyResponse> getActiveAgencies() {
        return transportAgencyRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransportAgencyResponse> getAllAgencies() {
        return transportAgencyRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransportAgencyResponse getAgencyById(UUID id) {
        TransportAgencyEntity entity = transportAgencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transport agency not found with ID: " + id));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public TransportAgencyEntity getAgencyEntityById(UUID id) {
        return transportAgencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transport agency not found with ID: " + id));
    }

    private TransportAgencyResponse mapToResponse(TransportAgencyEntity entity) {
        return new TransportAgencyResponse(
                entity.getId(),
                entity.getName(),
                entity.getContactPerson(),
                entity.getPhoneNumber(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
