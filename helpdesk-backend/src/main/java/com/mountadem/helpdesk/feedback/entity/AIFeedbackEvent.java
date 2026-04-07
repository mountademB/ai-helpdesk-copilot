package com.mountadem.helpdesk.feedback.entity;

import com.mountadem.helpdesk.ai.entity.AIRecommendation;
import com.mountadem.helpdesk.common.entity.BaseEntity;
import com.mountadem.helpdesk.ticket.entity.Ticket;
import jakarta.persistence.*;

@Entity
@Table(name = "ai_feedback_events")
public class AIFeedbackEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id")
    private AIRecommendation recommendation;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 50)
    private String eventValue;

    @Column(length = 50)
    private String rewriteMode;

    @Column(length = 30)
    private String source;

    private Long actorId;

    @Column(columnDefinition = "TEXT")
    private String note;

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public AIRecommendation getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(AIRecommendation recommendation) {
        this.recommendation = recommendation;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventValue() {
        return eventValue;
    }

    public void setEventValue(String eventValue) {
        this.eventValue = eventValue;
    }

    public String getRewriteMode() {
        return rewriteMode;
    }

    public void setRewriteMode(String rewriteMode) {
        this.rewriteMode = rewriteMode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
