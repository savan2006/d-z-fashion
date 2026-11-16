package com.dzfashion.payment.repository;

import com.dzfashion.payment.entity.InvoiceSequenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequenceEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceSequenceEntity s WHERE s.fiscalYear = :fiscalYear")
    Optional<InvoiceSequenceEntity> findByFiscalYearWithLock(@Param("fiscalYear") String fiscalYear);
}
