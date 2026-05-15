package com.globalpayments.assignment.common.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class Directories {
    private Directories() {
    }

    public static void createParentIfPresent(Path path) throws IOException {
        Objects.requireNonNull(path, "path");

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
