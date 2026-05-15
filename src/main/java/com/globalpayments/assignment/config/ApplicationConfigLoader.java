package com.globalpayments.assignment.config;

import com.globalpayments.assignment.common.io.Resources;
import com.globalpayments.assignment.common.validation.TextValues;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

public final class ApplicationConfigLoader {
    private static final String DEFAULT_RESOURCE = "application.properties";

    private ApplicationConfigLoader() {
    }

    public static ApplicationConfig load(String[] args) throws IOException {
        Properties properties = loadProperties(DEFAULT_RESOURCE);
        Map<ConfigValue, String> cli = parseArgs(args);

        return buildConfig(cli, properties);
    }

    private static ApplicationConfig buildConfig(
            Map<ConfigValue, String> cli,
            Properties properties
    ) {
        return ApplicationConfig.builder()
                .serverHost(ConfigValue.SERVER_HOST.resolve(cli, properties))
                .serverPort(Integer.parseInt(ConfigValue.SERVER_PORT.resolve(cli, properties)))
                .clientInputFile(Path.of(ConfigValue.CLIENT_INPUT_FILE.resolve(cli, properties)))
                .outputFile(Path.of(ConfigValue.OUTPUT_FILE.resolve(cli, properties)))
                .databaseUrl(ConfigValue.DATABASE_URL.resolve(cli, properties))
                .databaseSchema(ConfigValue.DATABASE_SCHEMA.resolve(cli, properties))
                .build();
    }

    private static Properties loadProperties(String resourceName) throws IOException {
        Properties properties = new Properties();

        try (InputStream input = Resources.openRequired(resourceName, ApplicationConfigLoader.class)) {
            properties.load(input);
        }

        return properties;
    }

    private static Map<ConfigValue, String> parseArgs(String[] args) {
        Map<ConfigValue, String> parsed = new EnumMap<>(ConfigValue.class);

        for (int i = 0; i < args.length; i++) {
            i = parseArgument(args, i, parsed);
        }

        return parsed;
    }

    private static int parseArgument(
            String[] args,
            int index,
            Map<ConfigValue, String> parsed
    ) {
        String arg = args[index];
        requireCliArgument(arg);

        String keyValue = arg.substring(2);
        int equalsIndex = keyValue.indexOf('=');

        if (equalsIndex >= 0) {
            parseInlineValue(parsed, keyValue, equalsIndex);
            return index;
        }

        return parseNextArgumentValue(args, index, parsed, keyValue);
    }

    private static void requireCliArgument(String arg) {
        if (!arg.startsWith("--")) {
            throw new IllegalArgumentException("Unexpected argument: " + arg);
        }
    }

    private static void parseInlineValue(
            Map<ConfigValue, String> parsed,
            String keyValue,
            int equalsIndex
    ) {
        ConfigValue configValue = ConfigValue.fromCliKey(
                keyValue.substring(0, equalsIndex)
        );
        parsed.put(configValue, keyValue.substring(equalsIndex + 1));
    }

    private static int parseNextArgumentValue(
            String[] args,
            int index,
            Map<ConfigValue, String> parsed,
            String key
    ) {
        if (index + 1 >= args.length) {
            throw new IllegalArgumentException("Missing value for argument: " + args[index]);
        }

        ConfigValue configValue = ConfigValue.fromCliKey(key);
        parsed.put(configValue, args[index + 1]);

        return index + 1;
    }

    private enum ConfigValue {
        SERVER_HOST("host", "app.server.host"),
        SERVER_PORT("port", "app.server.port"),
        CLIENT_INPUT_FILE("input", "app.client.input-file"),
        OUTPUT_FILE("output", "app.output.file"),
        DATABASE_URL("db", "app.database.url"),
        DATABASE_SCHEMA("schema", "app.database.schema");

        private final String cliKey;
        private final String propertyKey;

        ConfigValue(String cliKey, String propertyKey) {
            this.cliKey = cliKey;
            this.propertyKey = propertyKey;
        }

        private String resolve(Map<ConfigValue, String> cli, Properties properties) {
            String fromCli = cli.get(this);
            if (fromCli != null) {
                return requireConfiguredValue(fromCli);
            }

            String fromSystemProperty = System.getProperty(propertyKey);
            if (fromSystemProperty != null) {
                return requireConfiguredValue(fromSystemProperty);
            }

            return requireConfiguredValue(properties.getProperty(propertyKey));
        }

        private String requireConfiguredValue(String value) {
            return TextValues.nonBlank(value)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Missing configuration value: " + propertyKey
                    ));
        }

        private static ConfigValue fromCliKey(String cliKey) {
            for (ConfigValue configValue : values()) {
                if (configValue.cliKey.equals(cliKey)) {
                    return configValue;
                }
            }

            throw new IllegalArgumentException("Unknown argument: --" + cliKey);
        }
    }
}
