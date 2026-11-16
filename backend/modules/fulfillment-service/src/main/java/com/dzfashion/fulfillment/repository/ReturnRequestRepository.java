package com.dzfashion.fulfillment.repository;

import com.dzfashion.fulfillment.entity.ReturnRequestEntity;
import com.dzfashion.fulfillment.enums.ReturnRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequestEntity, UUID> {

    Optional<ReturnRequestEntity> findByReturnNumber(String returnNumber);

    List<ReturnRequestEntity> findByOrderId(UUID orderId);

    Page<ReturnRequestEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    List<ReturnRequestEntity> findByStatus(ReturnRequestStatus status);

    Page<ReturnRequestEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM ReturnRequestEntity r " +
            "WHERE r.orderItemId = :orderItemId AND r.status NOT IN ('REJECTED', 'CLOSED')")
    int countActiveReturnQuantityByOrderItemId(@Param("orderItemId") UUID orderItemId);
}
