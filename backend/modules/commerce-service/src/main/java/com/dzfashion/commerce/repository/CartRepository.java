package com.dzfashion.commerce.repository;

import com.dzfashion.commerce.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<CartEntity, UUID> {
    Optional<CartEntity> findByCustomerId(UUID customerId);
}
