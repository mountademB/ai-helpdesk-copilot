package com.mountadem.helpdesk.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mountadem.helpdesk.ai.dto.TicketAiAnalysis;
import com.mountadem.helpdesk.ticket.entity.Ticket;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OllamaTicketAiService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${helpdesk.ai.ollama.base-url:http://localhost:11434/api}")
    private String baseUrl;

    @Value("${helpdesk.ai.ollama.model:qwen2.5:7b-instruct}")
    private String model;

    public boolean isConfigured() {
        return hasText(baseUrl) && hasText(model);
    }

    public TicketAiAnalysis analyze(Ticket ticket) {
        if (!isConfigured()) {
            throw new IllegalStateException("Ollama is not configured.");
        }

        String systemPrompt = """
You are an enterprise helpdesk triage assistant.

Return only valid JSON matching this schema exactly.

Allowed values:
- predictedCategory: ACCOUNT_ACCESS, BILLING, TECHNICAL_ISSUE, BUG_REPORT, FEATURE_REQUEST, GENERAL_SUPPORT
- predictedPriority: LOW, MEDIUM, HIGH, URGENT
- suggestedTeamName: Support, Billing, Platform, Account Management

Rules:
- summary must be concise and factual
- draftReply must be short, professional, and customer-facing
- probableCause must be an internal agent-facing explanation
- recommendedActions must be internal agent-facing steps
- escalationSuggestion must say when or whether escalation is needed
- confidenceScore must be a number between 0.0 and 1.0
""";

        String userPrompt = """
Ticket Title:
%s

Ticket Description:
%s
""".formatted(ticket.getTitle(), ticket.getDescription());

        String requestBody = """
{
  "model": %s,
  "messages": [
    {
      "role": "system",
      "content": %s
    },
    {
      "role": "user",
      "content": %s
    }
  ],
  "stream": false,
  "format": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "predictedCategory": {
        "type": "string",
        "enum": ["ACCOUNT_ACCESS", "BILLING", "TECHNICAL_ISSUE", "BUG_REPORT", "FEATURE_REQUEST", "GENERAL_SUPPORT"]
      },
      "predictedPriority": {
        "type": "string",
        "enum": ["LOW", "MEDIUM", "HIGH", "URGENT"]
      },
      "suggestedTeamName": {
        "type": "string",
        "enum": ["Support", "Billing", "Platform", "Account Management"]
      },
      "summary": {
        "type": "string"
      },
      "draftReply": {
        "type": "string"
      },
      "confidenceScore": {
        "type": "number"
      },
      "probableCause": {
        "type": "string"
      },
      "recommendedActions": {
        "type": "string"
      },
      "escalationSuggestion": {
        "type": "string"
      }
    },
    "required": [
      "predictedCategory",
      "predictedPriority",
      "suggestedTeamName",
      "summary",
      "draftReply",
      "confidenceScore",
      "probableCause",
      "recommendedActions",
      "escalationSuggestion"
    ]
  }
}
""".formatted(
                jsonString(model),
                jsonString(systemPrompt),
                jsonString(userPrompt)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Ollama call failed: " + response.statusCode() + " - " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("message").path("content").asText();

            if (!hasText(content)) {
                throw new IllegalStateException("Ollama returned empty content.");
            }

            JsonNode result = objectMapper.readTree(content);

            return new TicketAiAnalysis(
                    result.path("predictedCategory").asText("GENERAL_SUPPORT"),
                    result.path("predictedPriority").asText("MEDIUM"),
                    result.path("suggestedTeamName").asText("Support"),
                    result.path("summary").asText("Support request received."),
                    result.path("draftReply").asText("Thanks for contacting support. We are reviewing your request and will get back to you shortly."),
                    result.path("confidenceScore").asDouble(0.80),
                    result.path("probableCause").asText("No probable cause suggested."),
                    result.path("recommendedActions").asText("No internal actions suggested."),
                    result.path("escalationSuggestion").asText("No escalation advice suggested."),
                    "OLLAMA"
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Ollama request failed.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ollama request interrupted.", ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String jsonString(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to JSON-escape string.", ex);
        }
    }
}
