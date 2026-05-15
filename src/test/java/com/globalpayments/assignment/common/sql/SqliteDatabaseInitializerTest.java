package com.globalpayments.assignment.common.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteDatabaseInitializerTest {
    @TempDir
    Path tempDir;

    @Test
    void createsSqliteFileParentDirectoriesAndSchemaIdempotently() throws Exception {
        Path databaseFile = tempDir.resolve("nested/framed-messages.db");
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        SqliteDatabaseInitializer initializer = new SqliteDatabaseInitializer(databaseUrl, "schema.sql");
        initializer.initialize();
        initializer.initialize();

        assertTrue(Files.exists(databaseFile));
        assertTableExists(databaseUrl, "received_transaction_messages");
    }

    @Test
    void createsParentDirectoriesForSqliteFileUri() throws Exception {
        Path databaseFile = tempDir.resolve("uri-nested/framed-messages.db");
        String databaseUrl = "jdbc:sqlite:file:" + databaseFile + "?mode=rwc";

        SqliteDatabaseInitializer initializer = new SqliteDatabaseInitializer(databaseUrl, "schema.sql");
        initializer.initialize();

        assertTrue(Files.exists(databaseFile));
        assertTableExists(databaseUrl, "received_transaction_messages");
    }

    private static void assertTableExists(String databaseUrl, String tableName) throws Exception {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT name
                     FROM sqlite_master
                     WHERE type = 'table'
                       AND name = ?
                     """)) {
            statement.setString(1, tableName);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(tableName, resultSet.getString("name"));
            }
        }
    }
}
