package com.dzfashion.payment.repository;

import com.dzfashion.payment.entity.PromotionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<PromotionEntity, UUID> {
    Optional<PromotionEntity> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PromotionEntity p WHERE p.id = :id")
    Optional<PromotionEntity> findByIdWithLock(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PromotionEntity p WHERE UPPER(p.code) = UPPER(:code)")
    Optional<PromotionEntity> findByCodeWithLock(@Param("code") String code);

    List<PromotionEntity> findByActiveTrue();
    Page<PromotionEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
