package com.globalpayments.assignment.common.io;

import com.globalpayments.assignment.common.validation.TextValues;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

public final class Resources {
    private Resources() {
    }

    public static InputStream openRequired(
            String resourceName,
            Class<?> fallbackClass
    ) throws IOException {
        String normalizedResourceName = TextValues.requireNonBlank(resourceName, "resourceName");
        InputStream input = open(normalizedResourceName, fallbackClass);

        if (input == null) {
            throw new IOException("Missing classpath resource: " + normalizedResourceName);
        }

        return input;
    }

    public static Optional<String> readString(
            String resourceName,
            Class<?> fallbackClass,
            Charset charset
    ) throws IOException {
        String normalizedResourceName = TextValues.requireNonBlank(resourceName, "resourceName");
        Charset effectiveCharset = charset != null ? charset : Charset.defaultCharset();

        return readStringResource(normalizedResourceName, fallbackClass, effectiveCharset);
    }

    private static Optional<String> readStringResource(
            String resourceName,
            Class<?> fallbackClass,
            Charset charset
    ) throws IOException {
        try (InputStream input = open(resourceName, fallbackClass)) {
            if (input == null) {
                return Optional.empty();
            }

            return Optional.of(new String(input.readAllBytes(), charset));
        }
    }

    private static InputStream open(String resourceName, Class<?> fallbackClass) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream input = loader != null ? loader.getResourceAsStream(resourceName) : null;

        if (input == null && fallbackClass != null && fallbackClass.getClassLoader() != null) {
            input = fallbackClass.getClassLoader().getResourceAsStream(resourceName);
        }

        if (input == null && Resources.class.getClassLoader() != null) {
            input = Resources.class.getClassLoader().getResourceAsStream(resourceName);
        }

        return input;
    }
}
