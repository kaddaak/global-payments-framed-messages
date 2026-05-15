package com.globalpayments.assignment.transactionfeed.framedmessage.server;

import com.globalpayments.assignment.common.framedmessage.channel.MessageFailureAction;
import com.globalpayments.assignment.common.framedmessage.channel.MessageProcessingException;
import com.globalpayments.assignment.common.framedmessage.channel.tcp.TcpFramedMessageChannel;
import com.globalpayments.assignment.common.framedmessage.codec.TextFrameDecoder;
import com.globalpayments.assignment.common.framedmessage.protocol.Frame;
import com.globalpayments.assignment.common.io.Directories;
import com.globalpayments.assignment.common.logging.ApplicationLogger;
import com.globalpayments.assignment.transactionfeed.parsing.TransactionFeedParser;
import com.globalpayments.assignment.transactionfeed.parsing.TransactionParseException;
import com.globalpayments.assignment.transactionfeed.persistence.TransactionFeedRepository;
import com.globalpayments.assignment.transactionfeed.service.TransactionFeedProcessingResult;
import com.globalpayments.assignment.transactionfeed.service.TransactionFeedProcessor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class FramedTransactionServer implements AutoCloseable {
    private static final ApplicationLogger LOGGER = ApplicationLogger.forClass(FramedTransactionServer.class);
    private static final TextFrameDecoder WIRE_DECODER = new TextFrameDecoder(Charset.forName("ISO-8859-2"));

    private final TcpFramedMessageChannel channel;
    private final Path outputFile;
    private final TransactionFeedProcessor processor;

    public FramedTransactionServer(
            int port,
            Path outputFile,
            TransactionFeedRepository repository
    ) {
        this(
                "127.0.0.1",
                port,
                outputFile,
                new TransactionFeedProcessor(new TransactionFeedParser(), repository)
        );
    }

    public FramedTransactionServer(
            String host,
            int port,
            Path outputFile,
            TransactionFeedProcessor processor
    ) {
        this(
                new TcpFramedMessageChannel(host, port),
                outputFile,
                processor
        );
    }

    FramedTransactionServer(
            TcpFramedMessageChannel channel,
            Path outputFile,
            TransactionFeedProcessor processor
    ) {
        this.channel = Objects.requireNonNull(channel, "channel");
        this.outputFile = Objects.requireNonNull(outputFile, "outputFile");
        this.processor = Objects.requireNonNull(processor, "processor");
    }

    public void start() throws IOException {
        try {
            channel.open();
            prepareOutputFile();
            try (BufferedWriter writer = openOutputWriter()) {
                receiveFrames(writer);
            }
        } catch (IOException ex) {
            closeAfterFailure(ex);
            throw ex;
        }
    }

    private void closeAfterFailure(IOException failure) {
        try {
            channel.close();
        } catch (IOException ex) {
            failure.addSuppressed(ex);
        }
    }

    private void prepareOutputFile() throws IOException {
        LOGGER.info("Preparing UTF-8 output file: " + outputFile);
        Directories.createParentIfPresent(outputFile);
    }

    private void receiveFrames(BufferedWriter writer) throws IOException {
        LOGGER.info("Starting framed transaction message channel");
        try {
            channel.receive(frame -> handleFrame(writer, frame));
        } finally {
            LOGGER.info("Framed transaction server stopped");
        }
    }

    public int boundPort() {
        return channel.boundPort();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    private BufferedWriter openOutputWriter() throws IOException {
        return Files.newBufferedWriter(
                outputFile,
                StandardCharsets.UTF_8,
                CREATE,
                WRITE,
                TRUNCATE_EXISTING
        );
    }

    private void handleFrame(BufferedWriter writer, Frame frame) throws IOException {
        String decodedMessage = decode(frame);
        writeDecodedMessage(writer, decodedMessage);

        try {
            TransactionFeedProcessingResult result = process(decodedMessage);
            logStoredMessage(result);
        } catch (TransactionParseException ex) {
            throw new MessageProcessingException(
                    MessageFailureAction.REJECT_MESSAGE,
                    ex.getMessage(),
                    ex
            );
        } catch (SQLException ex) {
            throw new MessageProcessingException(
                    MessageFailureAction.STOP_CHANNEL,
                    "Failed to persist received message",
                    ex
            );
        }
    }

    private String decode(Frame frame) {
        return WIRE_DECODER.decode(frame);
    }

    private TransactionFeedProcessingResult process(String decodedMessage) throws SQLException {
        return processor.process(decodedMessage);
    }

    private static void writeDecodedMessage(
            BufferedWriter writer,
            String decodedMessage
    ) throws IOException {
        writer.write(decodedMessage);
        writer.newLine();
        writer.flush();
    }

    private static void logStoredMessage(TransactionFeedProcessingResult result) {
        LOGGER.info(
                "Stored received message #"
                        + result.receivedOrder()
                        + " as "
                        + result.messageKind()
        );
    }
}
