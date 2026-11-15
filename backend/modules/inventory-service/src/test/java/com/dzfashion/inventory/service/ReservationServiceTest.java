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
import com.dzfashion.inventory.repository.StockReservationRepository;
import com.dzfashion.inventory.repository.WarehouseStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private WarehouseStockRepository stockRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @Mock
    private StockMovementService movementService;

    @Mock
    private AlertNotificationService alertService;

    @InjectMocks
    private ReservationService reservationService;

    private UUID variantId;
    private UUID orderId;
    private UUID actorId;
    private WarehouseStockEntity stockEntity;

    @BeforeEach
    void setUp() {
        variantId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        stockEntity = new WarehouseStockEntity(null, variantId, "DZF-TEST-SKU", 10, 5);
        stockEntity.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Reserve stock succeeds when available quantity is sufficient")
    void reserveStockSuccess() {
        ReserveStockRequest request = new ReserveStockRequest(orderId, variantId, 3, "IDEM-001");

        when(reservationRepository.findByIdempotencyKey("IDEM-001")).thenReturn(Optional.empty());
        when(stockRepository.findByVariantIdWithLock(variantId)).thenReturn(Optional.of(stockEntity));
        when(reservationRepository.save(any(StockReservationEntity.class))).thenAnswer(inv -> {
            StockReservationEntity r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ReservationResponse response = reservationService.reserveStock(request, actorId);

        assertThat(response).isNotNull();
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(stockEntity.getReservedQty()).isEqualTo(3);
        assertThat(stockEntity.getAvailableQty()).isEqualTo(7);

        verify(movementService).recordMovement(eq(variantId), eq(-3), eq(7), eq(StockMovementReason.ORDER_RESERVATION), anyString(), anyString(), eq(actorId));
        verify(alertService).evaluateStockLevel(stockEntity);
    }

    @Test
    @DisplayName("Reserve stock throws InsufficientStockException when requested > available")
    void reserveStockInsufficient() {
        ReserveStockRequest request = new ReserveStockRequest(orderId, variantId, 15, "IDEM-002");

        when(reservationRepository.findByIdempotencyKey("IDEM-002")).thenReturn(Optional.empty());
        when(stockRepository.findByVariantIdWithLock(variantId)).thenReturn(Optional.of(stockEntity));

        assertThatThrownBy(() -> reservationService.reserveStock(request, actorId))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient available stock");

        assertThat(stockEntity.getReservedQty()).isEqualTo(0);
        verify(movementService, never()).recordMovement(any(), anyInt(), anyInt(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Idempotent reservation returns existing reservation without double deduction")
    void reserveStockIdempotent() {
        StockReservationEntity existing = new StockReservationEntity(orderId, variantId, 3, "IDEM-003", null);
        existing.setId(UUID.randomUUID());

        when(reservationRepository.findByIdempotencyKey("IDEM-003")).thenReturn(Optional.of(existing));

        ReserveStockRequest request = new ReserveStockRequest(orderId, variantId, 3, "IDEM-003");
        ReservationResponse response = reservationService.reserveStock(request, actorId);

        assertThat(response.reservationId()).isEqualTo(existing.getId());
        verify(stockRepository, never()).findByVariantIdWithLock(any());
    }

    @Test
    @DisplayName("Duplicate idempotency key for different order throws DuplicateReservationException")
    void reserveStockDuplicateKeyConflict() {
        UUID differentOrderId = UUID.randomUUID();
        StockReservationEntity existing = new StockReservationEntity(differentOrderId, variantId, 3, "IDEM-CONFLICT", null);

        when(reservationRepository.findByIdempotencyKey("IDEM-CONFLICT")).thenReturn(Optional.of(existing));

        ReserveStockRequest request = new ReserveStockRequest(orderId, variantId, 3, "IDEM-CONFLICT");

        assertThatThrownBy(() -> reservationService.reserveStock(request, actorId))
                .isInstanceOf(DuplicateReservationException.class)
                .hasMessageContaining("Idempotency key already used");
    }

    @Test
    @DisplayName("Release stock reduces reserved quantity and marks reservation RELEASED")
    void releaseStockSuccess() {
        stockEntity.setReservedQty(3);
        StockReservationEntity reservation = new StockReservationEntity(orderId, variantId, 3, "IDEM-004", null);
        reservation.setId(UUID.randomUUID());

        when(reservationRepository.findByOrderIdAndVariantIdAndStatus(orderId, variantId, ReservationStatus.RESERVED))
                .thenReturn(Optional.of(reservation));
        when(stockRepository.findByVariantIdWithLock(variantId)).thenReturn(Optional.of(stockEntity));

        ReleaseStockRequest request = new ReleaseStockRequest(orderId, variantId, "IDEM-RELEASE");
        ActionResponse response = reservationService.releaseStock(request, actorId);

        assertThat(response.message()).contains("successfully released");
        assertThat(stockEntity.getReservedQty()).isEqualTo(0);
        assertThat(stockEntity.getAvailableQty()).isEqualTo(10);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);

        verify(movementService).recordMovement(eq(variantId), eq(3), eq(10), eq(StockMovementReason.ORDER_RELEASE), anyString(), anyString(), eq(actorId));
    }

    @Test
    @DisplayName("Release stock is idempotent: second release call returns cleanly without increasing stock twice")
    void releaseStockIdempotent() {
        when(reservationRepository.findByOrderIdAndVariantIdAndStatus(orderId, variantId, ReservationStatus.RESERVED))
                .thenReturn(Optional.empty());

        ReleaseStockRequest request = new ReleaseStockRequest(orderId, variantId, "IDEM-RELEASE-2");
        ActionResponse response = reservationService.releaseStock(request, actorId);

        assertThat(response.message()).contains("already released or does not exist");
        verify(stockRepository, never()).findByVariantIdWithLock(any());
    }

    @Test
    @DisplayName("Commit reservation reduces physical and reserved quantities upon dispatch")
    void commitReservationSuccess() {
        stockEntity.setPhysicalQty(10);
        stockEntity.setReservedQty(2);

        StockReservationEntity reservation = new StockReservationEntity(orderId, variantId, 2, "IDEM-005", null);
        reservation.setId(UUID.randomUUID());

        when(reservationRepository.findByOrderIdAndVariantIdAndStatus(orderId, variantId, ReservationStatus.RESERVED))
                .thenReturn(Optional.of(reservation));
        when(stockRepository.findByVariantIdWithLock(variantId)).thenReturn(Optional.of(stockEntity));

        CommitReservationRequest request = new CommitReservationRequest(orderId, variantId);
        ActionResponse response = reservationService.commitReservation(request, actorId);

        assertThat(response.message()).contains("successfully committed");
        assertThat(stockEntity.getPhysicalQty()).isEqualTo(8);
        assertThat(stockEntity.getReservedQty()).isEqualTo(0);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMMITTED);

        verify(movementService).recordMovement(eq(variantId), eq(-2), eq(8), eq(StockMovementReason.DISPATCH), anyString(), anyString(), eq(actorId));
    }
}
