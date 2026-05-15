package com.globalpayments.assignment.common.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SqlScripts {
    private SqlScripts() {
    }

    public static List<String> statements(String script) {
        Objects.requireNonNull(script, "script");

        String uncommentedScript = removeLineComments(script);

        return splitStatements(uncommentedScript);
    }

    private static String removeLineComments(String script) {
        StringBuilder withoutLineComments = new StringBuilder();

        for (String line : script.split("\\R")) {
            withoutLineComments.append(stripLineComment(line)).append('\n');
        }

        return withoutLineComments.toString();
    }

    private static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();

        for (String sql : script.split(";")) {
            String trimmed = sql.trim();
            if (!trimmed.isEmpty()) {
                statements.add(trimmed);
            }
        }

        return statements;
    }

    private static String stripLineComment(String line) {
        int commentIndex = line.indexOf("--");
        if (commentIndex < 0) {
            return line;
        }

        return line.substring(0, commentIndex);
    }
}
