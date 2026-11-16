package com.dzfashion.fulfillment.repository;

import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<ShipmentEntity, UUID> {
    Optional<ShipmentEntity> findByOrderId(UUID orderId);
    Optional<ShipmentEntity> findByOrderNumber(String orderNumber);
    Page<ShipmentEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
    List<ShipmentEntity> findByStatus(ShipmentStatus status);
    List<ShipmentEntity> findByStatusIn(Collection<ShipmentStatus> statuses);
    List<ShipmentEntity> findByDailyDispatchBatchId(UUID batchId);
    Page<ShipmentEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT s FROM ShipmentEntity s WHERE s.transportAgency.id = :agencyId AND s.status = 'DELIVERED' AND s.cod = true AND s.codSettlementBatchId IS NULL")
    List<ShipmentEntity> findDeliveredCodEligibleForSettlement(@Param("agencyId") UUID agencyId);

    @Query("SELECT s FROM ShipmentEntity s WHERE s.id IN :ids AND s.transportAgency.id = :agencyId AND s.status = 'DELIVERED' AND s.cod = true AND s.codSettlementBatchId IS NULL")
    List<ShipmentEntity> findDeliveredCodEligibleByIdsAndAgency(@Param("ids") Collection<UUID> ids, @Param("agencyId") UUID agencyId);

    long countByStatus(ShipmentStatus status);
}
