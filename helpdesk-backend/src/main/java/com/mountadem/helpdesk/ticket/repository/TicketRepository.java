package com.mountadem.helpdesk.ticket.repository;

import com.mountadem.helpdesk.common.enums.TicketStatus;
import com.mountadem.helpdesk.ticket.entity.Ticket;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByReferenceCode(String referenceCode);
    boolean existsByReferenceCode(String referenceCode);
    long countByStatus(TicketStatus status);
    long countByAiAnalyzed(boolean aiAnalyzed);
}
