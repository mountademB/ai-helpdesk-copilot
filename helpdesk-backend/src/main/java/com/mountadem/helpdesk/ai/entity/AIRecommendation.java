package com.mountadem.helpdesk.ai.entity;

import com.mountadem.helpdesk.common.entity.BaseEntity;
import com.mountadem.helpdesk.common.enums.AIReviewStatus;
import com.mountadem.helpdesk.common.enums.TicketCategory;
import com.mountadem.helpdesk.common.enums.TicketPriority;
import com.mountadem.helpdesk.team.entity.Team;
import com.mountadem.helpdesk.ticket.entity.Ticket;
import com.mountadem.helpdesk.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ai_recommendations")
public class AIRecommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TicketCategory predictedCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority predictedPriority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_team_id")
    private Team suggestedTeam;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String draftReply;

    @Column(nullable = false)
    private Double confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String probableCause;

    @Column(columnDefinition = "TEXT")
    private String recommendedActions;

    @Column(columnDefinition = "TEXT")
    private String escalationSuggestion;

    @Column(name = "analysis_source", length = 30)
    private String analysisSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AIReviewStatus reviewStatus = AIReviewStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    private Instant reviewedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public TicketCategory getPredictedCategory() {
        return predictedCategory;
    }

    public void setPredictedCategory(TicketCategory predictedCategory) {
        this.predictedCategory = predictedCategory;
    }

    public TicketPriority getPredictedPriority() {
        return predictedPriority;
    }

    public void setPredictedPriority(TicketPriority predictedPriority) {
        this.predictedPriority = predictedPriority;
    }

    public Team getSuggestedTeam() {
        return suggestedTeam;
    }

    public void setSuggestedTeam(Team suggestedTeam) {
        this.suggestedTeam = suggestedTeam;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDraftReply() {
        return draftReply;
    }

    public void setDraftReply(String draftReply) {
        this.draftReply = draftReply;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getProbableCause() {
        return probableCause;
    }

    public void setProbableCause(String probableCause) {
        this.probableCause = probableCause;
    }

    public String getRecommendedActions() {
        return recommendedActions;
    }

    public void setRecommendedActions(String recommendedActions) {
        this.recommendedActions = recommendedActions;
    }

    public String getEscalationSuggestion() {
        return escalationSuggestion;
    }

    public void setEscalationSuggestion(String escalationSuggestion) {
        this.escalationSuggestion = escalationSuggestion;
    }

    public String getAnalysisSource() {
        return analysisSource;
    }

    public void setAnalysisSource(String analysisSource) {
        this.analysisSource = analysisSource;
    }

    public AIReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(AIReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
