package com.mountadem.helpdesk.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mountadem.helpdesk.ai.dto.RewriteDraftReplyRequest;
import com.mountadem.helpdesk.ai.dto.RewriteDraftReplyResponse;
import com.mountadem.helpdesk.ai.entity.AIRecommendation;
import com.mountadem.helpdesk.ai.repository.AIRecommendationRepository;
import com.mountadem.helpdesk.feedback.service.FeedbackCaptureService;
import com.mountadem.helpdesk.ticket.entity.Ticket;
import com.mountadem.helpdesk.ticket.repository.TicketRepository;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AgentRewriteService {

    private final TicketRepository ticketRepository;
    private final AIRecommendationRepository aiRecommendationRepository;
    private final FeedbackCaptureService feedbackCaptureService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${helpdesk.ai.provider:rule}")
    private String provider;

    @Value("${helpdesk.ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${helpdesk.ai.openai.model:}")
    private String openAiModel;

    @Value("${helpdesk.ai.ollama.base-url:http://localhost:11434/api}")
    private String ollamaBaseUrl;

    @Value("${helpdesk.ai.ollama.model:qwen2.5:7b-instruct}")
    private String ollamaModel;

    public AgentRewriteService(
            TicketRepository ticketRepository,
            AIRecommendationRepository aiRecommendationRepository,
            FeedbackCaptureService feedbackCaptureService
    ) {
        this.ticketRepository = ticketRepository;
        this.aiRecommendationRepository = aiRecommendationRepository;
        this.feedbackCaptureService = feedbackCaptureService;
    }

    public RewriteDraftReplyResponse rewrite(Long ticketId, RewriteDraftReplyRequest request) {
        if (request == null || !hasText(request.mode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode is required.");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found."));

        AIRecommendation latestRecommendation = aiRecommendationRepository.findFirstByTicketIdOrderByCreatedAtDesc(ticketId).orElse(null);

        String mode = request.mode().trim().toUpperCase(Locale.ROOT);
        validateMode(mode);

        String baseText = resolveBaseText(latestRecommendation, request.text());

        RewriteDraftReplyResponse response;

        try {
            if ("OPENAI".equalsIgnoreCase(provider) && hasText(openAiApiKey) && hasText(openAiModel)) {
                response = rewriteWithOpenAi(ticket, baseText, mode);
            } else if ("OLLAMA".equalsIgnoreCase(provider) && hasText(ollamaBaseUrl) && hasText(ollamaModel)) {
                response = rewriteWithOllama(ticket, baseText, mode);
            } else {
                response = new RewriteDraftReplyResponse(mode, rewriteRuleBased(baseText, mode), "RULE");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            response = new RewriteDraftReplyResponse(mode, rewriteRuleBased(baseText, mode), "RULE");
        }

        feedbackCaptureService.recordRewrite(ticket, latestRecommendation, mode, response.source());
        return response;
    }

    private RewriteDraftReplyResponse rewriteWithOpenAi(Ticket ticket, String baseText, String mode) throws IOException, InterruptedException {
        String developerPrompt = """
You rewrite customer-facing helpdesk replies.

Return only valid JSON.
Do not add markdown.
Keep the meaning accurate to the original message and ticket context.
Do not invent technical details or promises.

Allowed modes:
- SHORTER
- MORE_FORMAL
- MORE_EMPATHETIC
- CUSTOMER_SAFE
""";

        String userPrompt = """
Ticket Title:
%s

Ticket Description:
%s

Rewrite Mode:
%s

Original Draft Reply:
%s
""".formatted(ticket.getTitle(), ticket.getDescription(), mode, baseText);

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
      "name": "rewrite_reply",
      "strict": true,
      "schema": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "rewrittenText": {
            "type": "string"
          }
        },
        "required": ["rewrittenText"]
      }
    }
  }
}
""".formatted(
                jsonString(openAiModel),
                jsonString(developerPrompt),
                jsonString(userPrompt)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI rewrite failed: " + response.statusCode() + " - " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("choices").path(0).path("message").path("content").asText();
        JsonNode result = objectMapper.readTree(content);

        return new RewriteDraftReplyResponse(
                mode,
                result.path("rewrittenText").asText(baseText),
                "OPENAI"
        );
    }

    private RewriteDraftReplyResponse rewriteWithOllama(Ticket ticket, String baseText, String mode) throws IOException, InterruptedException {
        String systemPrompt = """
