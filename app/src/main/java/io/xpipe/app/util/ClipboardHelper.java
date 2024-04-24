package io.xpipe.app.util;

import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.core.util.SecretValue;
import javafx.animation.PauseTransition;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.util.Duration;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ClipboardHelper {

    public static void copyPassword(SecretValue pass) {
        if (pass == null) {
            return;
        }

        PlatformThread.runLaterIfNeeded(() -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            Map<DataFormat, Object> previous = Stream.of(DataFormat.PLAIN_TEXT, DataFormat.URL, DataFormat.RTF, DataFormat.HTML, DataFormat.IMAGE, DataFormat.FILES)
                    .map(dataFormat -> new AbstractMap.SimpleEntry<>(dataFormat, clipboard.getContent(dataFormat))).filter(o -> o.getValue() != null)
                    .collect(HashMap::new, (m,v)->m.put(v.getKey(), v.getValue()), HashMap::putAll);

            var withPassword = new HashMap<>(previous);
            withPassword.put(DataFormat.PLAIN_TEXT, pass.getSecretValue());
            clipboard.setContent(withPassword);

            var transition = new PauseTransition(Duration.millis(10000));
            transition.setOnFinished(e -> {
                var present = clipboard.getString();
                if (present != null && present.equals(pass.getSecretValue())) {
                    previous.putIfAbsent(DataFormat.PLAIN_TEXT, "");
                    clipboard.setContent(previous);
                }
            });
            transition.play();
        });
    }

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
