package com.dzfashion.payment.repository;

import com.dzfashion.payment.entity.InvoiceEntity;
import com.dzfashion.payment.enums.InvoiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {
    Optional<InvoiceEntity> findByInvoiceNumber(String invoiceNumber);
    Optional<InvoiceEntity> findByOrderIdAndInvoiceType(UUID orderId, InvoiceType invoiceType);
    List<InvoiceEntity> findByOrderIdOrderByIssuedAtAsc(UUID orderId);
    Page<InvoiceEntity> findByCustomerIdOrderByIssuedAtDesc(UUID customerId, Pageable pageable);
    Page<InvoiceEntity> findAllByOrderByIssuedAtDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(i.totalTaxAmount), 0) FROM InvoiceEntity i WHERE i.invoiceType = :invoiceType")
    BigDecimal sumTaxAmountByInvoiceType(@Param("invoiceType") InvoiceType invoiceType);
}
