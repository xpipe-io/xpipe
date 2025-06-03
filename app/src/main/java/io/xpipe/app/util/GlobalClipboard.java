package io.xpipe.app.util;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileTransferMode;
import io.xpipe.app.browser.file.BrowserLocalFileSystem;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.util.FailableRunnable;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import lombok.SneakyThrows;
import lombok.Value;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GlobalClipboard {

    private static final List<Consumer<Clipboard>> clipboardListeners = new ArrayList<>();

    public static synchronized void addListener(Consumer<Clipboard> listener) {
        clipboardListeners.add(listener);
    }

    public static void init() {
        // Only access from one thread to fix https://bugs.openjdk.org/browse/JDK-8332271
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .addFlavorListener(e -> {
                    // Fix clipboard open issues: https://stackoverflow.com/a/51797746
                    ThreadHelper.sleep(20);

                    var cp = (Clipboard) e.getSource();
                    ThreadHelper.runFailableAsync(() -> {
                        synchronized (GlobalClipboard.class) {
                            for (Consumer<Clipboard> clipboardListener : clipboardListeners) {
                                clipboardListener.accept(cp);
                            }
                        }
                    });
                });
    }
}
