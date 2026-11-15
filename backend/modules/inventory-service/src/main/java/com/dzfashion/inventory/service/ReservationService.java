package com.dzfashion.inventory.service;

import com.dzfashion.inventory.dto.request.CommitReservationRequest;
import com.dzfashion.inventory.dto.request.ReleaseStockRequest;
import com.dzfashion.inventory.dto.request.ReserveStockRequest;
import com.dzfashion.inventory.dto.response.ActionResponse;
import com.dzfashion.inventory.dto.response.ReservationResponse;
import com.dzfashion.inventory.entity.StockReservationEntity;
import com.dzfashion.inventory.entity.WarehouseStockEntity;
import com.dzfashion.inventory.enums.ReservationStatus;
import com.dzfashion.inventory.enums.StockMovementReason;
import com.dzfashion.inventory.exception.DuplicateReservationException;
import com.dzfashion.inventory.exception.InsufficientStockException;
import com.dzfashion.inventory.exception.ResourceNotFoundException;
import com.dzfashion.inventory.repository.StockReservationRepository;
import com.dzfashion.inventory.repository.WarehouseStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ReservationService {

    private final WarehouseStockRepository stockRepository;
    private final StockReservationRepository reservationRepository;
    private final StockMovementService movementService;
    private final AlertNotificationService alertService;

    public ReservationService(WarehouseStockRepository stockRepository,
                              StockReservationRepository reservationRepository,
                              StockMovementService movementService,
                              AlertNotificationService alertService) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
        this.movementService = movementService;
        this.alertService = alertService;
    }

    @Transactional
    public ReservationResponse reserveStock(ReserveStockRequest request, UUID actorId) {
        // Check idempotency
        if (request.idempotencyKey() != null) {
            Optional<StockReservationEntity> existing = reservationRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                StockReservationEntity reservation = existing.get();
                if (reservation.getOrderId().equals(request.orderId()) && reservation.getVariantId().equals(request.variantId())) {
                    return mapToResponse(reservation);
                } else {
                    throw new DuplicateReservationException("Idempotency key already used for a different order or variant: " + request.idempotencyKey());
                }
            }
        }

        // Concurrency-safe pessimistic row lock
        WarehouseStockEntity stock = stockRepository.findByVariantIdWithLock(request.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for variant ID: " + request.variantId()));

        int available = stock.getAvailableQty();
        if (available < request.quantity()) {
            throw new InsufficientStockException(String.format(
                    "Insufficient available stock for variant %s. Requested: %d, Available: %d",
                    stock.getSku(), request.quantity(), available
            ));
        }

        // Apply reservation
        stock.setReservedQty(stock.getReservedQty() + request.quantity());
        stockRepository.save(stock);

        StockReservationEntity reservation = new StockReservationEntity(
                request.orderId(),
                request.variantId(),
                request.quantity(),
                request.idempotencyKey(),
                null
        );
        StockReservationEntity saved = reservationRepository.save(reservation);

        // Record immutable ledger movement
        movementService.recordMovement(
                stock.getVariantId(),
                -request.quantity(),
                stock.getAvailableQty(),
                StockMovementReason.ORDER_RESERVATION,
                "ORDER",
                request.orderId().toString(),
                actorId
        );

        // Evaluate low stock trigger
        alertService.evaluateStockLevel(stock);

        return mapToResponse(saved);
    }

    @Transactional
    public ActionResponse releaseStock(ReleaseStockRequest request, UUID actorId) {
        // Idempotent release: if already released or nonexistent, return cleanly
        Optional<StockReservationEntity> reservationOpt = reservationRepository
                .findByOrderIdAndVariantIdAndStatus(request.orderId(), request.variantId(), ReservationStatus.RESERVED);

        if (reservationOpt.isEmpty()) {
            return new ActionResponse("Reservation already released or does not exist");
        }

        StockReservationEntity reservation = reservationOpt.get();

        WarehouseStockEntity stock = stockRepository.findByVariantIdWithLock(request.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for variant ID: " + request.variantId()));

        stock.setReservedQty(Math.max(0, stock.getReservedQty() - reservation.getQuantity()));
        stockRepository.save(stock);

        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(reservation);

        movementService.recordMovement(
                stock.getVariantId(),
                reservation.getQuantity(),
                stock.getAvailableQty(),
                StockMovementReason.ORDER_RELEASE,
                "ORDER_CANCEL",
                request.orderId().toString(),
                actorId
        );

        alertService.evaluateStockLevel(stock);

        return new ActionResponse("Stock reservation successfully released");
    }

    @Transactional
    public ActionResponse commitReservation(CommitReservationRequest request, UUID actorId) {
        Optional<StockReservationEntity> reservationOpt = reservationRepository
                .findByOrderIdAndVariantIdAndStatus(request.orderId(), request.variantId(), ReservationStatus.RESERVED);

        if (reservationOpt.isEmpty()) {
            return new ActionResponse("Reservation already committed or not found");
        }

        StockReservationEntity reservation = reservationOpt.get();

        WarehouseStockEntity stock = stockRepository.findByVariantIdWithLock(request.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for variant ID: " + request.variantId()));

        stock.setPhysicalQty(Math.max(0, stock.getPhysicalQty() - reservation.getQuantity()));
        stock.setReservedQty(Math.max(0, stock.getReservedQty() - reservation.getQuantity()));
        stockRepository.save(stock);

        reservation.setStatus(ReservationStatus.COMMITTED);
        reservationRepository.save(reservation);

        movementService.recordMovement(
                stock.getVariantId(),
                -reservation.getQuantity(),
                stock.getAvailableQty(),
                StockMovementReason.DISPATCH,
                "ORDER_DISPATCH",
                request.orderId().toString(),
                actorId
        );

        alertService.evaluateStockLevel(stock);

        return new ActionResponse("Reservation successfully committed to dispatch");
    }

    public ReservationResponse mapToResponse(StockReservationEntity entity) {
        return new ReservationResponse(
                entity.getId(),
                entity.getOrderId(),
                entity.getVariantId(),
                entity.getQuantity(),
                entity.getStatus(),
                entity.getIdempotencyKey(),
                entity.getCreatedAt()
        );
    }
}
