package com.dzfashion.fulfillment.repository;

import com.dzfashion.fulfillment.entity.TransportAgencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransportAgencyRepository extends JpaRepository<TransportAgencyEntity, UUID> {
    Optional<TransportAgencyEntity> findByName(String name);
    List<TransportAgencyEntity> findByActiveTrueOrderByNameAsc();
}
