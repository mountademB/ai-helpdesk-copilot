package com.mountadem.helpdesk.dashboard.controller;

import com.mountadem.helpdesk.dashboard.dto.DashboardAiMetricsResponse;
import com.mountadem.helpdesk.dashboard.dto.RecentActivityResponse;
import com.mountadem.helpdesk.dashboard.service.DashboardAiMetricsService;
import com.mountadem.helpdesk.ticket.service.TicketService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardActivityController {

    private final TicketService ticketService;
    private final DashboardAiMetricsService dashboardAiMetricsService;

    @GetMapping("/recent-activity")
    public List<RecentActivityResponse> recentActivity() {
        return ticketService.listRecentActivity();
    }

    @GetMapping("/ai-metrics")
    public DashboardAiMetricsResponse aiMetrics() {
        return dashboardAiMetricsService.getMetrics();
    }
}
