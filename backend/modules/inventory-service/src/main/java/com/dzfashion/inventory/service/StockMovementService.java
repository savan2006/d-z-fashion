package com.dzfashion.inventory.service;

import com.dzfashion.inventory.dto.response.PageResponse;
import com.dzfashion.inventory.dto.response.StockMovementResponse;
import com.dzfashion.inventory.entity.StockMovementEntity;
import com.dzfashion.inventory.enums.StockMovementReason;
import com.dzfashion.inventory.repository.StockMovementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StockMovementService {

    private final StockMovementRepository movementRepository;

    public StockMovementService(StockMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovementEntity recordMovement(UUID variantId, int changeQty, int balanceAfter,
                                             StockMovementReason reason, String referenceType,
                                             String referenceId, UUID actorId) {
        StockMovementEntity movement = new StockMovementEntity(
                variantId,
                changeQty,
                balanceAfter,
                reason,
                referenceType,
                referenceId,
                actorId
        );
        return movementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovementsByVariant(UUID variantId) {
        return movementRepository.findByVariantIdOrderByCreatedAtDesc(variantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> getAllMovements(Pageable pageable) {
        Page<StockMovementEntity> page = movementRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<StockMovementResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    public StockMovementResponse mapToResponse(StockMovementEntity entity) {
        return new StockMovementResponse(
                entity.getId(),
                entity.getVariantId(),
                entity.getChangeQty(),
                entity.getBalanceAfter(),
                entity.getReason(),
                entity.getReferenceType(),
                entity.getReferenceId(),
                entity.getActorId(),
                entity.getCreatedAt()
        );
    }
}
