package com.dzfashion.fulfillment.repository;

import com.dzfashion.fulfillment.entity.ReturnInspectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnInspectionRepository extends JpaRepository<ReturnInspectionEntity, UUID> {
    Optional<ReturnInspectionEntity> findByReturnRequestId(UUID returnRequestId);
}
