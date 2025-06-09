package io.xpipe.app.util;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GlobalClipboard {

    private static final List<Consumer<Clipboard>> clipboardListeners = new ArrayList<>();

    public static synchronized void addListener(Consumer<Clipboard> listener) {
        clipboardListeners.add(listener);
    }

    public static void init() {
        // Only access from one thread to fix https://bugs.openjdk.org/browse/JDK-8332271
        Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(e -> {
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
