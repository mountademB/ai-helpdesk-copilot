package com.mountadem.helpdesk.dashboard.dto;

public record DashboardAiMetricsResponse(
        long totalAnalyses,
        long acceptedRecommendations,
        long rejectedRecommendations,
        long rewriteCount,
        long fallbackAnalyses,
        double acceptanceRate
) {
}
