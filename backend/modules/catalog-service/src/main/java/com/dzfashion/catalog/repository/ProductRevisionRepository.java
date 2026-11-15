package com.dzfashion.catalog.repository;

import com.dzfashion.catalog.entity.ProductRevisionEntity;
import com.dzfashion.catalog.enums.RevisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRevisionRepository extends JpaRepository<ProductRevisionEntity, UUID> {

    List<ProductRevisionEntity> findByProductIdOrderByCreatedAtDesc(UUID productId);

    List<ProductRevisionEntity> findByStatusOrderByCreatedAtDesc(RevisionStatus status);

    List<ProductRevisionEntity> findByProductIdAndStatus(UUID productId, RevisionStatus status);
}
