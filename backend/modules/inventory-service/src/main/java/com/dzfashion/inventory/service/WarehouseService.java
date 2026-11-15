package com.dzfashion.inventory.service;

import com.dzfashion.inventory.dto.response.WarehouseResponse;
import com.dzfashion.inventory.entity.WarehouseEntity;
import com.dzfashion.inventory.exception.ResourceNotFoundException;
import com.dzfashion.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WarehouseService {

    private static final String DEFAULT_WAREHOUSE_CODE = "SURAT-MAIN";

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public WarehouseEntity getOrCreateSuratWarehouse() {
        return warehouseRepository.findByCode(DEFAULT_WAREHOUSE_CODE)
                .orElseGet(() -> {
                    WarehouseEntity surat = new WarehouseEntity(
                            DEFAULT_WAREHOUSE_CODE,
                            "Surat Central Warehouse",
                            "Surat",
                            "Gujarat",
                            "India"
                    );
                    return warehouseRepository.save(surat);
                });
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseByCode(String code) {
        WarehouseEntity entity = warehouseRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with code: " + code));
        return mapToResponse(entity);
    }

    public WarehouseResponse mapToResponse(WarehouseEntity entity) {
        return new WarehouseResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getCity(),
                entity.getState(),
                entity.getCountry(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
