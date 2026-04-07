package com.mountadem.helpdesk.knowledge.dto;

public record KnowledgeArticleResponse(
        String articleId,
        String title,
        String category,
        String teamName,
        String relevanceReason,
        Integer relevanceScore,
        String summary,
        String recommendedChecks,
        String escalationNotes
) {
}
