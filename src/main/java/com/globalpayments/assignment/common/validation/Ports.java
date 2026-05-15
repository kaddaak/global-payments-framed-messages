package com.globalpayments.assignment.common.validation;

public final class Ports {
    private static final int MAX_PORT = 65_535;

    private Ports() {
    }

    public static int requireTcpPort(int port, String name, int minimumPort) {
        if (port < minimumPort || port > MAX_PORT) {
            throw new IllegalArgumentException(
                    name + " must be between " + minimumPort + " and " + MAX_PORT
            );
        }

        return port;
    }
}
