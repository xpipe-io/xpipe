package io.xpipe.app.browser;

import lombok.Value;

@Value
public class BrowserTransferProgress {

    static BrowserTransferProgress empty() {
        return new BrowserTransferProgress(null, 0, 0);
    }

    static BrowserTransferProgress empty(String name, long size) {
        return new BrowserTransferProgress(name, 0, size);
    }

    static BrowserTransferProgress finished(String name, long size) {
        return new BrowserTransferProgress(name, size, size);
    }

    String name;
    long transferred;
    long total;

    public boolean done() {
        return transferred >= total;
    }
}
