package com.globalpayments.assignment.common.framedmessage.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public final class FrameReader {
    public Optional<Frame> read(InputStream input) throws IOException {
        Optional<Integer> payloadLength = readPayloadLength(input);
        if (payloadLength.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(readFrame(input, payloadLength.get()));
    }

    private static Optional<Integer> readPayloadLength(InputStream input) throws IOException {
        int firstHeaderByte = input.read();
        if (firstHeaderByte == -1) {
            return Optional.empty();
        }

        int secondHeaderByte = readSecondHeaderByte(input);
        int payloadLength = parsePayloadLength(firstHeaderByte, secondHeaderByte);

        validatePayloadLength(payloadLength);

        return Optional.of(payloadLength);
    }

    private static int readSecondHeaderByte(InputStream input) throws IOException {
        int secondHeaderByte = input.read();
        if (secondHeaderByte != -1) {
            return secondHeaderByte;
        }

        throw new FrameException(
                "Truncated frame header: expected 2 bytes but reached EOF after 1 byte"
        );
    }

    private static int parsePayloadLength(int firstHeaderByte, int secondHeaderByte) {
        return ((firstHeaderByte & 0xff) << 8) | (secondHeaderByte & 0xff);
    }

    private static void validatePayloadLength(int payloadLength) throws FrameException {
        if (payloadLength == 0) {
            throw new FrameException("Invalid frame length: zero-length payload is not allowed");
        }
        if (payloadLength > Frame.MAX_PAYLOAD_BYTES) {
            throw new FrameException(
                    "Invalid frame length: " + payloadLength
                            + " exceeds " + Frame.MAX_PAYLOAD_BYTES + " bytes"
            );
        }
    }

    private static Frame readFrame(InputStream input, int payloadLength) throws IOException {
        byte[] payload = readPayload(input, payloadLength);

        return new Frame(payload);
    }

    private static byte[] readPayload(InputStream input, int payloadLength) throws IOException {
        byte[] payload = input.readNBytes(payloadLength);
        if (payload.length != payloadLength) {
            throw new FrameException(
                    "Truncated frame payload: expected " + payloadLength
                            + " bytes but reached EOF after "
                            + payload.length
                            + " bytes"
            );
        }

        return payload;
    }
}
