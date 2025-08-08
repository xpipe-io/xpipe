package io.xpipe.beacon;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.util.Optional;

@UtilityClass
public class BeaconConfig {

    public static final String BEACON_PORT_PROP = "io.xpipe.beacon.port";
    public static final String DAEMON_ARGUMENTS_PROP = "io.xpipe.beacon.daemonArgs";
    private static final String PRINT_MESSAGES_PROPERTY = "io.xpipe.beacon.printMessages";

    public static boolean printMessages() {
        if (System.getProperty(PRINT_MESSAGES_PROPERTY) != null) {
            return Boolean.parseBoolean(System.getProperty(PRINT_MESSAGES_PROPERTY));
        }
        return false;
    }

    public static int getUsedPort() {
        var beaconPort = System.getenv("BEACON_PORT");
        if (beaconPort != null && !beaconPort.isBlank()) {
            return Integer.parseInt(beaconPort);
        }

        if (System.getProperty(BEACON_PORT_PROP) != null) {
            return Integer.parseInt(System.getProperty(BEACON_PORT_PROP));
        }

        return getDefaultBeaconPort();
    }

    public static int getDefaultBeaconPort() {
        var staging = Optional.ofNullable(System.getProperty("io.xpipe.app.staging"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        var offset = staging ? 1 : 0;
        return 21721 + offset;
    }

    public static Path getLocalBeaconAuthFile() {
        var staging = Optional.ofNullable(System.getProperty("io.xpipe.app.staging"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        return Path.of(System.getProperty("java.io.tmpdir"), staging ? "xpipe_ptb_auth" : "xpipe_auth");
    }
}
