package io.xpipe.app.browser.file;

import io.xpipe.app.storage.DataStoreEntry;

import java.io.OutputStream;
import java.util.Optional;

public interface BrowserFileOutput {

    static BrowserFileOutput none() {
        return new BrowserFileOutput() {

            @Override
            public Optional<DataStoreEntry> target() {
                return Optional.empty();
            }

            @Override
            public boolean hasOutput() {
                return false;
            }

            @Override
            public OutputStream open() {
                return null;
            }

            @Override
            public void onFinish() throws Exception {

            }
        };
    }

    Optional<DataStoreEntry> target();

    boolean hasOutput();

    OutputStream open() throws Exception;

    void onFinish() throws Exception;
}
