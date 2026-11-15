package com.dzfashion.commerce.repository;

import com.dzfashion.commerce.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {
    Optional<CartItemEntity> findByCartIdAndVariantId(UUID cartId, UUID variantId);
    List<CartItemEntity> findByCartId(UUID cartId);
}
