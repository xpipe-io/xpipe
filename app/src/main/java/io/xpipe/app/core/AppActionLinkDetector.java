package io.xpipe.app.core;

import io.xpipe.app.core.window.AppDialog;

import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;

import lombok.Setter;

import java.util.List;

public class AppActionLinkDetector {

    @Setter
    private static String lastDetectedAction;

    private static String getClipboardAction() {
        var content = Clipboard.getSystemClipboard().getContent(DataFormat.URL);
        if (content == null) {
            content = Clipboard.getSystemClipboard().getContent(DataFormat.PLAIN_TEXT);
        }

        return content != null ? content.toString() : null;
    }

    public static void handle(String content, boolean showAlert) {
        var detected = AppOpenArguments.parseActions(content);
        if (detected.size() == 0) {
            return;
        }

        if (showAlert && !showAlert()) {
            return;
        }

        AppOpenArguments.handle(List.of(content));
    }

    public static void detectOnPaste() {
        var content = getClipboardAction();
        if (content == null) {
            return;
        }
        lastDetectedAction = content;
        handle(content, false);
    }

    private static boolean showAlert() {
        return AppDialog.confirm("clipboardActionDetected");
    }
}
