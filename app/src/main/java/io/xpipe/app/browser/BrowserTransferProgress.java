package io.xpipe.app.browser;

import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Value
public class BrowserTransferProgress {

    String name;
    long transferred;
    long total;
    Instant start;

    public static BrowserTransferProgress empty() {
        return new BrowserTransferProgress(null, 0, 0, Instant.now());
    }

    static BrowserTransferProgress empty(String name, long size) {
        return new BrowserTransferProgress(name, 0, size, Instant.now());
    }

    public static BrowserTransferProgress finished(String name, long size) {
        return new BrowserTransferProgress(name, size, size, Instant.now());
    }

    public boolean done() {
        return transferred >= total;
    }

    public Duration elapsedTime() {
        var now = Instant.now();
        var elapsed = Duration.between(start,now);
        return elapsed;
    }

    public Duration expectedTimeRemaining() {
        var elapsed = elapsedTime();
        var share = (double) transferred / total;
        var rest = 1.0 - share;
        var restMillis = (long) (elapsed.toMillis() * rest);
        return Duration.of(restMillis, ChronoUnit.MILLIS);
    }
}
