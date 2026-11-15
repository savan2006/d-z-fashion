package com.dzfashion.inventory.repository;

import com.dzfashion.inventory.entity.StockReservationEntity;
import com.dzfashion.inventory.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservationEntity, UUID> {

    Optional<StockReservationEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<StockReservationEntity> findByOrderId(UUID orderId);

    List<StockReservationEntity> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    Optional<StockReservationEntity> findByOrderIdAndVariantIdAndStatus(UUID orderId, UUID variantId, ReservationStatus status);
}
