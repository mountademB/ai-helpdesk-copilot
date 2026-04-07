package com.mountadem.helpdesk.ai.dto;

public record RewriteDraftReplyResponse(
        String mode,
        String rewrittenText,
        String source
) {
}
