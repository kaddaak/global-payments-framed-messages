package com.globalpayments.assignment.transactionfeed.framedmessage;

import com.globalpayments.assignment.common.framedmessage.protocol.Frame;
import com.globalpayments.assignment.common.framedmessage.protocol.FrameWriter;
import com.globalpayments.assignment.common.sql.SqliteDatabaseInitializer;
import com.globalpayments.assignment.transactionfeed.framedmessage.client.FramedTransactionClient;
import com.globalpayments.assignment.transactionfeed.framedmessage.server.FramedTransactionServer;
import com.globalpayments.assignment.transactionfeed.persistence.TransactionFeedRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientServerIntegrationTest {
    private static final Charset ISO_8859_2 = Charset.forName("ISO-8859-2");
    private static final String LOCALHOST = "127.0.0.1";

    @TempDir
    Path tempDir;

    @Test
    void clientToServerFlowWritesUtf8OutputAndDatabaseRows() throws Exception {
        Path inputFile = tempDir.resolve("input.bin");
        writeInputFrames(inputFile, List.of(
                "UPLATA|ČĆŽŠĐ|100.50|HRK",
                "STORNO|žćčšđ|42.00|EUR",
                "KRAJ"
        ));

        Path outputFile = tempDir.resolve("messages.txt");
        Path databaseFile = tempDir.resolve("messages.db");
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        new SqliteDatabaseInitializer(databaseUrl, "schema.sql").initialize();
        TransactionFeedRepository repository = new TransactionFeedRepository(databaseUrl);

        try (FramedTransactionServer server = new FramedTransactionServer(0, outputFile, repository)) {
            Thread serverThread = startServerInBackground(server);

            try {
                FramedTransactionClient client = new FramedTransactionClient(LOCALHOST, server.boundPort());

                assertEquals(3, client.send(inputFile));
                awaitOutputLines(outputFile, 3);
            } finally {
                try {
                    server.close();
                } finally {
                    serverThread.join(2_000);
                }
            }

            assertFalse(serverThread.isAlive());
        }

        assertEquals(
                List.of(
                        "UPLATA|ČĆŽŠĐ|100.50|HRK",
                        "STORNO|žćčšđ|42.00|EUR",
                        "KRAJ"
                ),
                Files.readAllLines(outputFile, StandardCharsets.UTF_8)
        );

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT received_order,
                            raw_message,
                            transaction_id,
                            currency
                     FROM received_transaction_messages
                     ORDER BY received_order
                     """)) {
            assertTrue(resultSet.next());
            assertEquals(1L, resultSet.getLong("received_order"));
            assertEquals("UPLATA|ČĆŽŠĐ|100.50|HRK", resultSet.getString("raw_message"));
            assertEquals("ČĆŽŠĐ", resultSet.getString("transaction_id"));
            assertEquals("HRK", resultSet.getString("currency"));

            assertTrue(resultSet.next());
            assertEquals(2L, resultSet.getLong("received_order"));
            assertEquals("STORNO|žćčšđ|42.00|EUR", resultSet.getString("raw_message"));
            assertEquals("žćčšđ", resultSet.getString("transaction_id"));
            assertEquals("EUR", resultSet.getString("currency"));

            assertTrue(resultSet.next());
            assertEquals(3L, resultSet.getLong("received_order"));
            assertEquals("KRAJ", resultSet.getString("raw_message"));
            assertNull(resultSet.getString("transaction_id"));
            assertNull(resultSet.getString("currency"));
        }
    }

    @Test
    void providedAsciiSampleIsProcessedEndToEnd() throws Exception {
        assertSampleInputProcessed(
                Path.of("docs/assignment/sample-input-ascii.bin"),
                List.of(
                        "PAYMENT|12345|100.50|HRK",
                        "REFUND|98765|42.00|EUR",
                        "END"
                )
        );
    }

    @Test
    void providedCroatianSampleIsProcessedEndToEnd() throws Exception {
        assertSampleInputProcessed(
                Path.of("docs/assignment/sample-input-hr.bin"),
                List.of(
                        "UPLATA|ČĆŽŠĐ|100.50|HRK",
                        "STORNO|žćčšđ|42.00|EUR",
                        "KRAJ"
                )
        );
    }

    @Test
    void malformedTransactionFrameIsWrittenToFileButRejectedFromDatabase() throws Exception {
        Path inputFile = tempDir.resolve("input.bin");
        writeInputFrames(inputFile, List.of(
                "UPLATA|FIRST|100.50|HRK",
                "BROKEN|MESSAGE|ONLY",
                "STORNO|SECOND|42.00|EUR"
        ));

        Path outputFile = tempDir.resolve("messages.txt");
        Path databaseFile = tempDir.resolve("messages.db");
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        new SqliteDatabaseInitializer(databaseUrl, "schema.sql").initialize();
        TransactionFeedRepository repository = new TransactionFeedRepository(databaseUrl);

        try (FramedTransactionServer server = new FramedTransactionServer(0, outputFile, repository)) {
            Thread serverThread = startServerInBackground(server);

            try {
                FramedTransactionClient client = new FramedTransactionClient(LOCALHOST, server.boundPort());

                assertEquals(3, client.send(inputFile));
                awaitOutputLines(outputFile, 3);
            } finally {
                try {
                    server.close();
                } finally {
                    serverThread.join(2_000);
                }
            }

            assertFalse(serverThread.isAlive());
        }

        assertEquals(
                List.of(
                        "UPLATA|FIRST|100.50|HRK",
                        "BROKEN|MESSAGE|ONLY",
                        "STORNO|SECOND|42.00|EUR"
                ),
                Files.readAllLines(outputFile, StandardCharsets.UTF_8)
        );

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT raw_message
                     FROM received_transaction_messages
                     ORDER BY received_order
                     """)) {
            assertTrue(resultSet.next());
            assertEquals("UPLATA|FIRST|100.50|HRK", resultSet.getString("raw_message"));

            assertTrue(resultSet.next());
            assertEquals("STORNO|SECOND|42.00|EUR", resultSet.getString("raw_message"));

            assertFalse(resultSet.next());
        }
    }

    @Test
    void startFailureDoesNotTruncateExistingOutputFile() throws Exception {
        Path outputFile = tempDir.resolve("messages.txt");
        Files.writeString(outputFile, "existing output\n", StandardCharsets.UTF_8);

        Path databaseFile = tempDir.resolve("messages.db");
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        new SqliteDatabaseInitializer(databaseUrl, "schema.sql").initialize();
        TransactionFeedRepository repository = new TransactionFeedRepository(databaseUrl);

        try (ServerSocket occupiedPort = new ServerSocket(0)) {
            FramedTransactionServer server = new FramedTransactionServer(
                    occupiedPort.getLocalPort(),
                    outputFile,
                    repository
            );

            assertThrows(IOException.class, server::start);
        }

        assertEquals(
                "existing output\n",
                Files.readString(outputFile, StandardCharsets.UTF_8)
        );
    }

    @Test
    void closeStopsServerWhenClientLeavesPartialFrameOpen() throws Exception {
        Path outputFile = tempDir.resolve("messages.txt");
        Path databaseFile = tempDir.resolve("messages.db");
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        new SqliteDatabaseInitializer(databaseUrl, "schema.sql").initialize();
        TransactionFeedRepository repository = new TransactionFeedRepository(databaseUrl);

        try (FramedTransactionServer server = new FramedTransactionServer(0, outputFile, repository)) {
            Thread serverThread = startServerInBackground(server);

            try (Socket client = new Socket(LOCALHOST, server.boundPort())) {
                OutputStream output = client.getOutputStream();
                new FrameWriter().write(
                        output,
                        new Frame("UPLATA|FIRST|100.50|HRK".getBytes(ISO_8859_2))
                );
                output.flush();
                awaitOutputLines(outputFile, 1);

                output.write(new byte[] { 0x00, 0x05, 'A' });
                output.flush();

                server.close();
                serverThread.join(2_000);

                assertFalse(serverThread.isAlive());
            } finally {
                server.close();
                serverThread.join(2_000);
            }
        }
    }

    @Test
    void stalledClientIsTimedOutAndNextClientIsProcessed() throws Exception {
        Path inputFile = tempDir.resolve("input.bin");
        writeInputFrames(inputFile, List.of("UPLATA|AFTERTIMEOUT|1.00|EUR"));

        Path outputFile = tempDir.resolve("messages.txt");
        Path databaseFile = tempDir.resolve("messages.db");
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        new SqliteDatabaseInitializer(databaseUrl, "schema.sql").initialize();
        TransactionFeedRepository repository = new TransactionFeedRepository(databaseUrl);

        try (FramedTransactionServer server = new FramedTransactionServer(0, outputFile, repository)) {
            Thread serverThread = startServerInBackground(server);

            try (Socket stalledClient = new Socket(LOCALHOST, server.boundPort())) {
                sendPartialFrame(stalledClient);

                FramedTransactionClient client = new FramedTransactionClient(LOCALHOST, server.boundPort());

                assertEquals(1, client.send(inputFile));
                awaitOutputLines(outputFile, 1, Duration.ofSeconds(8));
                assertTrue(serverThread.isAlive());
            } finally {
                server.close();
                serverThread.join(2_000);
            }

            assertFalse(serverThread.isAlive());
        }

        assertEquals(
                List.of("UPLATA|AFTERTIMEOUT|1.00|EUR"),
                Files.readAllLines(outputFile, StandardCharsets.UTF_8)
        );

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT raw_message
                     FROM received_transaction_messages
                     ORDER BY received_order
                     """)) {
            assertTrue(resultSet.next());
            assertEquals("UPLATA|AFTERTIMEOUT|1.00|EUR", resultSet.getString("raw_message"));

            assertFalse(resultSet.next());
        }
    }

    @Test
    void truncatedSocketFrameIsRejectedAndNextClientIsProcessed() throws Exception {
        Path inputFile = tempDir.resolve("input.bin");
        writeInputFrames(inputFile, List.of("UPLATA|AFTERTRUNCATED|1.00|EUR"));

        Path outputFile = tempDir.resolve("messages.txt");
        Path databaseFile = tempDir.resolve("messages.db");
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        new SqliteDatabaseInitializer(databaseUrl, "schema.sql").initialize();
        TransactionFeedRepository repository = new TransactionFeedRepository(databaseUrl);

        try (FramedTransactionServer server = new FramedTransactionServer(0, outputFile, repository)) {
            Thread serverThread = startServerInBackground(server);

            try {
                try (Socket client = new Socket(LOCALHOST, server.boundPort())) {
                    sendPartialFrame(client);
                }

                FramedTransactionClient client = new FramedTransactionClient(LOCALHOST, server.boundPort());

                assertEquals(1, client.send(inputFile));
                awaitOutputLines(outputFile, 1);
                assertTrue(serverThread.isAlive());
            } finally {
                server.close();
                serverThread.join(2_000);
            }

            assertFalse(serverThread.isAlive());
        }

        assertEquals(
                List.of("UPLATA|AFTERTRUNCATED|1.00|EUR"),
                Files.readAllLines(outputFile, StandardCharsets.UTF_8)
        );
        assertDatabaseRawMessages(databaseUrl, List.of("UPLATA|AFTERTRUNCATED|1.00|EUR"));
    }

    @Test
    void persistenceFailureWritesDecodedLineAndStopsServer() throws Exception {
        String message = "UPLATA|DBFAILURE|1.00|EUR";
        Path inputFile = tempDir.resolve("input.bin");
        writeInputFrames(inputFile, List.of(message));

        Path outputFile = tempDir.resolve("messages.txt");
        Path databaseFile = tempDir.resolve("messages.db");
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        new SqliteDatabaseInitializer(databaseUrl, "schema.sql").initialize();
        TransactionFeedRepository repository = new TransactionFeedRepository(databaseUrl);
        installInsertFailureTrigger(databaseUrl);

        try (FramedTransactionServer server = new FramedTransactionServer(0, outputFile, repository)) {
            Thread serverThread = startServerInBackground(server);

            try {
                FramedTransactionClient client = new FramedTransactionClient(LOCALHOST, server.boundPort());

                assertEquals(1, client.send(inputFile));
                awaitOutputLines(outputFile, 1);
                serverThread.join(2_000);

                assertFalse(serverThread.isAlive());
            } finally {
                server.close();
                serverThread.join(2_000);
            }
        }

        assertEquals(
                List.of(message),
                Files.readAllLines(outputFile, StandardCharsets.UTF_8)
        );
        assertDatabaseRawMessages(databaseUrl, List.of());
    }

    private void assertSampleInputProcessed(
            Path inputFile,
            List<String> expectedMessages
    ) throws Exception {
        Path outputFile = tempDir.resolve(inputFile.getFileName() + ".txt");
        Path databaseFile = tempDir.resolve(inputFile.getFileName() + ".db");
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        new SqliteDatabaseInitializer(databaseUrl, "schema.sql").initialize();
        TransactionFeedRepository repository = new TransactionFeedRepository(databaseUrl);

        try (FramedTransactionServer server = new FramedTransactionServer(0, outputFile, repository)) {
            Thread serverThread = startServerInBackground(server);

            try {
                FramedTransactionClient client = new FramedTransactionClient(LOCALHOST, server.boundPort());

                assertEquals(expectedMessages.size(), client.send(inputFile));
                awaitOutputLines(outputFile, expectedMessages.size());
            } finally {
                server.close();
                serverThread.join(2_000);
            }

            assertFalse(serverThread.isAlive());
        }

        assertEquals(
                expectedMessages,
                Files.readAllLines(outputFile, StandardCharsets.UTF_8)
        );
        assertDatabaseRawMessages(databaseUrl, expectedMessages);
    }

    private static Thread startServerInBackground(FramedTransactionServer server) throws IOException {
        AtomicReference<IOException> startupFailure = new AtomicReference<>();
        Thread thread = backgroundServerThread(server, startupFailure);

        thread.start();
        awaitServerBinding(server, thread, startupFailure);

        return thread;
    }

    private static Thread backgroundServerThread(
            FramedTransactionServer server,
            AtomicReference<IOException> startupFailure
    ) {
        return new Thread(
                () -> {
                    try {
                        server.start();
                    } catch (IOException ex) {
                        startupFailure.set(ex);
                    }
                },
                "framed-transaction-server-test"
        );
    }

    private static void awaitServerBinding(
            FramedTransactionServer server,
            Thread thread,
            AtomicReference<IOException> startupFailure
    ) throws IOException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();

        while (System.nanoTime() < deadline) {
            throwIfStartupFailed(startupFailure);

            if (isServerBound(server)) {
                return;
            }

            throwIfServerStopped(thread);
            pauseBeforeStartupRetry();
        }

        throw new IOException("Timed out waiting for server startup");
    }

    private static void throwIfStartupFailed(
            AtomicReference<IOException> startupFailure
    ) throws IOException {
        IOException failure = startupFailure.get();
        if (failure != null) {
            throw failure;
        }
    }

    private static boolean isServerBound(FramedTransactionServer server) {
        try {
            server.boundPort();
            return true;
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    private static void throwIfServerStopped(Thread thread) throws IOException {
        if (!thread.isAlive()) {
            throw new IOException("Server stopped before startup completed");
        }
    }

    private static void pauseBeforeStartupRetry() throws IOException {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for server startup", ex);
        }
    }

    private static void installInsertFailureTrigger(String databaseUrl) throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TRIGGER fail_received_transaction_messages_insert
                    BEFORE INSERT ON received_transaction_messages
                    BEGIN
                        SELECT RAISE(FAIL, 'simulated persistence failure');
                    END
                    """);
        }
    }

    private static void assertDatabaseRawMessages(
            String databaseUrl,
            List<String> expectedMessages
    ) throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT raw_message
                     FROM received_transaction_messages
                     ORDER BY received_order
                     """)) {
            for (String expectedMessage : expectedMessages) {
                assertTrue(resultSet.next());
                assertEquals(expectedMessage, resultSet.getString("raw_message"));
            }

            assertFalse(resultSet.next());
        }
    }

    private static void sendPartialFrame(Socket client) throws IOException {
        OutputStream output = client.getOutputStream();

        output.write(new byte[] { 0x00, 0x05, 'A' });
        output.flush();
    }

    private static void writeInputFrames(Path inputFile, List<String> messages) throws IOException {
        FrameWriter writer = new FrameWriter();

        try (OutputStream output = Files.newOutputStream(inputFile)) {
            for (String message : messages) {
                writer.write(output, new Frame(message.getBytes(ISO_8859_2)));
            }
        }
    }

    private static void awaitOutputLines(
            Path outputFile,
            int expectedLines
    ) throws IOException, InterruptedException {
        awaitOutputLines(outputFile, expectedLines, Duration.ofSeconds(5));
    }

    private static void awaitOutputLines(
            Path outputFile,
            int expectedLines,
            Duration timeout
    ) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            if (hasAtLeastOutputLines(outputFile, expectedLines)) {
                return;
            }

            Thread.sleep(50);
        }

        throw new AssertionError("Timed out waiting for " + expectedLines + " output lines");
    }

    private static boolean hasAtLeastOutputLines(
            Path outputFile,
            int expectedLines
    ) throws IOException {
        return Files.exists(outputFile)
                && Files.readAllLines(outputFile, StandardCharsets.UTF_8).size() >= expectedLines;
    }
}
