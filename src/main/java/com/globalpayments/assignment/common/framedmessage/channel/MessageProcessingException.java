package com.globalpayments.assignment.common.framedmessage.channel;

import java.io.IOException;
import java.util.Objects;

public final class MessageProcessingException extends IOException {
    private final MessageFailureAction failureAction;

    public MessageProcessingException(
            MessageFailureAction failureAction,
            String message
    ) {
        super(message);
        this.failureAction = Objects.requireNonNull(failureAction, "failureAction");
    }

    public MessageProcessingException(
            MessageFailureAction failureAction,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.failureAction = Objects.requireNonNull(failureAction, "failureAction");
    }

    public MessageFailureAction failureAction() {
        return failureAction;
    }
}
