package com.mountadem.helpdesk.ai.service;

import com.mountadem.helpdesk.ai.dto.TicketAiAnalysis;
import com.mountadem.helpdesk.common.enums.TicketCategory;
import com.mountadem.helpdesk.common.enums.TicketPriority;
import com.mountadem.helpdesk.ticket.entity.Ticket;
import org.springframework.stereotype.Service;

@Service
public class RuleBasedTicketAiService {

    public TicketAiAnalysis analyze(Ticket ticket) {
        String text = (ticket.getTitle() + " " + ticket.getDescription()).toLowerCase();
        TicketCategory category = detectCategory(text);
        TicketPriority priority = detectPriority(text);

        return new TicketAiAnalysis(
                category.name(),
                priority.name(),
                suggestedTeamName(category),
                buildSummary(ticket),
                buildDraftReply(category),
                confidenceFor(category, priority),
                probableCauseFor(category),
                recommendedActionsFor(category, priority),
                escalationSuggestionFor(category, priority),
                "RULE"
        );
    }

    private TicketCategory detectCategory(String text) {
        if (containsAny(text, "password", "login", "access", "account", "credential", "credentials", "sign in", "vpn")) {
            return TicketCategory.ACCOUNT_ACCESS;
        }
        if (containsAny(text, "bill", "billing", "invoice", "payment", "charge", "charged")) {
            return TicketCategory.BILLING;
        }
        if (containsAny(text, "feature", "enhancement", "improve")) {
            return TicketCategory.FEATURE_REQUEST;
        }
        if (containsAny(text, "bug", "crash", "exception", "stacktrace")) {
            return TicketCategory.BUG_REPORT;
        }
        if (containsAny(text, "error", "issue", "broken", "failed", "failure", "not working")) {
            return TicketCategory.TECHNICAL_ISSUE;
        }
        return TicketCategory.GENERAL_SUPPORT;
    }

    private TicketPriority detectPriority(String text) {
        if (containsAny(text, "urgent", "production down", "prod down", "outage", "critical")) {
            return TicketPriority.URGENT;
        }
        if (containsAny(text, "blocked", "cannot access", "can't access", "high priority", "asap")) {
            return TicketPriority.HIGH;
        }
        if (containsAny(text, "minor", "low priority", "whenever")) {
            return TicketPriority.LOW;
        }
        return TicketPriority.MEDIUM;
    }

    private String suggestedTeamName(TicketCategory category) {
        return switch (category) {
            case ACCOUNT_ACCESS -> "Account Management";
            case BILLING -> "Billing";
            case BUG_REPORT, TECHNICAL_ISSUE -> "Platform";
            case FEATURE_REQUEST, GENERAL_SUPPORT -> "Support";
        };
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String buildSummary(Ticket ticket) {
        String summary = (ticket.getTitle() + " - " + ticket.getDescription()).replaceAll("\\s+", " ").trim();
        return summary.length() > 180 ? summary.substring(0, 177) + "..." : summary;
    }

    private String buildDraftReply(TicketCategory category) {
        return switch (category) {
            case ACCOUNT_ACCESS -> "Thanks for reporting this. We are reviewing your account access issue and will help you restore access as quickly as possible.";
            case BILLING -> "Thanks for reaching out. We are checking the billing details you mentioned and will follow up with the next steps shortly.";
            case BUG_REPORT, TECHNICAL_ISSUE -> "Thank you for the report. We are investigating the technical issue and will update you once we have more information.";
            case FEATURE_REQUEST -> "Thank you for the suggestion. We have recorded your feature request and shared it with the relevant team for review.";
            case GENERAL_SUPPORT -> "Thanks for contacting support. We are reviewing your request and will get back to you shortly.";
        };
    }

    private Double confidenceFor(TicketCategory category, TicketPriority priority) {
        double base = switch (category) {
            case ACCOUNT_ACCESS, BILLING -> 0.93;
            case BUG_REPORT, TECHNICAL_ISSUE -> 0.89;
            case FEATURE_REQUEST -> 0.87;
            case GENERAL_SUPPORT -> 0.82;
        };

        if (priority == TicketPriority.URGENT) {
            base -= 0.03;
        }

        return base;
    }

    private String probableCauseFor(TicketCategory category) {
        return switch (category) {
            case ACCOUNT_ACCESS -> "Possible credential mismatch, expired session, account lockout, or identity verification issue.";
            case BILLING -> "Possible duplicate charge, invoice mismatch, subscription sync issue, or payment processor reconciliation delay.";
            case BUG_REPORT -> "Possible reproducible application defect triggered by a recent release or edge-case workflow.";
            case TECHNICAL_ISSUE -> "Possible environment issue, access dependency failure, service degradation, or configuration mismatch.";
            case FEATURE_REQUEST -> "No defect indicated. Request appears to be a product capability gap or enhancement need.";
            case GENERAL_SUPPORT -> "Likely a general workflow or account assistance request requiring initial triage.";
        };
    }

    private String recommendedActionsFor(TicketCategory category, TicketPriority priority) {
        String urgency = priority == TicketPriority.HIGH || priority == TicketPriority.URGENT
                ? "Treat as time-sensitive.\n"
                : "";

        return switch (category) {
            case ACCOUNT_ACCESS -> urgency + """
1. Confirm the user's identity and affected account.
2. Check account lockout, password reset, and MFA status.
3. Verify whether the issue is isolated or affects multiple users.
4. Confirm whether VPN, SSO, or directory sync is involved.
""";
            case BILLING -> urgency + """
1. Review invoice history and payment transaction logs.
2. Check for duplicate charges, failed reversals, or subscription overlap.
3. Confirm the exact billing period and impacted amount.
4. Coordinate with billing operations before promising a refund outcome.
""";
            case BUG_REPORT, TECHNICAL_ISSUE -> urgency + """
1. Reproduce the issue with the same steps, environment, and permissions.
2. Check service health, logs, and recent deployments or config changes.
3. Identify scope: one user, one tenant, or broader impact.
4. Capture screenshots, timestamps, and error details for engineering if needed.
""";
            case FEATURE_REQUEST -> """
1. Clarify the business need and expected outcome.
2. Confirm whether a current workaround already exists.
3. Record impact, frequency, and user group affected.
4. Route to product or platform review with supporting context.
""";
            case GENERAL_SUPPORT -> """
1. Clarify the request and expected outcome.
2. Check whether the issue maps to an existing known workflow.
3. Route to the correct queue if ownership is obvious.
4. Ask for missing evidence before escalating.
""";
        };
    }

    private String escalationSuggestionFor(TicketCategory category, TicketPriority priority) {
        if (priority == TicketPriority.URGENT) {
            return "Escalate immediately to the owning team and monitor until acknowledgement.";
        }

        return switch (category) {
            case ACCOUNT_ACCESS -> "Escalate if identity verification passes but access still fails after reset, unlock, or SSO checks.";
            case BILLING -> "Escalate if duplicate charges are confirmed, refund approval is required, or multiple accounts show the same pattern.";
            case BUG_REPORT, TECHNICAL_ISSUE -> "Escalate to Platform/Engineering when reproducible, multi-user, or tied to a recent deployment.";
            case FEATURE_REQUEST -> "Escalate only after capturing business justification and confirming there is no supported workaround.";
            case GENERAL_SUPPORT -> "Escalate if ownership is unclear after triage or if the request impacts multiple users.";
        };
    }
}
