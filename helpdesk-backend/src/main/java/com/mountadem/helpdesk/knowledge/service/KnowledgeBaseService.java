package com.mountadem.helpdesk.knowledge.service;

import com.mountadem.helpdesk.ai.entity.AIRecommendation;
import com.mountadem.helpdesk.common.enums.TicketCategory;
import com.mountadem.helpdesk.knowledge.dto.KnowledgeArticleResponse;
import com.mountadem.helpdesk.knowledge.dto.KnowledgeGuidanceResponse;
import com.mountadem.helpdesk.team.entity.Team;
import com.mountadem.helpdesk.ticket.entity.Ticket;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseService {

    private final List<KnowledgeArticle> articles = List.of(
            new KnowledgeArticle(
                    "KB-ACC-001",
                    "Account Access Lockout Recovery",
                    TicketCategory.ACCOUNT_ACCESS.name(),
                    "Account Management",
                    "Guide for locked accounts, repeated failed login attempts, and recovery workflow.",
                    """
1. Confirm the exact account identifier and last successful login.
2. Check lockout status, password reset completion, and MFA enrollment.
3. Validate whether the issue is isolated or tenant-wide.
4. Re-test after unlock/reset before escalating.
""",
                    "Escalate if the user remains blocked after reset, unlock, and identity verification.",
                    List.of("account", "access", "password", "login", "credentials", "signin", "locked", "lockout", "mfa")
            ),
            new KnowledgeArticle(
                    "KB-ACC-002",
                    "VPN and SSO Sign-In Checklist",
                    TicketCategory.ACCOUNT_ACCESS.name(),
                    "Account Management",
                    "Checklist for account access issues involving VPN, SSO, or identity provider handoff.",
                    """
1. Confirm whether VPN is required for the workflow.
2. Check SSO provider status and recent auth-related incidents.
3. Verify user group, role mapping, and session expiry.
4. Capture exact error wording and timestamp.
""",
                    "Escalate to Platform if SSO/VPN dependency appears degraded for multiple users.",
                    List.of("vpn", "sso", "signin", "session", "identity", "login", "access")
            ),
            new KnowledgeArticle(
                    "KB-BIL-001",
                    "Duplicate Charge Investigation",
                    TicketCategory.BILLING.name(),
                    "Billing",
                    "Steps for reviewing duplicate charges, billing overlap, and refund decision path.",
                    """
1. Compare invoice records with payment processor transactions.
2. Check whether the customer was charged under overlapping plans or retries.
3. Confirm amount, period, and payment method.
4. Document findings before issuing or requesting refund approval.
""",
                    "Escalate if duplicate billing is confirmed or if refund approval is needed.",
                    List.of("billing", "invoice", "charge", "charged", "payment", "refund", "duplicate", "subscription")
            ),
            new KnowledgeArticle(
                    "KB-PLT-001",
                    "Service Error and Degradation Triage",
                    TicketCategory.TECHNICAL_ISSUE.name(),
                    "Platform",
                    "Standard triage for runtime errors, failed actions, degraded services, and recent deployments.",
                    """
1. Identify whether the issue is reproducible and capture exact steps.
2. Check service health, deployment history, and recent config changes.
3. Confirm scope: single user, tenant, or global.
4. Attach logs, timestamps, and screenshots before escalation.
""",
                    "Escalate to Platform/Engineering if reproducible or if multiple users are impacted.",
                    List.of("error", "failed", "failure", "broken", "service", "degraded", "issue", "platform", "outage")
            ),
            new KnowledgeArticle(
                    "KB-PLT-002",
                    "Bug Reproduction Checklist",
                    TicketCategory.BUG_REPORT.name(),
                    "Platform",
                    "Checklist for suspected product defects and reproducible workflow failures.",
                    """
1. Reproduce in a clean environment with the same permissions.
2. Record exact inputs, steps, and expected versus actual behavior.
3. Compare against recent releases or known defects.
4. Prepare a concise reproduction note for engineering.
""",
                    "Escalate when a defect is reproducible or likely tied to a release regression.",
                    List.of("bug", "crash", "exception", "stacktrace", "reproduce", "defect", "regression")
            ),
            new KnowledgeArticle(
                    "KB-GEN-001",
                    "General Intake and Clarification Checklist",
                    TicketCategory.GENERAL_SUPPORT.name(),
                    "Support",
                    "Baseline intake flow for ambiguous or incomplete support requests.",
                    """
1. Clarify the desired outcome and business impact.
2. Ask for screenshots, timestamps, or steps if missing.
3. Confirm whether there is a workaround or urgency.
4. Route to the correct team once the issue type is clear.
""",
                    "Escalate only after clarifying ownership or confirming wider impact.",
                    List.of("support", "general", "help", "clarify", "request", "question", "workflow")
            )
    );

    public KnowledgeGuidanceResponse buildGuidance(Ticket ticket, AIRecommendation latestRecommendation) {
        String referenceCategory = latestRecommendation != null && latestRecommendation.getPredictedCategory() != null
                ? latestRecommendation.getPredictedCategory().name()
                : ticket.getCategory().name();

        String referenceTeam = latestRecommendation != null && latestRecommendation.getSuggestedTeam() != null
                ? latestRecommendation.getSuggestedTeam().getName()
                : ticket.getTeam() != null ? ticket.getTeam().getName() : null;

        List<String> referenceKeywords = extractKeywords(ticket.getTitle() + " " + ticket.getDescription());

        List<ScoredArticle> matches = articles.stream()
                .map(article -> scoreArticle(article, referenceCategory, referenceTeam, referenceKeywords))
                .filter(scored -> scored.score() > 0)
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .limit(3)
                .toList();

        if (matches.isEmpty()) {
            return new KnowledgeGuidanceResponse(
                    "No matching knowledge articles were found yet.",
                    "1. Clarify the issue and collect more evidence.\n2. Re-run analysis after additional context is available.",
                    "Escalate only if the issue impacts multiple users or blocks a critical workflow.",
                    List.of()
            );
        }

        List<KnowledgeArticleResponse> articleResponses = matches.stream()
                .map(scored -> new KnowledgeArticleResponse(
                        scored.article().articleId(),
                        scored.article().title(),
                        scored.article().category(),
                        scored.article().teamName(),
                        scored.reason(),
                        scored.score(),
                        scored.article().summary(),
                        scored.article().recommendedChecks(),
                        scored.article().escalationNotes()
                ))
                .toList();

        String summary = "Grounded from " + articleResponses.size() + " internal article"
                + (articleResponses.size() == 1 ? "" : "s")
                + ": "
                + articleResponses.stream().map(KnowledgeArticleResponse::title).collect(Collectors.joining("; "));

        String recommendedChecks = numberedMerge(
                articleResponses.stream().map(KnowledgeArticleResponse::recommendedChecks).toList(),
                8
        );

        String escalationGuidance = articleResponses.stream()
                .map(KnowledgeArticleResponse::escalationNotes)
                .distinct()
                .collect(Collectors.joining(" "));

        return new KnowledgeGuidanceResponse(summary, recommendedChecks, escalationGuidance, articleResponses);
    }

    private ScoredArticle scoreArticle(
            KnowledgeArticle article,
            String referenceCategory,
            String referenceTeam,
            List<String> referenceKeywords
    ) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (article.category().equals(referenceCategory)) {
            score += "GENERAL_SUPPORT".equals(referenceCategory) ? 2 : 5;
            reasons.add("Same category");
        }

        if (referenceTeam != null && article.teamName().equals(referenceTeam)) {
            score += 3;
            reasons.add("Same team");
        }

        List<String> overlap = referenceKeywords.stream()
                .filter(article.keywords()::contains)
                .distinct()
                .limit(3)
                .toList();

        if (!overlap.isEmpty()) {
            score += Math.min(4, overlap.size() + 1);
            reasons.add("Shared terms: " + String.join(", ", overlap));
        }

        return new ScoredArticle(article, score, reasons.isEmpty() ? "General relevance" : String.join(" | ", reasons));
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Set<String> stopWords = Set.of(
                "the", "and", "for", "with", "from", "that", "this", "have", "keep", "after", "into", "your",
                "you", "are", "was", "were", "will", "been", "them", "they", "their", "our", "can", "cannot",
                "cant", "not", "but", "too", "get", "got", "had", "has", "did", "does", "done", "using",
                "user", "ticket", "issue", "need", "help", "please", "still", "more", "than", "when", "then"
        );

        return new LinkedHashSet<>(
                List.of(text.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9 ]", " ")
                        .split("\\s+"))
        ).stream()
                .filter(token -> token.length() >= 4)
                .filter(token -> !stopWords.contains(token))
                .limit(12)
                .toList();
    }

    private String numberedMerge(List<String> blocks, int maxLines) {
        List<String> lines = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String block : blocks) {
            if (block == null || block.isBlank()) {
                continue;
            }

            for (String rawLine : block.split("\\R")) {
                String line = rawLine.trim();
                if (line.isBlank()) {
                    continue;
                }

                line = line.replaceFirst("^\\d+\\.\\s*", "").trim();

                if (seen.add(line)) {
                    lines.add(line);
                }

                if (lines.size() >= maxLines) {
                    break;
                }
            }

            if (lines.size() >= maxLines) {
                break;
            }
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            builder.append(i + 1).append(". ").append(lines.get(i));
            if (i < lines.size() - 1) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    private record KnowledgeArticle(
            String articleId,
            String title,
            String category,
            String teamName,
            String summary,
            String recommendedChecks,
            String escalationNotes,
            List<String> keywords
    ) {
    }

    private record ScoredArticle(KnowledgeArticle article, int score, String reason) {
    }
}
