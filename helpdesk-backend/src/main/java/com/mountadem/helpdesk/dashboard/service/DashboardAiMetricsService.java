package com.mountadem.helpdesk.dashboard.service;

import com.mountadem.helpdesk.ai.repository.AIRecommendationRepository;
import com.mountadem.helpdesk.dashboard.dto.DashboardAiMetricsResponse;
import com.mountadem.helpdesk.feedback.repository.AIFeedbackEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardAiMetricsService {

    private final AIRecommendationRepository aiRecommendationRepository;
    private final AIFeedbackEventRepository aiFeedbackEventRepository;

    public DashboardAiMetricsResponse getMetrics() {
        long totalAnalyses = aiRecommendationRepository.count();
        long acceptedRecommendations = aiFeedbackEventRepository.countByEventTypeAndEventValue("REVIEW_DECISION", "ACCEPTED");
        long rejectedRecommendations = aiFeedbackEventRepository.countByEventTypeAndEventValue("REVIEW_DECISION", "REJECTED");
        long rewriteCount = aiFeedbackEventRepository.countByEventType("REWRITE_USED");
        long fallbackAnalyses = aiRecommendationRepository.countByAnalysisSource("RULE_FALLBACK");

        long reviewedTotal = acceptedRecommendations + rejectedRecommendations;
        double acceptanceRate = reviewedTotal == 0 ? 0.0 : (acceptedRecommendations * 100.0) / reviewedTotal;

        return new DashboardAiMetricsResponse(
                totalAnalyses,
                acceptedRecommendations,
                rejectedRecommendations,
                rewriteCount,
                fallbackAnalyses,
                acceptanceRate
        );
    }
}
