package com.mountadem.helpdesk.event.repository;

import com.mountadem.helpdesk.event.entity.TicketEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketEventRepository extends JpaRepository<TicketEvent, Long> {

    List<TicketEvent> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    List<TicketEvent> findTop10ByOrderByCreatedAtDesc();
}
