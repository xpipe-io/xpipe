package io.xpipe.app.util;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.core.process.CommandControl;
import io.xpipe.core.process.ProcessOutputException;

import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CommandDialog {

    public static void runAsyncAndShow(Map<String, CommandControl> cmds) {
        ThreadHelper.runAsync(() -> {
            StringBuilder acc = new StringBuilder();
            for (var e : cmds.entrySet()) {
                String out;
                try {
                    out = e.getValue().readStdoutOrThrow();
                    out = formatOutput(out);
                } catch (ProcessOutputException ex) {
                    out = ex.getMessage();
                } catch (Throwable t) {
                    out = ExceptionUtils.getStackTrace(t);
                }

                acc.append(e.getKey()).append(" (exit code ").append(e.getValue().getExitCode()).append("):\n").append(out).append("\n\n");
            }
            show(acc.toString());
        });
    }

    public static void runAsyncAndShow(CommandControl cmd) {
        ThreadHelper.runAsync(() -> {
            String out;
            try {
                out = cmd.readStdoutOrThrow();
                out = formatOutput(out);
            } catch (ProcessOutputException e) {
                out = e.getMessage();
            } catch (Throwable t) {
                out = ExceptionUtils.getStackTrace(t);
            }
            show(out);
        });
    }

    private static void show(String out) {
        var modal = ModalOverlay.of(
                "commandOutput",
                Comp.of(() -> {
                            var text = new TextArea(out);
                            text.setWrapText(true);
                            text.setEditable(false);
                            text.setPrefRowCount(
                                    Math.max(8, (int) out.lines().count()));
                            var sp = new StackPane(text);
                            return sp;
                        })
                        .prefWidth(650));
        modal.show();
    }

    private static String formatOutput(String out) {
        if (out.isEmpty()) {
            out = "<empty>";
        }

        if (out.length() > 10000) {
            var counter = new AtomicInteger();
            var start = out.lines()
                    .filter(s -> {
                        counter.incrementAndGet();
                        return true;
                    })
                    .limit(100)
                    .collect(Collectors.joining("\n"));
            var notShownLines = counter.get() - 100;
            if (notShownLines > 0) {
                out = start + "\n\n... " + notShownLines + " more lines";
            } else {
                out = start;
            }
        }

        return out;
    }
}
