package io.xpipe.beacon;

import io.xpipe.core.OsType;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalInt;

@UtilityClass
public class BeaconConfig {

    public static final String BEACON_PORT_PROP = "io.xpipe.beacon.port";
    private static final String PRINT_MESSAGES_PROPERTY = "io.xpipe.beacon.printMessages";
    private static Integer portOverride;

    public static boolean printMessages() {
        if (System.getProperty(PRINT_MESSAGES_PROPERTY) != null) {
            return Boolean.parseBoolean(System.getProperty(PRINT_MESSAGES_PROPERTY));
        }
        return false;
    }

    public static int getUsedPort() {
        if (portOverride != null) {
            return portOverride;
        }

        var beaconPort = System.getenv("BEACON_PORT");
        if (beaconPort != null && !beaconPort.isBlank()) {
            return Integer.parseInt(beaconPort);
        }

        if (System.getProperty(BEACON_PORT_PROP) != null) {
            return Integer.parseInt(System.getProperty(BEACON_PORT_PROP));
        }

        return getDefaultBeaconPort();
    }

    public static OptionalInt fallBackToAnotherPort() {
        var hasEnv = System.getenv("BEACON_PORT") != null || System.getenv("XPIPE_BEACON_PORT") != null;
        if (hasEnv) {
            return OptionalInt.empty();
        }

        var start = 21723;
        for (int i = 0; i < 20; i++) {
            var p = start + i;
            var occupied = BeaconServer.isReachable(p);
            if (!occupied) {
                portOverride = p;
                return OptionalInt.of(p);
            }
        }
        return OptionalInt.empty();
    }

    public static int getDefaultBeaconPort() {
        var customPortVar = System.getenv("XPIPE_BEACON_PORT");
        Integer customPort = null;
        if (customPortVar != null) {
            try {
                customPort = Integer.parseInt(customPortVar);
            } catch (NumberFormatException ignored) {
            }
        }

        var effectivePortBase = customPort != null ? customPort : 21721;

        var staging = Optional.ofNullable(System.getProperty("io.xpipe.app.staging"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        var offset = staging ? 1 : 0;

        return effectivePortBase + offset;
    }

    public static Path getLocalBeaconAuthFile() {
        var staging = Optional.ofNullable(System.getProperty("io.xpipe.app.staging"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        if (OsType.ofLocal() == OsType.LINUX) {
            var name = System.getenv("USER") != null ? System.getenv("USER") : System.getProperty("user.name");
            return Path.of(System.getProperty("java.io.tmpdir"), staging ? "xpipe-ptb" : "xpipe", name, "beacon-auth");
        } else {
            var path = Path.of(System.getProperty("java.io.tmpdir"), staging ? "xpipe-ptb" : "xpipe", "beacon-auth");
            if (path.startsWith(Path.of("C:\\Windows"))) {
                path = Path.of(System.getenv("LOCALAPPDATA"))
                        .resolve("Temp", staging ? "xpipe-ptb" : "xpipe", "beacon-auth");
            }
            return path;
        }
    }

    public static Path getLocalBeaconLockFile() {
        return getLocalBeaconAuthFile().getParent().resolve("lock");
    }
}
