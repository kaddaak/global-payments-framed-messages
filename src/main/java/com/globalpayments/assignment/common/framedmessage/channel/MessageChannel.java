package com.globalpayments.assignment.common.framedmessage.channel;

import java.io.IOException;

public interface MessageChannel extends AutoCloseable {
    MessageChannelType type();

    void receive(MessageHandler handler) throws IOException;

    boolean isReceiving();

    @Override
    void close() throws IOException;
}
