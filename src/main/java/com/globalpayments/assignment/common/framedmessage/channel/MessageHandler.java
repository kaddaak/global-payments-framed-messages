package com.globalpayments.assignment.common.framedmessage.channel;

import com.globalpayments.assignment.common.framedmessage.protocol.Frame;

import java.io.IOException;

@FunctionalInterface
public interface MessageHandler {
    void handle(Frame frame) throws IOException;
}
