package com.mountadem.helpdesk.feedback.repository;

import com.mountadem.helpdesk.feedback.entity.AIFeedbackEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIFeedbackEventRepository extends JpaRepository<AIFeedbackEvent, Long> {

    List<AIFeedbackEvent> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    long countByEventType(String eventType);

    long countByEventTypeAndEventValue(String eventType, String eventValue);
}
