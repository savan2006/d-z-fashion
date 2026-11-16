package com.dzfashion.fulfillment.repository;

import com.dzfashion.fulfillment.entity.ShipmentItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItemEntity, UUID> {
    List<ShipmentItemEntity> findByShipmentId(UUID shipmentId);

    @Query("SELECT item FROM ShipmentItemEntity item WHERE item.shipment.id IN :shipmentIds ORDER BY item.sku ASC")
    List<ShipmentItemEntity> findByShipmentIdInOrderBySkuAsc(@Param("shipmentIds") Collection<UUID> shipmentIds);
}
