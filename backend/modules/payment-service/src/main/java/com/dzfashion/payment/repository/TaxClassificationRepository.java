package com.dzfashion.payment.repository;

import com.dzfashion.payment.entity.TaxClassificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxClassificationRepository extends JpaRepository<TaxClassificationEntity, UUID> {
    Optional<TaxClassificationEntity> findByHsnCode(String hsnCode);
    List<TaxClassificationEntity> findByActiveTrue();
}
