package com.mountadem.helpdesk.comment.dto;

public record AddCommentRequest(
        Long authorId,
        String content,
        Boolean internalNote
) {
}
