package com.globalpayments.assignment.common.framedmessage.codec;

import com.globalpayments.assignment.common.framedmessage.protocol.Frame;

import java.nio.charset.Charset;
import java.util.Objects;

public final class TextFrameDecoder {
    private final Charset charset;

    public TextFrameDecoder(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    public String decode(Frame frame) {
        Objects.requireNonNull(frame, "frame");

        return new String(frame.payload(), charset);
    }
}
