package com.globalpayments.assignment.common.sql;

import com.globalpayments.assignment.common.io.Directories;
import com.globalpayments.assignment.common.io.Resources;
import com.globalpayments.assignment.common.logging.ApplicationLogger;
import com.globalpayments.assignment.common.validation.TextValues;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

public final class SqliteDatabaseInitializer {
    private static final ApplicationLogger LOGGER = ApplicationLogger.forClass(SqliteDatabaseInitializer.class);

    private final String databaseUrl;
    private final String schemaResource;

    public SqliteDatabaseInitializer(String databaseUrl, String schemaResource) {
        this.databaseUrl = TextValues.requireNonBlank(databaseUrl, "databaseUrl");
        this.schemaResource = TextValues.requireNonBlank(schemaResource, "schemaResource");
    }

    public void initialize() throws IOException, SQLException {
        LOGGER.info("Initializing database schema: " + schemaResource);

        List<String> statements = loadSchemaStatements();
        applySchemaStatements(statements);
        logSchemaReady(statements);
    }

    private List<String> loadSchemaStatements() throws IOException {
        ensureSqliteParentDirectory();
        String schema = loadSchema();

        return SqlScripts.statements(schema);
    }

    private void applySchemaStatements(List<String> statements) throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    private static void logSchemaReady(List<String> statements) {
        LOGGER.info("Database schema ready: " + statements.size() + " statement(s) applied");
    }

    private void ensureSqliteParentDirectory() throws IOException {
        Optional<Path> databasePath = SqliteDatabaseUrls.filePath(databaseUrl);
        if (databasePath.isPresent()) {
            Directories.createParentIfPresent(databasePath.get());
        }
    }

    private String loadSchema() throws IOException {
        Optional<String> classpathSchema = Resources.readString(
                schemaResource,
                SqliteDatabaseInitializer.class,
                StandardCharsets.UTF_8
        );

        if (classpathSchema.isPresent()) {
            return classpathSchema.get();
        }

        Path schemaPath = Path.of(schemaResource);
        if (Files.exists(schemaPath)) {
            return Files.readString(schemaPath, StandardCharsets.UTF_8);
        }

        throw new IOException("Missing database schema resource: " + schemaResource);
    }
}
