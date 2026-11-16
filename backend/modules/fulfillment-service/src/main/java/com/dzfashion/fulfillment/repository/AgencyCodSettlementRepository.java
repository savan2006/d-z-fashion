package com.dzfashion.fulfillment.repository;

import com.dzfashion.fulfillment.entity.AgencyCodSettlementEntity;
import com.dzfashion.fulfillment.enums.AgencyCodSettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgencyCodSettlementRepository extends JpaRepository<AgencyCodSettlementEntity, UUID> {
    Optional<AgencyCodSettlementEntity> findByBatchReference(String batchReference);
    List<AgencyCodSettlementEntity> findByTransportAgencyIdOrderBySettlementDateDesc(UUID transportAgencyId);
    List<AgencyCodSettlementEntity> findByStatusOrderBySettlementDateDesc(AgencyCodSettlementStatus status);
    Page<AgencyCodSettlementEntity> findAllByOrderBySettlementDateDesc(Pageable pageable);
}
