package io.xpipe.app.issue;

import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.util.Deobfuscator;

import java.nio.file.Path;

public class EventHandlerImpl extends EventHandler {

    public static TrackEvent fromErrorEvent(ErrorEvent ee) {
        var te = TrackEvent.builder();
        var prefix = ee.getDescription() != null ? ee.getDescription() + ":\n" : "";
        var suffix = ee.getThrowable() != null ? Deobfuscator.deobfuscateToString(ee.getThrowable()) : "";
        te.message(prefix + suffix);
        te.type("error");
        te.tag("omitted", ee.isOmitted());
        te.tag("terminal", ee.isTerminal());
        te.elements(ee.getAttachments().stream().map(Path::toString).toList());
        return te.build();
    }

    @Override
    public void handle(TrackEvent te) {
        if (AppLogs.get() != null) {
            AppLogs.get().logEvent(te);
        } else {
            System.out.println(te);
            System.out.flush();
        }
    }

    @Override
    public void handle(ErrorEvent ee) {
        if (AppProperties.get() != null && AppProperties.get().isAotTrainMode()) {
            new LogErrorHandler().handle(ee);
            if (ee.isTerminal()) {
                AppOperationMode.halt(1);
            }
            return;
        }

        if (ee.isTerminal()) {
            new TerminalErrorHandler().handle(ee);
            return;
        }

        // Don't block shutdown
        if (AppOperationMode.isInShutdown()) {
            handleOnShutdown(ee);
            return;
        }

        if (AppOperationMode.get() == null) {
            AppOperationMode.BACKGROUND.getErrorHandler().handle(ee);
        } else {
            AppOperationMode.get().getErrorHandler().handle(ee);
        }
    }

    @Override
    public void modify(ErrorEvent ee) {
        if (AppLogs.get() != null && AppLogs.get().getSessionLogsDirectory() != null) {
            ee.addAttachment(AppLogs.get().getSessionLogsDirectory());
        }
    }

    private void handleOnShutdown(ErrorEvent ee) {
        ErrorAction.ignore().handle(ee);
        handle(fromErrorEvent(ee));
    }
}
