package com.mountadem.helpdesk.config;

import com.mountadem.helpdesk.ai.entity.AIRecommendation;
import com.mountadem.helpdesk.ai.repository.AIRecommendationRepository;
import com.mountadem.helpdesk.comment.entity.TicketComment;
import com.mountadem.helpdesk.comment.repository.TicketCommentRepository;
import com.mountadem.helpdesk.common.enums.AIReviewStatus;
import com.mountadem.helpdesk.common.enums.ActorType;
import com.mountadem.helpdesk.common.enums.Role;
import com.mountadem.helpdesk.common.enums.TicketCategory;
import com.mountadem.helpdesk.common.enums.TicketEventType;
import com.mountadem.helpdesk.common.enums.TicketPriority;
import com.mountadem.helpdesk.common.enums.TicketStatus;
import com.mountadem.helpdesk.event.entity.TicketEvent;
import com.mountadem.helpdesk.event.repository.TicketEventRepository;
import com.mountadem.helpdesk.team.entity.Team;
import com.mountadem.helpdesk.team.repository.TeamRepository;
import com.mountadem.helpdesk.ticket.entity.Ticket;
import com.mountadem.helpdesk.ticket.repository.TicketRepository;
import com.mountadem.helpdesk.user.entity.User;
import com.mountadem.helpdesk.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final TicketEventRepository ticketEventRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final AIRecommendationRepository aiRecommendationRepository;

    @Override
    public void run(String... args) {
        seedTeams();
        seedUsers();
        seedTickets();
    }

    private void seedTeams() {
        if (teamRepository.count() > 0) {
            return;
        }

        teamRepository.saveAll(List.of(
                team("Support", "General support queue"),
                team("Billing", "Billing operations"),
                team("Platform", "Technical issues and bugs"),
                team("Account Management", "Account access and identity issues")
        ));
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }

        Team support = teamRepository.findByName("Support").orElseThrow();
        Team billing = teamRepository.findByName("Billing").orElseThrow();
        Team platform = teamRepository.findByName("Platform").orElseThrow();

        User admin = new User();
        admin.setFullName("Admin User");
        admin.setEmail("admin@helpdesk.local");
        admin.setPasswordHash("demo");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        User supportAgent = new User();
        supportAgent.setFullName("Sara Support");
        supportAgent.setEmail("agent.support@helpdesk.local");
        supportAgent.setPasswordHash("demo");
        supportAgent.setRole(Role.AGENT);
        supportAgent.setTeam(support);
        supportAgent.setActive(true);

        User billingAgent = new User();
        billingAgent.setFullName("Bilal Billing");
        billingAgent.setEmail("agent.billing@helpdesk.local");
        billingAgent.setPasswordHash("demo");
        billingAgent.setRole(Role.AGENT);
        billingAgent.setTeam(billing);
        billingAgent.setActive(true);

        User platformAgent = new User();
        platformAgent.setFullName("Pia Platform");
        platformAgent.setEmail("agent.platform@helpdesk.local");
        platformAgent.setPasswordHash("demo");
        platformAgent.setRole(Role.AGENT);
        platformAgent.setTeam(platform);
        platformAgent.setActive(true);

        User requester = new User();
        requester.setFullName("Youssef Requester");
        requester.setEmail("user@helpdesk.local");
        requester.setPasswordHash("demo");
        requester.setRole(Role.USER);
        requester.setActive(true);

        userRepository.saveAll(List.of(admin, supportAgent, billingAgent, platformAgent, requester));
    }

    private void seedTickets() {
        if (ticketRepository.count() > 0) {
            return;
        }

        User requester = userRepository.findByEmail("user@helpdesk.local").orElseThrow();
        User supportAgent = userRepository.findByEmail("agent.support@helpdesk.local").orElseThrow();
        User billingAgent = userRepository.findByEmail("agent.billing@helpdesk.local").orElseThrow();

        Team accountManagement = teamRepository.findByName("Account Management").orElseThrow();
        Team billing = teamRepository.findByName("Billing").orElseThrow();

        Ticket ticket1 = new Ticket();
        ticket1.setReferenceCode("TICK-SEED-0001");
        ticket1.setTitle("Unable to access my account");
        ticket1.setDescription("I cannot sign in and I keep getting invalid credentials even after resetting my password.");
        ticket1.setCreatedBy(requester);
        ticket1.setAssignedTo(supportAgent);
        ticket1.setTeam(accountManagement);
        ticket1.setStatus(TicketStatus.OPEN);
        ticket1.setPriority(TicketPriority.HIGH);
        ticket1.setCategory(TicketCategory.ACCOUNT_ACCESS);
        ticket1.setAiAnalyzed(true);

        Ticket ticket2 = new Ticket();
        ticket2.setReferenceCode("TICK-SEED-0002");
        ticket2.setTitle("Charged twice on my invoice");
        ticket2.setDescription("I was billed twice this month and need a refund review.");
        ticket2.setCreatedBy(requester);
        ticket2.setAssignedTo(billingAgent);
        ticket2.setTeam(billing);
        ticket2.setStatus(TicketStatus.IN_PROGRESS);
        ticket2.setPriority(TicketPriority.HIGH);
        ticket2.setCategory(TicketCategory.BILLING);
        ticket2.setAiAnalyzed(false);

        ticketRepository.saveAll(List.of(ticket1, ticket2));

        TicketComment comment1 = new TicketComment();
        comment1.setTicket(ticket1);
        comment1.setAuthor(requester);
        comment1.setContent("This is blocking me from working.");
        comment1.setInternalNote(false);

        TicketComment comment2 = new TicketComment();
        comment2.setTicket(ticket2);
        comment2.setAuthor(billingAgent);
        comment2.setContent("Investigating payment processor logs.");
        comment2.setInternalNote(true);

        ticketCommentRepository.saveAll(List.of(comment1, comment2));

        TicketEvent event1 = new TicketEvent();
        event1.setTicket(ticket1);
        event1.setType(TicketEventType.TICKET_CREATED);
        event1.setActorType(ActorType.USER);
        event1.setActorId(requester.getId());
        event1.setDetails("Seeded ticket created");

        TicketEvent event2 = new TicketEvent();
        event2.setTicket(ticket1);
        event2.setType(TicketEventType.ASSIGNED);
        event2.setActorType(ActorType.SYSTEM);
        event2.setDetails("Assigned to Sara Support in team Account Management");

        TicketEvent event3 = new TicketEvent();
        event3.setTicket(ticket2);
        event3.setType(TicketEventType.TICKET_CREATED);
        event3.setActorType(ActorType.USER);
        event3.setActorId(requester.getId());
        event3.setDetails("Seeded ticket created");

        ticketEventRepository.saveAll(List.of(event1, event2, event3));

        AIRecommendation recommendation = new AIRecommendation();
        recommendation.setTicket(ticket1);
        recommendation.setPredictedCategory(TicketCategory.ACCOUNT_ACCESS);
        recommendation.setPredictedPriority(TicketPriority.HIGH);
        recommendation.setSuggestedTeam(accountManagement);
        recommendation.setSummary("User cannot sign in despite resetting the password and considers the issue blocking.");
        recommendation.setDraftReply("Thanks for reporting this. We are reviewing your account access issue and will help you restore access as quickly as possible.");
        recommendation.setConfidenceScore(0.93);
        recommendation.setAnalysisSource("RULE");
        recommendation.setReviewStatus(AIReviewStatus.PENDING);

        aiRecommendationRepository.save(recommendation);
    }

    private Team team(String name, String description) {
        Team team = new Team();
        team.setName(name);
        team.setDescription(description);
        return team;
    }
}

