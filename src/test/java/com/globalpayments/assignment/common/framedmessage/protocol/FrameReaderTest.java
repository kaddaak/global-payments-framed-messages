package com.globalpayments.assignment.common.framedmessage.protocol;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrameReaderTest {
    private static final String TRUNCATED_HEADER_ERROR = "Truncated frame header: expected 2 bytes but reached EOF after 1 byte";

    private static final String ZERO_LENGTH_PAYLOAD_ERROR = "Invalid frame length: zero-length payload is not allowed";

    private static final String OVERSIZED_PAYLOAD_ERROR = "Invalid frame length: 4097 exceeds 4096 bytes";

    private static final String TRUNCATED_PAYLOAD_ERROR = "Truncated frame payload: expected 5 bytes but reached EOF after 3 bytes";

    private final FrameReader reader = new FrameReader();

    @Test
    void readsCompleteFramesInOrder() throws Exception {
        byte[] first = "PAYMENT|12345|100.50|HRK".getBytes(StandardCharsets.US_ASCII);
        byte[] second = "END".getBytes(StandardCharsets.US_ASCII);

        ByteArrayInputStream input = new ByteArrayInputStream(concat(frame(first), frame(second)));

        assertArrayEquals(first, reader.read(input).orElseThrow().payload());
        assertArrayEquals(second, reader.read(input).orElseThrow().payload());
        assertEquals(Optional.empty(), reader.read(input));
    }

    @Test
    void decodesIso88592PayloadAfterFrameParsing() throws Exception {
        Charset iso88592 = Charset.forName("ISO-8859-2");
        byte[] payload = "UPLATA|ČĆŽŠĐ|100.50|HRK".getBytes(iso88592);

        byte[] parsedPayload = reader
                .read(new ByteArrayInputStream(frame(payload)))
                .orElseThrow()
                .payload();

        assertEquals("UPLATA|ČĆŽŠĐ|100.50|HRK", new String(parsedPayload, iso88592));
    }

    @Test
    void returnsEmptyWhenEofOccursBeforeHeader() throws Exception {
        assertFalse(reader.read(new ByteArrayInputStream(new byte[0])).isPresent());
    }

    @Test
    void rejectsTruncatedHeader() {
        FrameException exception = assertThrows(
                FrameException.class,
                () -> reader.read(new ByteArrayInputStream(new byte[] { 0x00 }))
        );

        assertEquals(TRUNCATED_HEADER_ERROR, exception.getMessage());
    }

    @Test
    void rejectsZeroLengthPayload() {
        FrameException exception = assertThrows(
                FrameException.class,
                () -> reader.read(new ByteArrayInputStream(new byte[] { 0x00, 0x00 }))
        );

        assertEquals(ZERO_LENGTH_PAYLOAD_ERROR, exception.getMessage());
    }

    @Test
    void rejectsOversizedPayload() {
        FrameException exception = assertThrows(
                FrameException.class,
                () -> reader.read(new ByteArrayInputStream(new byte[] { 0x10, 0x01 }))
        );

        assertEquals(OVERSIZED_PAYLOAD_ERROR, exception.getMessage());
    }

    @Test
    void rejectsTruncatedPayload() {
        FrameException exception = assertThrows(
                FrameException.class,
                () -> reader.read(new ByteArrayInputStream(
                        new byte[] { 0x00, 0x05, 'A', 'B', 'C' }
                ))
        );

        assertEquals(TRUNCATED_PAYLOAD_ERROR, exception.getMessage());
    }

    private static byte[] frame(byte[] payload) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write((payload.length >>> 8) & 0xff);
        output.write(payload.length & 0xff);
        output.write(payload);
        return output.toByteArray();
    }

    private static byte[] concat(byte[] first, byte[] second) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(first);
        output.write(second);
        return output.toByteArray();
    }
}
