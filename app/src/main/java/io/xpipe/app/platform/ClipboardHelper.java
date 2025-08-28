package io.xpipe.app.platform;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.core.SecretValue;

import javafx.animation.PauseTransition;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.util.Duration;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ClipboardHelper {

    private static final AppLayoutModel.QueueEntry COPY_QUEUE_ENTRY = new AppLayoutModel.QueueEntry(
            AppI18n.observable("passwordCopied"),
            new LabelGraphic.IconGraphic("mdi2c-clipboard-check-outline"),
            () -> {});

    private static void apply(Map<DataFormat, Object> map, boolean showNotification) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        Map<DataFormat, Object> contents = Stream.of(
                        DataFormat.PLAIN_TEXT,
                        DataFormat.URL,
                        DataFormat.RTF,
                        DataFormat.HTML,
                        DataFormat.IMAGE,
                        DataFormat.FILES)
                .map(dataFormat -> {
                    try {
                        // This can fail if the clipboard data is invalid
                        return new AbstractMap.SimpleEntry<>(dataFormat, clipboard.getContent(dataFormat));
                    } catch (Exception e) {
                        return new AbstractMap.SimpleEntry<>(dataFormat, null);
                    }
                })
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
        contents.putAll(map);
        contents.entrySet().removeIf(e -> e.getValue() == null);
        clipboard.setContent(contents);

        if (showNotification) {
            AppLayoutModel.get().showQueueEntry(COPY_QUEUE_ENTRY, java.time.Duration.ofSeconds(15), true);
        }
    }

    public static void copyPassword(SecretValue pass) {
        if (pass == null) {
            return;
        }

        PlatformThread.runLaterIfNeeded(() -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            var text = clipboard.getString();

            apply(Map.of(DataFormat.PLAIN_TEXT, pass.getSecretValue()), true);

            var transition = new PauseTransition(Duration.millis(15000));
            transition.setOnFinished(e -> {
                var present = clipboard.getString();
                if (present != null && present.equals(pass.getSecretValue())) {
                    var map = new HashMap<DataFormat, Object>();
                    map.put(DataFormat.PLAIN_TEXT, text);
                    apply(map, false);
                }
            });
            transition.play();
        });
    }

    public static void copyText(String s) {
        PlatformThread.runLaterIfNeeded(() -> {
            apply(Map.of(DataFormat.PLAIN_TEXT, s), true);
        });
    }

    public static void copyUrl(String s) {
        PlatformThread.runLaterIfNeeded(() -> {
            apply(Map.of(DataFormat.URL, s, DataFormat.PLAIN_TEXT, s), true);
        });
    }
}
