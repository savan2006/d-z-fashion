package com.dzfashion.commerce.repository;

import com.dzfashion.commerce.entity.SupportTicketMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessageEntity, UUID> {

    List<SupportTicketMessageEntity> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    List<SupportTicketMessageEntity> findByTicketIdAndInternalNoteFalseOrderByCreatedAtAsc(UUID ticketId);
}
