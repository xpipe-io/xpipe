package io.xpipe.app.browser.file;

import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Value
public class BrowserTransferProgress {

    String name;
    long transferred;
    long total;
    Instant timestamp = Instant.now();

    public static BrowserTransferProgress finished(String name, long size) {
        return new BrowserTransferProgress(name, size, size);
    }

    public static long estimateTransferSpeed(BrowserTransferProgress start, BrowserTransferProgress end) {
        var diff = end.transferred - start.transferred;
        var duration = Duration.between(start.timestamp, end.timestamp);
        return (long) (diff / (duration.toMillis() / 1000.0));
    }

    public static long estimateTransferSpeed(List<BrowserTransferProgress> list, BrowserTransferProgress now) {
        if (list.isEmpty()) {
            return 0;
        }

        var rSize = list.size() > 1 ? list.size() - 1 : list.size();
        var r = new double[rSize];
        for (int i = 0; i < rSize; i++) {
            r[i] = estimateTransferSpeed(list.get(i), now);
        }

        double sum = 0;
        var lookBack = Math.min(r.length, 5);
        for (int i = 0; i < lookBack; i++) {
            sum += r[r.length - i - 1];
        }
        return (long) (sum / lookBack);
    }

    public boolean done() {
        return transferred >= total;
    }
}
