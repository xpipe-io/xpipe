package io.xpipe.app.browser;

import lombok.Value;

@Value
public class BrowserTransferProgress {

    String name;
    long transferred;
    long total;

    public static BrowserTransferProgress empty() {
        return new BrowserTransferProgress(null, 0, 0);
    }

    static BrowserTransferProgress empty(String name, long size) {
        return new BrowserTransferProgress(name, 0, size);
    }

    public static BrowserTransferProgress finished(String name, long size) {
        return new BrowserTransferProgress(name, size, size);
    }

    public boolean done() {
        return transferred >= total;
    }
}
