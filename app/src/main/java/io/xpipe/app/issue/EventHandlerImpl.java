package io.xpipe.app.issue;

import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.core.util.Deobfuscator;

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
        if (ee.isTerminal()) {
            new TerminalErrorHandler().handle(ee);
            return;
        }

        // Don't block shutdown
        if (OperationMode.isInShutdown()) {
            SentryErrorHandler.getInstance().handle(ee);
            handle(fromErrorEvent(ee));
            return;
        }

        if (OperationMode.get() == null) {
            OperationMode.BACKGROUND.getErrorHandler().handle(ee);
        } else {
            OperationMode.get().getErrorHandler().handle(ee);
        }
    }

    @Override
    public void modify(ErrorEvent ee) {
        if (AppLogs.get() != null && AppLogs.get().getSessionLogsDirectory() != null) {
            ee.addAttachment(AppLogs.get().getSessionLogsDirectory());
        }
    }
}
