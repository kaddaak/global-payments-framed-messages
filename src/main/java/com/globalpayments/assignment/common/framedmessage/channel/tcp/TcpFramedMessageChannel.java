package com.globalpayments.assignment.common.framedmessage.channel.tcp;

import com.globalpayments.assignment.common.framedmessage.channel.MessageChannel;
import com.globalpayments.assignment.common.framedmessage.channel.MessageChannelType;
import com.globalpayments.assignment.common.framedmessage.channel.MessageHandler;
import com.globalpayments.assignment.common.framedmessage.channel.MessageProcessingException;
import com.globalpayments.assignment.common.framedmessage.protocol.Frame;
import com.globalpayments.assignment.common.framedmessage.protocol.FrameException;
import com.globalpayments.assignment.common.framedmessage.protocol.FrameReader;
import com.globalpayments.assignment.common.logging.ApplicationLogger;
import com.globalpayments.assignment.common.validation.Ports;
import com.globalpayments.assignment.common.validation.TextValues;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.Optional;

public final class TcpFramedMessageChannel implements MessageChannel {
    private static final ApplicationLogger LOGGER = ApplicationLogger.forClass(TcpFramedMessageChannel.class);
    private static final int CLIENT_READ_TIMEOUT_MILLIS = 5_000;

    private final String host;
    private final int port;
    private final FrameReader frameReader;

    private volatile boolean running;
    private volatile ServerSocket serverSocket;
    private volatile Socket activeClientSocket;

    public TcpFramedMessageChannel(String host, int port) {
        this.host = TextValues.requireNonBlank(host, "host");
        this.port = Ports.requireTcpPort(port, "port", 0);
        this.frameReader = new FrameReader();
    }

    @Override
    public MessageChannelType type() {
        return MessageChannelType.TCP;
    }

    @Override
    public void receive(MessageHandler handler) throws IOException {
        Objects.requireNonNull(handler, "handler");
        open();
        ServerSocket socket = requireStartedSocket();
        running = true;

        try {
            receiveClients(socket, handler);
        } finally {
            running = false;
            close();
        }
    }

    public synchronized void open() throws IOException {
        if (isStarted()) {
            return;
        }

        ServerSocket socket = new ServerSocket();
        try {
            bindServerSocket(socket);
        } catch (IOException ex) {
            closeAfterFailedOpen(socket, ex);
            throw ex;
        }
    }

    public int boundPort() {
        ServerSocket socket = serverSocket;
        if (socket == null || !socket.isBound()) {
            throw new IllegalStateException("Server is not bound yet");
        }

        return socket.getLocalPort();
    }

    public boolean isStarted() {
        ServerSocket socket = serverSocket;

        return socket != null
                && socket.isBound()
                && !socket.isClosed();
    }

    @Override
    public boolean isReceiving() {
        return running;
    }

    @Override
    public synchronized void close() throws IOException {
        running = false;
        IOException failure = closeActiveClientSocket();

        ServerSocket socket = serverSocket;
        if (socket != null && !socket.isClosed()) {
            LOGGER.info("Stopping TCP framed message channel");
            try {
                socket.close();
            } catch (IOException ex) {
                if (failure == null) {
                    failure = ex;
                } else {
                    failure.addSuppressed(ex);
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    private void handleClient(Socket clientSocket, MessageHandler handler) throws IOException {
        SocketAddress remoteAddress = clientSocket.getRemoteSocketAddress();
        LOGGER.info("Accepted client connection from " + remoteAddress);

        try (Socket client = clientSocket) {
            handleClientFrames(client, handler);
        }

        LOGGER.info("Client connection closed: " + remoteAddress);
    }

    private void bindServerSocket(ServerSocket socket) throws IOException {
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(host, port));
        serverSocket = socket;
        LOGGER.info("TCP framed message channel listening on " + boundEndpoint(socket));
    }

    private ServerSocket requireStartedSocket() {
        ServerSocket socket = serverSocket;
        if (socket == null || !socket.isBound() || socket.isClosed()) {
            throw new IllegalStateException("Server is not bound yet");
        }

        return socket;
    }

    private static void closeAfterFailedOpen(
            ServerSocket socket,
            IOException failure
    ) {
        try {
            socket.close();
        } catch (IOException ex) {
            failure.addSuppressed(ex);
        }
    }

    private void receiveClients(ServerSocket socket, MessageHandler handler) throws IOException {
        while (running) {
            Socket clientSocket = null;
            try {
                clientSocket = acceptClient(socket);
                if (!clientSocket.isClosed()) {
                    handleClient(clientSocket, handler);
                }
            } catch (SocketException ex) {
                handleSocketException(ex);
            } catch (SocketTimeoutException ex) {
                rejectClientMessage(
                        "Timed out waiting for client frame data after "
                                + CLIENT_READ_TIMEOUT_MILLIS
                                + " ms"
                );
            } catch (FrameException ex) {
                rejectClientMessage(ex.getMessage());
            } catch (MessageProcessingException ex) {
                handleProcessingException(ex);
            } finally {
                clearActiveClientSocket(clientSocket);
            }
        }
    }

    private Socket acceptClient(ServerSocket socket) throws IOException {
        Socket clientSocket = socket.accept();
        activeClientSocket = clientSocket;
        configureClientSocket(clientSocket);

        if (!running) {
            clientSocket.close();
        }

        return clientSocket;
    }

    private static void configureClientSocket(Socket clientSocket) throws SocketException {
        clientSocket.setSoTimeout(CLIENT_READ_TIMEOUT_MILLIS);
    }

    private IOException closeActiveClientSocket() {
        Socket clientSocket = activeClientSocket;
        if (clientSocket == null || clientSocket.isClosed()) {
            return null;
        }

        try {
            clientSocket.close();
            return null;
        } catch (IOException ex) {
            return ex;
        }
    }

    private void clearActiveClientSocket(Socket clientSocket) {
        if (activeClientSocket == clientSocket) {
            activeClientSocket = null;
        }
    }

    private void handleSocketException(SocketException ex) throws SocketException {
        if (running) {
            throw ex;
        }
    }

    private void handleClientFrames(Socket client, MessageHandler handler) throws IOException {
        Optional<Frame> frame;

        while ((frame = frameReader.read(client.getInputStream())).isPresent()) {
            handleClientFrame(handler, frame.get());
        }
    }

    private static void handleClientFrame(
            MessageHandler handler,
            Frame frame
    ) throws IOException {
        try {
            handler.handle(frame);
        } catch (MessageProcessingException ex) {
            handleProcessingException(ex);
        }
    }

    private static void handleProcessingException(
            MessageProcessingException ex
    ) throws IOException {
        switch (ex.failureAction()) {
            case REJECT_MESSAGE -> rejectClientMessage(ex.getMessage());
            case STOP_CHANNEL -> throw ex;
        }
    }

    private static void rejectClientMessage(String message) {
        LOGGER.warn("Rejected client message: " + message);
    }

    private static String boundEndpoint(ServerSocket socket) {
        return socket.getInetAddress().getHostAddress() + ":" + socket.getLocalPort();
    }
}