You rewrite customer-facing helpdesk replies.

Return only valid JSON.
Do not add markdown.
Keep the meaning accurate to the original message and ticket context.
Do not invent technical details or promises.

Allowed modes:
- SHORTER
- MORE_FORMAL
- MORE_EMPATHETIC
- CUSTOMER_SAFE
""";

        String userPrompt = """
Ticket Title:
%s

Ticket Description:
%s

Rewrite Mode:
%s

Original Draft Reply:
%s
""".formatted(ticket.getTitle(), ticket.getDescription(), mode, baseText);

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
      "rewrittenText": {
        "type": "string"
      }
    },
    "required": ["rewrittenText"]
  }
}
""".formatted(
                jsonString(ollamaModel),
                jsonString(systemPrompt),
                jsonString(userPrompt)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaBaseUrl + "/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Ollama rewrite failed: " + response.statusCode() + " - " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("message").path("content").asText();
        JsonNode result = objectMapper.readTree(content);

        return new RewriteDraftReplyResponse(
                mode,
                result.path("rewrittenText").asText(baseText),
                "OLLAMA"
        );
    }

    private String resolveBaseText(AIRecommendation latestRecommendation, String requestText) {
        if (hasText(requestText)) {
            return requestText.trim();
        }

        if (latestRecommendation != null && hasText(latestRecommendation.getDraftReply())) {
            return latestRecommendation.getDraftReply().trim();
        }

        return "Thank you for contacting support. We are reviewing your request and will share an update as soon as possible.";
    }

    private void validateMode(String mode) {
        Set<String> allowed = Set.of("SHORTER", "MORE_FORMAL", "MORE_EMPATHETIC", "CUSTOMER_SAFE");
        if (!allowed.contains(mode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported rewrite mode.");
        }
    }

    private String rewriteRuleBased(String baseText, String mode) {
        return switch (mode) {
            case "SHORTER" -> shorter(baseText);
            case "MORE_FORMAL" -> moreFormal(baseText);
            case "MORE_EMPATHETIC" -> moreEmpathetic(baseText);
            case "CUSTOMER_SAFE" -> customerSafe(baseText);
            default -> baseText;
        };
    }

    private String shorter(String text) {
        String normalized = normalize(text);
        if (normalized.length() <= 110) {
            return normalized;
        }

        int cut = normalized.indexOf(". ");
        if (cut > 30) {
            return normalized.substring(0, cut + 1).trim();
        }

        return normalized.substring(0, 107).trim() + "...";
    }

    private String moreFormal(String text) {
        String output = normalize(text)
                .replace("Thanks", "Thank you")
                .replace("We're", "We are")
                .replace("we're", "we are")
                .replace("We'll", "We will")
                .replace("we'll", "we will")
                .replace("can't", "cannot")
                .replace("Can't", "Cannot");

        if (!output.endsWith(".")) {
            output += ".";
        }

        return output;
    }

    private String moreEmpathetic(String text) {
        String normalized = normalize(text);
        String prefix = "We understand this may be frustrating, and we appreciate your patience. ";
        if (normalized.isBlank()) {
            return prefix.trim();
        }
        if (normalized.toLowerCase(Locale.ROOT).contains("appreciate your patience")) {
            return normalized;
        }
        return prefix + Character.toLowerCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String customerSafe(String text) {
        return "Thank you for reporting this. We are reviewing the issue and will share an update as soon as possible.";
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
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
