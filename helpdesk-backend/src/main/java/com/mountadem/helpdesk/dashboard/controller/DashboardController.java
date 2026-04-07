package com.mountadem.helpdesk.dashboard.controller;

import com.mountadem.helpdesk.dashboard.dto.DashboardSummaryResponse;
import com.mountadem.helpdesk.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TicketService ticketService;

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return ticketService.dashboardSummary();
    }
}
