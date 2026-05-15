package com.globalpayments.assignment.config;

import com.globalpayments.assignment.common.validation.Ports;
import com.globalpayments.assignment.common.validation.TextValues;

import java.nio.file.Path;
import java.util.Objects;

public record ApplicationConfig(
        String serverHost,
        int serverPort,
        Path clientInputFile,
        Path outputFile,
        String databaseUrl,
        String databaseSchema
) {
    public ApplicationConfig {
        serverHost = TextValues.requireNonBlank(serverHost, "serverHost");
        serverPort = Ports.requireTcpPort(serverPort, "serverPort", 0);
        clientInputFile = Objects.requireNonNull(clientInputFile, "clientInputFile");
        outputFile = Objects.requireNonNull(outputFile, "outputFile");
        databaseUrl = TextValues.requireNonBlank(databaseUrl, "databaseUrl");
        databaseSchema = TextValues.requireNonBlank(databaseSchema, "databaseSchema");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String serverHost;
        private Integer serverPort;
        private Path clientInputFile;
        private Path outputFile;
        private String databaseUrl;
        private String databaseSchema;

        private Builder() {
        }

        public Builder serverHost(String serverHost) {
            this.serverHost = serverHost;
            return this;
        }

        public Builder serverPort(int serverPort) {
            this.serverPort = serverPort;
            return this;
        }

        public Builder clientInputFile(Path clientInputFile) {
            this.clientInputFile = clientInputFile;
            return this;
        }

        public Builder outputFile(Path outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public Builder databaseUrl(String databaseUrl) {
            this.databaseUrl = databaseUrl;
            return this;
        }

        public Builder databaseSchema(String databaseSchema) {
            this.databaseSchema = databaseSchema;
            return this;
        }

        public ApplicationConfig build() {
            return new ApplicationConfig(
                    serverHost,
                    Objects.requireNonNull(serverPort, "serverPort"),
                    clientInputFile,
                    outputFile,
                    databaseUrl,
                    databaseSchema
            );
        }
    }
}
