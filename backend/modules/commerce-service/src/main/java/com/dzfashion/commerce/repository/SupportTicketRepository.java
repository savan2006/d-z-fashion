package com.dzfashion.commerce.repository;

import com.dzfashion.commerce.entity.SupportTicketEntity;
import com.dzfashion.commerce.enums.SupportTicketChannel;
import com.dzfashion.commerce.enums.SupportTicketPriority;
import com.dzfashion.commerce.enums.SupportTicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicketEntity, UUID>, JpaSpecificationExecutor<SupportTicketEntity> {

    Optional<SupportTicketEntity> findByTicketNumber(String ticketNumber);

    Page<SupportTicketEntity> findByCustomerId(UUID customerId, Pageable pageable);

    Page<SupportTicketEntity> findByStatus(SupportTicketStatus status, Pageable pageable);

    Page<SupportTicketEntity> findByAssignedManagerId(UUID assignedManagerId, Pageable pageable);

    Page<SupportTicketEntity> findByEscalatedTrue(Pageable pageable);

    long countByStatus(SupportTicketStatus status);

    long countByEscalatedTrue();

    long countByStatusIn(Iterable<SupportTicketStatus> statuses);
}
