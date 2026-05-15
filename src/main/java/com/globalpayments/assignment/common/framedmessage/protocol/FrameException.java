package com.globalpayments.assignment.common.framedmessage.protocol;

import java.io.IOException;

public final class FrameException extends IOException {
    public FrameException(String message) {
        super(message);
    }
}
