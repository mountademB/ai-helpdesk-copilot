package com.mountadem.helpdesk.reference.controller;

import com.mountadem.helpdesk.reference.dto.TeamListItemResponse;
import com.mountadem.helpdesk.reference.dto.UserListItemResponse;
import com.mountadem.helpdesk.ticket.service.TicketService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final TicketService ticketService;

    @GetMapping("/users")
    public List<UserListItemResponse> users() {
        return ticketService.listUsers();
    }

    @GetMapping("/teams")
    public List<TeamListItemResponse> teams() {
        return ticketService.listTeams();
    }
}
