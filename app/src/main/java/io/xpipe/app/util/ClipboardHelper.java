package io.xpipe.app.util;

import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;

import java.util.Map;

public class ClipboardHelper {

    public static void copyText(String s) {
        PlatformThread.runLaterIfNeeded(() -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(Map.of(DataFormat.PLAIN_TEXT, s));
        });
    }

    public static void copyUrl(String s) {
        PlatformThread.runLaterIfNeeded(() -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(Map.of(DataFormat.PLAIN_TEXT, s, DataFormat.URL, s));
        });
    }
}
