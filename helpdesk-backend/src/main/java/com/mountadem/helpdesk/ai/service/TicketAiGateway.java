package com.mountadem.helpdesk.ai.service;

import com.mountadem.helpdesk.ai.dto.TicketAiAnalysis;
import com.mountadem.helpdesk.ticket.entity.Ticket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TicketAiGateway {

    private final RuleBasedTicketAiService ruleBasedTicketAiService;
    private final OpenAiTicketAiService openAiTicketAiService;
    private final OllamaTicketAiService ollamaTicketAiService;

    @Value("${helpdesk.ai.provider:rule}")
    private String provider;

    public TicketAiGateway(
            RuleBasedTicketAiService ruleBasedTicketAiService,
            OpenAiTicketAiService openAiTicketAiService,
            OllamaTicketAiService ollamaTicketAiService
    ) {
        this.ruleBasedTicketAiService = ruleBasedTicketAiService;
        this.openAiTicketAiService = openAiTicketAiService;
        this.ollamaTicketAiService = ollamaTicketAiService;
    }

    public TicketAiAnalysis analyze(Ticket ticket) {
        if ("openai".equalsIgnoreCase(provider) && openAiTicketAiService.isConfigured()) {
            try {
                return openAiTicketAiService.analyze(ticket);
            } catch (Exception ex) {
                ex.printStackTrace();
                TicketAiAnalysis fallback = ruleBasedTicketAiService.analyze(ticket);
                return new TicketAiAnalysis(
                        fallback.predictedCategory(),
                        fallback.predictedPriority(),
                        fallback.suggestedTeamName(),
                        fallback.summary(),
                        fallback.draftReply(),
                        fallback.confidenceScore(),
                        fallback.probableCause(),
                        fallback.recommendedActions(),
                        fallback.escalationSuggestion(),
                        "RULE_FALLBACK"
                );
            }
        }

        if ("ollama".equalsIgnoreCase(provider) && ollamaTicketAiService.isConfigured()) {
            try {
                return ollamaTicketAiService.analyze(ticket);
            } catch (Exception ex) {
                ex.printStackTrace();
                TicketAiAnalysis fallback = ruleBasedTicketAiService.analyze(ticket);
                return new TicketAiAnalysis(
                        fallback.predictedCategory(),
                        fallback.predictedPriority(),
                        fallback.suggestedTeamName(),
                        fallback.summary(),
                        fallback.draftReply(),
                        fallback.confidenceScore(),
                        fallback.probableCause(),
                        fallback.recommendedActions(),
                        fallback.escalationSuggestion(),
                        "RULE_FALLBACK"
                );
            }
        }

        return ruleBasedTicketAiService.analyze(ticket);
    }
}
