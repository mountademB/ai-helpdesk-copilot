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
public class OpenAiTicketAiService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${helpdesk.ai.openai.api-key:}")
    private String apiKey;

    @Value("${helpdesk.ai.openai.model:}")
    private String model;

    public boolean isConfigured() {
        return hasText(apiKey) && hasText(model);
    }

    public TicketAiAnalysis analyze(Ticket ticket) {
        if (!isConfigured()) {
            throw new IllegalStateException("OpenAI is not configured.");
        }

        String developerPrompt = """
You are an enterprise helpdesk triage assistant.

Return only the fields required by the schema.

Rules:
- predictedCategory must be one of:
  ACCOUNT_ACCESS, BILLING, TECHNICAL_ISSUE, BUG_REPORT, FEATURE_REQUEST, GENERAL_SUPPORT
- predictedPriority must be one of:
  LOW, MEDIUM, HIGH, URGENT
- suggestedTeamName must be one of:
  Support, Billing, Platform, Account Management
- summary must be concise and factual
- draftReply must be professional, short, and customer-facing
- probableCause must be an internal agent-facing explanation
- recommendedActions must be internal agent-facing steps, concise but practical
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
      "role": "developer",
      "content": %s
    },
    {
      "role": "user",
      "content": %s
    }
  ],
  "temperature": 0.2,
  "response_format": {
    "type": "json_schema",
    "json_schema": {
      "name": "ticket_analysis",
      "strict": true,
      "schema": {
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
  }
}
""".formatted(
                jsonString(model),
                jsonString(developerPrompt),
                jsonString(userPrompt)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI call failed: " + response.statusCode() + " - " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();

            if (!hasText(content)) {
                throw new IllegalStateException("OpenAI returned empty content.");
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
                    "OPENAI"
            );
        } catch (IOException ex) {
            throw new IllegalStateException("OpenAI request failed.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI request interrupted.", ex);
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
