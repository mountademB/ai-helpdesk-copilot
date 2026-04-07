package com.mountadem.helpdesk.knowledge.dto;

import java.util.List;

public record KnowledgeGuidanceResponse(
        String guidanceSummary,
        String recommendedChecks,
        String escalationGuidance,
        List<KnowledgeArticleResponse> articles
) {
}
