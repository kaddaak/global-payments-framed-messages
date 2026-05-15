package com.globalpayments.assignment.common.framedmessage.protocol;

import java.io.IOException;
import java.io.OutputStream;

public final class FrameWriter {
    public void write(OutputStream output, Frame frame) throws IOException {
        int payloadLength = frame.payloadLength();

        writeHeader(output, payloadLength);
        writePayload(output, frame);
    }

    private static void writeHeader(OutputStream output, int payloadLength) throws IOException {
        output.write((payloadLength >>> 8) & 0xff);
        output.write(payloadLength & 0xff);
    }

    private static void writePayload(OutputStream output, Frame frame) throws IOException {
        output.write(frame.payload());
    }
}
