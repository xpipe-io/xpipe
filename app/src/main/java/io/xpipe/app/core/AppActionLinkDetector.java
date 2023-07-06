package io.xpipe.app.core;

import io.xpipe.app.launcher.LauncherInput;
import javafx.scene.control.Alert;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;

import java.util.List;

public class AppActionLinkDetector {

    private static String lastDetectedAction;

    private static String getClipboardAction() {
        var content = Clipboard.getSystemClipboard().getContent(DataFormat.URL);
        if (content == null) {
            content = Clipboard.getSystemClipboard().getContent(DataFormat.PLAIN_TEXT);
        }

        return content != null ? content.toString() : null;
    }

    public static void handle(String content, boolean showAlert) {
        var detected = LauncherInput.of(content);
        if (detected.size() == 0) {
            return;
        }

        if (showAlert && !showAlert()) {
            return;
        }

        LauncherInput.handle(List.of(content));
    }

    public static void setLastDetectedAction(String s) {
        lastDetectedAction = s;
    }

    public static void detectOnFocus() {
        var content = getClipboardAction();
        if (content == null) {
            lastDetectedAction = null;
            return;
        }
        if (content.equals(lastDetectedAction)) {
            return;
        }
        lastDetectedAction = content;
        handle(content, true);
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
        var paste = AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                    alert.setTitle(AppI18n.get("clipboardActionDetectedTitle"));
                    alert.setHeaderText(AppI18n.get("clipboardActionDetectedHeader"));
                    alert.getDialogPane()
                            .setContent(
                                    AppWindowHelper.alertContentText(AppI18n.get("clipboardActionDetectedContent")));
                })
                .map(buttonType -> buttonType.getButtonData().isDefaultButton())
                .orElse(false);
        return paste;
    }
}
