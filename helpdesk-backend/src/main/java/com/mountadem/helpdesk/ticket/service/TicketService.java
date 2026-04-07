package com.mountadem.helpdesk.ticket.service;

import com.mountadem.helpdesk.ai.dto.AIRecommendationResponse;
import com.mountadem.helpdesk.ai.dto.ReviewAIRecommendationRequest;
import com.mountadem.helpdesk.ai.dto.TicketAiAnalysis;
import com.mountadem.helpdesk.ai.entity.AIRecommendation;
import com.mountadem.helpdesk.ai.repository.AIRecommendationRepository;
import com.mountadem.helpdesk.ai.service.TicketAiGateway;
import com.mountadem.helpdesk.comment.dto.AddCommentRequest;
import com.mountadem.helpdesk.comment.dto.CommentResponse;
import com.mountadem.helpdesk.comment.entity.TicketComment;
import com.mountadem.helpdesk.comment.repository.TicketCommentRepository;
import com.mountadem.helpdesk.common.enums.AIReviewStatus;
import com.mountadem.helpdesk.common.enums.ActorType;
import com.mountadem.helpdesk.common.enums.Role;
import com.mountadem.helpdesk.common.enums.TicketCategory;
import com.mountadem.helpdesk.common.enums.TicketEventType;
import com.mountadem.helpdesk.common.enums.TicketPriority;
import com.mountadem.helpdesk.common.enums.TicketStatus;
import com.mountadem.helpdesk.dashboard.dto.DashboardSummaryResponse;
import com.mountadem.helpdesk.dashboard.dto.RecentActivityResponse;
import com.mountadem.helpdesk.event.dto.EventResponse;
import com.mountadem.helpdesk.feedback.service.FeedbackCaptureService;
import com.mountadem.helpdesk.knowledge.dto.KnowledgeGuidanceResponse;
import com.mountadem.helpdesk.knowledge.service.KnowledgeBaseService;
import com.mountadem.helpdesk.event.entity.TicketEvent;
import com.mountadem.helpdesk.event.repository.TicketEventRepository;
import com.mountadem.helpdesk.reference.dto.TeamListItemResponse;
import com.mountadem.helpdesk.reference.dto.UserListItemResponse;
import com.mountadem.helpdesk.team.entity.Team;
import com.mountadem.helpdesk.team.repository.TeamRepository;
import com.mountadem.helpdesk.ticket.dto.AssignTicketRequest;
import com.mountadem.helpdesk.ticket.dto.CreateTicketRequest;
import com.mountadem.helpdesk.ticket.dto.SimilarTicketResponse;
import com.mountadem.helpdesk.ticket.dto.TicketResponse;
import com.mountadem.helpdesk.ticket.dto.UpdateTicketStatusRequest;
import com.mountadem.helpdesk.ticket.entity.Ticket;
import com.mountadem.helpdesk.ticket.repository.TicketRepository;
import com.mountadem.helpdesk.user.entity.User;
import com.mountadem.helpdesk.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketEventRepository ticketEventRepository;
    private final AIRecommendationRepository aiRecommendationRepository;
    private final TicketAiGateway ticketAiGateway;
    private final KnowledgeBaseService knowledgeBaseService;
    private final FeedbackCaptureService feedbackCaptureService;

    public TicketResponse create(CreateTicketRequest request) {
        if (request == null || request.createdById() == null || !hasText(request.title()) || !hasText(request.description())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "createdById, title and description are required.");
        }

        User createdBy = getUser(request.createdById());

        Ticket ticket = new Ticket();
        ticket.setReferenceCode(generateReferenceCode());
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setCreatedBy(createdBy);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setCategory(TicketCategory.GENERAL_SUPPORT);

        Ticket saved = ticketRepository.save(ticket);

        logEvent(saved, TicketEventType.TICKET_CREATED, actorTypeFromRole(createdBy.getRole()), createdBy.getId(), "Ticket created");

        return toTicketResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets() {
        return ticketRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toTicketResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketResponse(Long ticketId) {
        return toTicketResponse(getTicket(ticketId));
    }

    @Transactional(readOnly = true)
    public List<SimilarTicketResponse> listSimilarTickets(Long ticketId) {
        Ticket source = getTicket(ticketId);
        AIRecommendation latestAi = aiRecommendationRepository.findFirstByTicketIdOrderByCreatedAtDesc(ticketId).orElse(null);

        TicketCategory referenceCategory = latestAi != null ? latestAi.getPredictedCategory() : source.getCategory();
        Team referenceTeam = latestAi != null && latestAi.getSuggestedTeam() != null ? latestAi.getSuggestedTeam() : source.getTeam();
        List<String> referenceKeywords = extractKeywords(source.getTitle() + " " + source.getDescription());

        return ticketRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(ticket -> !ticket.getId().equals(ticketId))
                .map(ticket -> buildSimilarTicketScore(ticket, referenceCategory, referenceTeam, referenceKeywords))
                .filter(result -> result.score() > 0)
                .sorted(Comparator.comparingInt(SimilarTicketScore::score).reversed()
                        .thenComparing(result -> result.response().createdAt(), Comparator.reverseOrder()))
                .limit(5)
                .map(SimilarTicketScore::response)
                .toList();
    }

    public TicketResponse updateStatus(Long ticketId, UpdateTicketStatusRequest request) {
        if (request == null || !hasText(request.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required.");
        }

        Ticket ticket = getTicket(ticketId);
        TicketStatus newStatus;

        try {
            newStatus = TicketStatus.valueOf(request.status().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status.");
        }

        ticket.setStatus(newStatus);
        Ticket saved = ticketRepository.save(ticket);

        logEvent(saved, TicketEventType.STATUS_CHANGED, ActorType.SYSTEM, null, "Status changed to " + newStatus.name());

        return toTicketResponse(saved);
    }

    public TicketResponse assign(Long ticketId, AssignTicketRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }

        Ticket ticket = getTicket(ticketId);

        User assignedTo = null;
        if (request.assignedToId() != null) {
            assignedTo = getUser(request.assignedToId());
            if (assignedTo.getRole() == Role.USER) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assignedToId must belong to an AGENT or ADMIN.");
            }
        }

        Team team = null;
        if (request.teamId() != null) {
            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found."));
        } else if (assignedTo != null) {
            team = assignedTo.getTeam();
        }

        ticket.setAssignedTo(assignedTo);
        ticket.setTeam(team);

        Ticket saved = ticketRepository.save(ticket);

        String details = "Assigned to " + (assignedTo != null ? assignedTo.getFullName() : "nobody")
                + (team != null ? " in team " + team.getName() : "");

        logEvent(saved, TicketEventType.ASSIGNED, ActorType.SYSTEM, null, details);

        return toTicketResponse(saved);
    }

    public CommentResponse addComment(Long ticketId, AddCommentRequest request) {
        if (request == null || request.authorId() == null || !hasText(request.content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authorId and content are required.");
        }

        Ticket ticket = getTicket(ticketId);
        User author = getUser(request.authorId());

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(request.content().trim());
        comment.setInternalNote(Boolean.TRUE.equals(request.internalNote()));

        TicketComment saved = ticketCommentRepository.save(comment);

        logEvent(
                ticket,
                saved.isInternalNote() ? TicketEventType.INTERNAL_NOTE_ADDED : TicketEventType.COMMENT_ADDED,
                actorTypeFromRole(author.getRole()),
                author.getId(),
                saved.isInternalNote() ? "Internal note added" : "Comment added"
        );

        return toCommentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(Long ticketId) {
        ensureTicketExists(ticketId);

        return ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toCommentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listEvents(Long ticketId) {
        ensureTicketExists(ticketId);

        return ticketEventRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toEventResponse)
                .toList();
    }

    public AIRecommendationResponse analyze(Long ticketId) {
        Ticket ticket = getTicket(ticketId);

        TicketAiAnalysis analysis = ticketAiGateway.analyze(ticket);

        TicketCategory category = TicketCategory.valueOf(analysis.predictedCategory().trim().toUpperCase());
        TicketPriority priority = TicketPriority.valueOf(analysis.predictedPriority().trim().toUpperCase());
        Team suggestedTeam = teamRepository.findByName(analysis.suggestedTeamName()).orElse(null);

        AIRecommendation recommendation = new AIRecommendation();
        recommendation.setTicket(ticket);
        recommendation.setPredictedCategory(category);
        recommendation.setPredictedPriority(priority);
        recommendation.setSuggestedTeam(suggestedTeam);
        recommendation.setSummary(analysis.summary());
        recommendation.setDraftReply(analysis.draftReply());
        recommendation.setConfidenceScore(analysis.confidenceScore());
        recommendation.setProbableCause(analysis.probableCause());
        recommendation.setRecommendedActions(analysis.recommendedActions());
        recommendation.setEscalationSuggestion(analysis.escalationSuggestion());
        recommendation.setAnalysisSource(analysis.analysisSource());
        recommendation.setReviewStatus(AIReviewStatus.PENDING);

        AIRecommendation saved = aiRecommendationRepository.save(recommendation);

        ticket.setAiAnalyzed(true);
        ticketRepository.save(ticket);

        logEvent(ticket, TicketEventType.AI_ANALYSIS_GENERATED, ActorType.AI, null, "AI analysis generated");

        return toAIRecommendationResponse(saved);
    }

    @Transactional(readOnly = true)
    public AIRecommendationResponse getLatestRecommendation(Long ticketId) {
        ensureTicketExists(ticketId);

        AIRecommendation recommendation = aiRecommendationRepository.findFirstByTicketIdOrderByCreatedAtDesc(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No AI recommendation found for this ticket."));

        return toAIRecommendationResponse(recommendation);
    }

    public AIRecommendationResponse reviewLatestRecommendation(Long ticketId, ReviewAIRecommendationRequest request) {
        if (request == null || request.reviewedById() == null || !hasText(request.action())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reviewedById and action are required.");
        }

        Ticket ticket = getTicket(ticketId);
        User reviewer = getUser(request.reviewedById());

        AIRecommendation recommendation = aiRecommendationRepository.findFirstByTicketIdOrderByCreatedAtDesc(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No AI recommendation found for this ticket."));

        String action = request.action().trim().toUpperCase();

        recommendation.setReviewedBy(reviewer);
        recommendation.setReviewedAt(Instant.now());

        if (action.startsWith("ACCEPT")) {
            recommendation.setReviewStatus(AIReviewStatus.ACCEPTED);
            ticket.setCategory(recommendation.getPredictedCategory());
            ticket.setPriority(recommendation.getPredictedPriority());
            if (recommendation.getSuggestedTeam() != null) {
                ticket.setTeam(recommendation.getSuggestedTeam());
            }
            ticketRepository.save(ticket);
            logEvent(ticket, TicketEventType.AI_SUGGESTION_ACCEPTED, actorTypeFromRole(reviewer.getRole()), reviewer.getId(), "AI suggestion accepted");
        } else if (action.startsWith("REJECT")) {
            recommendation.setReviewStatus(AIReviewStatus.REJECTED);
            logEvent(ticket, TicketEventType.AI_SUGGESTION_REJECTED, actorTypeFromRole(reviewer.getRole()), reviewer.getId(), "AI suggestion rejected");
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action must be ACCEPT or REJECT.");
        }

        AIRecommendation saved = aiRecommendationRepository.save(recommendation);
        feedbackCaptureService.recordReview(ticket, saved, saved.getReviewStatus().name(), reviewer.getId());
        return toAIRecommendationResponse(saved);
    }


    @Transactional(readOnly = true)
    public KnowledgeGuidanceResponse getKnowledgeGuidance(Long ticketId) {
        Ticket ticket = getTicket(ticketId);
        AIRecommendation latestRecommendation = aiRecommendationRepository.findFirstByTicketIdOrderByCreatedAtDesc(ticketId).orElse(null);
        return knowledgeBaseService.buildGuidance(ticket, latestRecommendation);
    }

    @Transactional(readOnly = true)
    public List<RecentActivityResponse> listRecentActivity() {
        return ticketEventRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(event -> new RecentActivityResponse(
                        event.getTicket().getId(),
                        event.getTicket().getReferenceCode(),
                        event.getTicket().getTitle(),
                        event.getType().name(),
                        event.getActorType().name(),
                        event.getDetails(),
                        event.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse dashboardSummary() {
        return new DashboardSummaryResponse(
                ticketRepository.count(),
                ticketRepository.countByStatus(TicketStatus.OPEN),
                ticketRepository.countByStatus(TicketStatus.IN_PROGRESS),
                ticketRepository.countByStatus(TicketStatus.WAITING_FOR_CUSTOMER),
                ticketRepository.countByStatus(TicketStatus.RESOLVED),
                ticketRepository.countByStatus(TicketStatus.CLOSED),
                ticketRepository.countByAiAnalyzed(true)
        );
    }

    @Transactional(readOnly = true)
    public List<UserListItemResponse> listUsers() {
        return userRepository.findAll(Sort.by("id"))
                .stream()
                .map(user -> new UserListItemResponse(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getTeam() != null ? user.getTeam().getId() : null,
                        user.getTeam() != null ? user.getTeam().getName() : null,
                        user.isActive()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamListItemResponse> listTeams() {
        return teamRepository.findAll(Sort.by("id"))
                .stream()
                .map(team -> new TeamListItemResponse(team.getId(), team.getName(), team.getDescription()))
                .toList();
    }

    private Ticket getTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found."));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private void ensureTicketExists(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found.");
        }
    }

    private void logEvent(Ticket ticket, TicketEventType type, ActorType actorType, Long actorId, String details) {
        TicketEvent event = new TicketEvent();
        event.setTicket(ticket);
        event.setType(type);
        event.setActorType(actorType);
        event.setActorId(actorId);
        event.setDetails(details);
        ticketEventRepository.save(event);
    }

    private ActorType actorTypeFromRole(Role role) {
        return switch (role) {
            case USER -> ActorType.USER;
            case AGENT -> ActorType.AGENT;
            case ADMIN -> ActorType.ADMIN;
        };
    }

    private String generateReferenceCode() {
        return "TICK-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private SimilarTicketScore buildSimilarTicketScore(
            Ticket ticket,
            TicketCategory referenceCategory,
            Team referenceTeam,
            List<String> referenceKeywords
    ) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (referenceCategory != null && ticket.getCategory() == referenceCategory) {
            score += referenceCategory == TicketCategory.GENERAL_SUPPORT ? 2 : 5;
            reasons.add("Same category");
        }

        if (referenceTeam != null && ticket.getTeam() != null && ticket.getTeam().getId().equals(referenceTeam.getId())) {
            score += 3;
            reasons.add("Same team");
        }

        List<String> candidateKeywords = extractKeywords(ticket.getTitle() + " " + ticket.getDescription());
        List<String> overlap = referenceKeywords.stream()
                .filter(candidateKeywords::contains)
                .distinct()
                .limit(3)
                .toList();

        if (!overlap.isEmpty()) {
            score += Math.min(4, overlap.size() + 1);
            reasons.add("Shared terms: " + String.join(", ", overlap));
        }

        SimilarTicketResponse response = new SimilarTicketResponse(
                ticket.getId(),
                ticket.getReferenceCode(),
                ticket.getTitle(),
                ticket.getStatus().name(),
                ticket.getPriority().name(),
                ticket.getCategory().name(),
                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFullName() : null,
                ticket.getTeam() != null ? ticket.getTeam().getName() : null,
                reasons.isEmpty() ? "Potentially related" : String.join(" | ", reasons),
                score,
                ticket.getCreatedAt()
        );

        return new SimilarTicketScore(score, response);
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Set<String> stopWords = Set.of(
                "the", "and", "for", "with", "from", "that", "this", "have", "keep", "after", "into", "your",
                "you", "are", "was", "were", "will", "been", "them", "they", "their", "our", "can", "cannot",
                "cant", "not", "but", "too", "get", "got", "had", "has", "did", "does", "done", "using",
                "user", "ticket", "issue", "need", "help", "please", "still", "more", "than", "when", "then",
                "sign", "reset"
        );

        return new LinkedHashSet<>(
                List.of(text.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9 ]", " ")
                        .split("\\s+"))
        ).stream()
                .filter(token -> token.length() >= 4)
                .filter(token -> !stopWords.contains(token))
                .limit(12)
                .collect(Collectors.toList());
    }

    private TicketResponse toTicketResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getReferenceCode(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus().name(),
                ticket.getPriority().name(),
                ticket.getCategory().name(),
                ticket.getCreatedBy().getId(),
                ticket.getCreatedBy().getFullName(),
                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null,
                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFullName() : null,
                ticket.getTeam() != null ? ticket.getTeam().getId() : null,
                ticket.getTeam() != null ? ticket.getTeam().getName() : null,
                ticket.isAiAnalyzed(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    private CommentResponse toCommentResponse(TicketComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getFullName(),
                comment.getContent(),
                comment.isInternalNote(),
                comment.getCreatedAt()
        );
    }

    private EventResponse toEventResponse(TicketEvent event) {
        return new EventResponse(
                event.getId(),
                event.getType().name(),
                event.getActorType().name(),
                event.getActorId(),
                event.getDetails(),
                event.getCreatedAt()
        );
    }

    private AIRecommendationResponse toAIRecommendationResponse(AIRecommendation recommendation) {
        return new AIRecommendationResponse(
                recommendation.getId(),
                recommendation.getPredictedCategory().name(),
                recommendation.getPredictedPriority().name(),
                recommendation.getSuggestedTeam() != null ? recommendation.getSuggestedTeam().getId() : null,
                recommendation.getSuggestedTeam() != null ? recommendation.getSuggestedTeam().getName() : null,
                recommendation.getSummary(),
                recommendation.getDraftReply(),
                recommendation.getConfidenceScore(),
                recommendation.getProbableCause(),
                recommendation.getRecommendedActions(),
                recommendation.getEscalationSuggestion(),
                recommendation.getAnalysisSource(),
                recommendation.getReviewStatus().name(),
                recommendation.getReviewedBy() != null ? recommendation.getReviewedBy().getId() : null,
                recommendation.getReviewedAt(),
                recommendation.getCreatedAt()
        );
    }

    private record SimilarTicketScore(int score, SimilarTicketResponse response) {
    }
}





