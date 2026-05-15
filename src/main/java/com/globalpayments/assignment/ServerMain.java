package com.globalpayments.assignment;

import com.globalpayments.assignment.common.logging.ApplicationLogger;
import com.globalpayments.assignment.common.sql.SqliteDatabaseInitializer;
import com.globalpayments.assignment.config.ApplicationConfig;
import com.globalpayments.assignment.config.ApplicationConfigLoader;
import com.globalpayments.assignment.transactionfeed.framedmessage.server.FramedTransactionServer;
import com.globalpayments.assignment.transactionfeed.parsing.TransactionFeedParser;
import com.globalpayments.assignment.transactionfeed.persistence.TransactionFeedRepository;
import com.globalpayments.assignment.transactionfeed.service.TransactionFeedProcessor;

import java.io.IOException;
import java.sql.SQLException;

public final class ServerMain {
    private static final ApplicationLogger LOGGER = ApplicationLogger.forClass(ServerMain.class);

    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        try {
            ApplicationConfig config = ApplicationConfigLoader.load(args);

            logConfiguration(config);
            initializeDatabase(config);
            TransactionFeedProcessor processor = buildProcessor(config);

            startServer(config, processor);
        } catch (Exception ex) {
            LOGGER.error("Server failed", ex);
            throw ex;
        }
    }

    private static void logConfiguration(ApplicationConfig config) {
        LOGGER.info("Starting framed transaction server");
        LOGGER.info("Configured TCP endpoint: " + config.serverHost() + ":" + config.serverPort());
        LOGGER.info("Configured UTF-8 output file: " + config.outputFile());
        LOGGER.info("Configured database: " + config.databaseUrl());
    }

    private static void initializeDatabase(ApplicationConfig config) throws IOException, SQLException {
        new SqliteDatabaseInitializer(config.databaseUrl(), config.databaseSchema()).initialize();
    }

    private static TransactionFeedProcessor buildProcessor(ApplicationConfig config) throws SQLException {
        TransactionFeedRepository repository = new TransactionFeedRepository(config.databaseUrl());

        return new TransactionFeedProcessor(
                new TransactionFeedParser(),
                repository
        );
    }

    private static void startServer(
            ApplicationConfig config,
            TransactionFeedProcessor processor
    ) throws IOException {
        try (FramedTransactionServer server = new FramedTransactionServer(
                config.serverHost(),
                config.serverPort(),
                config.outputFile(),
                processor
        )) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> closeQuietly(server)));
            server.start();
        }
    }

    private static void closeQuietly(FramedTransactionServer server) {
        try {
            server.close();
        } catch (Exception ex) {
            LOGGER.warn("Server shutdown close failed: " + ex.getMessage());
        }
    }
}
