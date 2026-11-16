package com.dzfashion.fulfillment.repository;

import com.dzfashion.fulfillment.entity.DailyDispatchBatchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyDispatchBatchRepository extends JpaRepository<DailyDispatchBatchEntity, UUID> {
    Optional<DailyDispatchBatchEntity> findByBatchNumber(String batchNumber);
    List<DailyDispatchBatchEntity> findByDispatchDateOrderByCreatedAtDesc(LocalDate dispatchDate);
    Page<DailyDispatchBatchEntity> findAllByOrderByDispatchDateDesc(Pageable pageable);
}
