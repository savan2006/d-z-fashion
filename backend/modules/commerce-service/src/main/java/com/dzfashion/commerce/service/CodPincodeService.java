package com.dzfashion.commerce.service;

import com.dzfashion.commerce.dto.request.AddCodPincodeRequest;
import com.dzfashion.commerce.dto.response.CodPincodeResponse;
import com.dzfashion.commerce.entity.CodServiceablePincodeEntity;
import com.dzfashion.commerce.exception.ResourceConflictException;
import com.dzfashion.commerce.exception.ResourceNotFoundException;
import com.dzfashion.commerce.repository.CodServiceablePincodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CodPincodeService {

    private final CodServiceablePincodeRepository pincodeRepository;

    public CodPincodeService(CodServiceablePincodeRepository pincodeRepository) {
        this.pincodeRepository = pincodeRepository;
    }

    @Transactional(readOnly = true)
    public boolean isPincodeServiceable(String pincode) {
        if (pincode == null || pincode.trim().isEmpty()) {
            return false;
        }
        return pincodeRepository.existsByPincodeAndActiveTrue(pincode.trim());
    }

    @Transactional(readOnly = true)
    public List<CodPincodeResponse> listServiceablePincodes() {
        return pincodeRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public CodPincodeResponse addPincode(AddCodPincodeRequest request, UUID ownerId) {
        String cleanPincode = request.pincode().trim();
        pincodeRepository.findByPincode(cleanPincode).ifPresent(p -> {
            throw new ResourceConflictException("Pincode " + cleanPincode + " already exists in the COD serviceability registry");
        });

        CodServiceablePincodeEntity entity = new CodServiceablePincodeEntity(cleanPincode, true, ownerId);
        CodServiceablePincodeEntity saved = pincodeRepository.save(entity);
        return mapToResponse(saved);
    }

    @Transactional
    public CodPincodeResponse togglePincodeActive(String pincode, boolean active) {
        String cleanPincode = pincode.trim();
        CodServiceablePincodeEntity entity = pincodeRepository.findByPincode(cleanPincode)
                .orElseThrow(() -> new ResourceNotFoundException("Pincode " + cleanPincode + " not found"));
        entity.setActive(active);
        return mapToResponse(pincodeRepository.save(entity));
    }

    @Transactional
    public void deletePincode(String pincode) {
        String cleanPincode = pincode.trim();
        CodServiceablePincodeEntity entity = pincodeRepository.findByPincode(cleanPincode)
                .orElseThrow(() -> new ResourceNotFoundException("Pincode " + cleanPincode + " not found"));
        pincodeRepository.delete(entity);
    }

    private CodPincodeResponse mapToResponse(CodServiceablePincodeEntity entity) {
        return new CodPincodeResponse(
                entity.getId(),
                entity.getPincode(),
                entity.isActive(),
                entity.getEnabledBy(),
                entity.getCreatedAt()
        );
    }
}
