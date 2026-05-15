package com.globalpayments.assignment.common.framedmessage.protocol;

import java.util.Arrays;
import java.util.Objects;

public record Frame(byte[] payload) {
    public static final int HEADER_BYTES = 2;
    public static final int MIN_PAYLOAD_BYTES = 1;
    public static final int MAX_PAYLOAD_BYTES = 4096;

    public Frame {
        Objects.requireNonNull(payload, "payload");
        validateLength(payload.length);
        payload = Arrays.copyOf(payload, payload.length);
    }

    @Override
    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }

    public int payloadLength() {
        return payload.length;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Frame frame && Arrays.equals(payload, frame.payload);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(payload);
    }

    static void validateLength(int length) {
        if (length < MIN_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Frame payload length must be at least 1 byte");
        }
        if (length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException(
                    "Frame payload length exceeds " + MAX_PAYLOAD_BYTES + " bytes: " + length
            );
        }
    }
}
