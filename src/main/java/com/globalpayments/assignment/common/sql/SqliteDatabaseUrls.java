package com.globalpayments.assignment.common.sql;

import java.nio.file.Path;
import java.util.Optional;

public final class SqliteDatabaseUrls {
    private static final String SQLITE_PREFIX = "jdbc:sqlite:";

    private SqliteDatabaseUrls() {
    }

    public static Optional<Path> filePath(String databaseUrl) {
        if (databaseUrl == null || !databaseUrl.startsWith(SQLITE_PREFIX)) {
            return Optional.empty();
        }

        String pathValue = filePathValue(databaseUrl.substring(SQLITE_PREFIX.length()));
        if (!isFileBackedPath(pathValue)) {
            return Optional.empty();
        }

        return Optional.of(Path.of(pathValue));
    }

    private static boolean isFileBackedPath(String pathValue) {
        return pathValue != null
                && !pathValue.isBlank()
                && !pathValue.equals(":memory:");
    }

    private static String filePathValue(String pathValue) {
        String filePath = removeQueryString(pathValue);
        if (filePath.startsWith("file:")) {
            return filePath.substring("file:".length());
        }

        return filePath;
    }

    private static String removeQueryString(String pathValue) {
        int queryIndex = pathValue.indexOf('?');
        if (queryIndex < 0) {
            return pathValue;
        }

        return pathValue.substring(0, queryIndex);
    }
}
