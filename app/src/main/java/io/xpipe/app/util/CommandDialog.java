package io.xpipe.app.util;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.process.CommandControl;
import io.xpipe.app.process.ProcessOutputException;

import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;

import lombok.Value;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CommandDialog {

    @Value
    public static class CommandEntry {

        String name;
        CommandControl command;
    }

    public static void runMultipleAndShow(List<CommandEntry> cmds) {
        var parts = new String[cmds.size()];
        var latch = new CountDownLatch(parts.length);
        for (int i = 0; i < cmds.size(); i++) {
            var e = cmds.get(i);
            var ii = i;
            ThreadHelper.runAsync(() -> {
                String out;
                try {
                    out = e.getCommand().readStdoutOrThrow();
                    out = formatOutput(out);
                } catch (ProcessOutputException ex) {
                    out = ex.getMessage();
                } catch (Throwable t) {
                    out = ExceptionUtils.getStackTrace(t);
                }

                var s = e.getName() + " (exit code " + e.getCommand().getExitCode() + "):\n" + out;
                parts[ii] = s;
                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException ignored) {}

        var joined = String.join("\n\n", parts);
        show(joined);
    }

    public static void runAndShow(CommandControl cmd) {
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
    }

    private static void show(String out) {
        var modal = ModalOverlay.of(
                "commandOutput",
                RegionBuilder.of(() -> {
                            var text = new TextArea(out);
                            text.setWrapText(true);
                            text.setEditable(false);
                            text.setPrefRowCount(Math.max(8, (int) out.lines().count()));
                            var sp = new StackPane(text);
                            return sp;
                        })
                        .prefWidth(650));
        modal.show();
    }

    public static String formatOutput(String out) {
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
