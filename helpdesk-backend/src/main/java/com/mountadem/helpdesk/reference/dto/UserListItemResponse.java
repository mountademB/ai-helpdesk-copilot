package com.mountadem.helpdesk.reference.dto;

public record UserListItemResponse(
        Long id,
        String fullName,
        String email,
        String role,
        Long teamId,
        String teamName,
        boolean active
) {
}
