package com.globalpayments.assignment.transactionfeed.framedmessage.client;

import com.globalpayments.assignment.common.framedmessage.protocol.Frame;
import com.globalpayments.assignment.common.framedmessage.protocol.FrameReader;
import com.globalpayments.assignment.common.framedmessage.protocol.FrameWriter;
import com.globalpayments.assignment.common.logging.ApplicationLogger;
import com.globalpayments.assignment.common.validation.Ports;
import com.globalpayments.assignment.common.validation.TextValues;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class FramedTransactionClient {
    private static final ApplicationLogger LOGGER = ApplicationLogger.forClass(FramedTransactionClient.class);

    private final String host;
    private final int port;
    private final FrameReader frameReader;
    private final FrameWriter frameWriter;

    public FramedTransactionClient(String host, int port) {
        this.host = TextValues.requireNonBlank(host, "host");
        this.port = Ports.requireTcpPort(port, "port", 1);
        this.frameReader = new FrameReader();
        this.frameWriter = new FrameWriter();
    }

    public int send(Path inputFile) throws IOException {
        logSendStart(inputFile);

        try (InputStream input = Files.newInputStream(inputFile);
             Socket socket = new Socket()) {
            connect(socket);

            return sendFrames(input, socket.getOutputStream());
        }
    }

    private void logSendStart(Path inputFile) {
        LOGGER.info("Opening framed input file: " + inputFile);
        LOGGER.info("Connecting to framed transaction server at " + host + ":" + port);
    }

    private void connect(Socket socket) throws IOException {
        socket.connect(new InetSocketAddress(host, port), 5_000);
        LOGGER.info("Connected to framed transaction server");
    }

    private int sendFrames(InputStream input, OutputStream output) throws IOException {
        int sentFrames = 0;
        Optional<Frame> frame;
        while ((frame = frameReader.read(input)).isPresent()) {
            sendFrame(output, frame.get());
            sentFrames++;
        }

        output.flush();

        return sentFrames;
    }

    private void sendFrame(OutputStream output, Frame frame) throws IOException {
        frameWriter.write(output, frame);
    }
}
