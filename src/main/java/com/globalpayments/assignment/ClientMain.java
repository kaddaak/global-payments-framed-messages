package com.globalpayments.assignment;

import com.globalpayments.assignment.common.logging.ApplicationLogger;
import com.globalpayments.assignment.config.ApplicationConfig;
import com.globalpayments.assignment.config.ApplicationConfigLoader;
import com.globalpayments.assignment.transactionfeed.framedmessage.client.FramedTransactionClient;

import java.io.IOException;

public final class ClientMain {
    private static final ApplicationLogger LOGGER = ApplicationLogger.forClass(ClientMain.class);

    private ClientMain() {
    }

    public static void main(String[] args) throws Exception {
        try {
            ApplicationConfig config = ApplicationConfigLoader.load(args);

            logStartup();
            sendConfiguredInput(config);
        } catch (Exception ex) {
            LOGGER.error("Client failed", ex);
            throw ex;
        }
    }

    private static void logStartup() {
        LOGGER.info("Starting framed transaction client");
    }

    private static void sendConfiguredInput(ApplicationConfig config) throws IOException {
        FramedTransactionClient client = buildClient(config);
        int sentFrames = client.send(config.clientInputFile());

        logSentFrames(sentFrames, config);
    }

    private static FramedTransactionClient buildClient(ApplicationConfig config) {
        return new FramedTransactionClient(config.serverHost(), config.serverPort());
    }

    private static void logSentFrames(int sentFrames, ApplicationConfig config) {
        LOGGER.info("Sent " + sentFrames + " frame(s) from " + config.clientInputFile());
    }
}
