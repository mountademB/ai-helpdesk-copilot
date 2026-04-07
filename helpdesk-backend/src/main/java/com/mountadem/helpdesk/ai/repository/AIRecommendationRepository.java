package com.mountadem.helpdesk.ai.repository;

import com.mountadem.helpdesk.ai.entity.AIRecommendation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {

    Optional<AIRecommendation> findFirstByTicketIdOrderByCreatedAtDesc(Long ticketId);

    long countByAnalysisSource(String analysisSource);
}
