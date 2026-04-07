package com.mountadem.helpdesk.comment.dto;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long authorId,
        String authorName,
        String content,
        boolean internalNote,
        Instant createdAt
) {
}
