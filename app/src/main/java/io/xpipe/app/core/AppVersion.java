package io.xpipe.app.core;

import lombok.NonNull;
import lombok.Value;

import java.util.Optional;

@Value
public class AppVersion implements Comparable<AppVersion> {

    public static Optional<AppVersion> parse(String version) {
        try {
            var releaseSplit = version.split("-");
            var split = releaseSplit[0].split("\\.");
            var major = Integer.parseInt(split[0]);
            var minor = split.length > 1 ? Integer.parseInt(split[1]) : 0;
            var patch = split.length > 2 ? Integer.parseInt(split[2]) : 0;
            return Optional.of(new AppVersion(major, minor, patch));
        } catch (Exception ex) {
            // This can happen on number format exceptions
            // It shouldn't happen if the version is correctly formatted though
            return Optional.empty();
        }
    }

    int major;
    int minor;
    int patch;

    public boolean greaterThan(AppVersion other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(@NonNull AppVersion o) {
        var majorCompare = major - o.major;
        if (majorCompare != 0) {
            return majorCompare;
        }

        var minorCompare = minor - o.minor;
        if (minorCompare != 0) {
            return minorCompare;
        }

        return patch - o.patch;
    }
}
