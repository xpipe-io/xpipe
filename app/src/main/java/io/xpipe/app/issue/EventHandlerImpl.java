package io.xpipe.app.issue;

import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.core.util.Deobfuscator;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.EventHandler;
import io.xpipe.extension.event.TrackEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EventHandlerImpl extends EventHandler {

    private final List<TrackEvent> events = new ArrayList<>();

    public static TrackEvent fromErrorEvent(ErrorEvent ee) {
        var te = TrackEvent.builder();
        var prefix = ee.getDescription() != null ? ee.getDescription() + ":\n" : "";
        var suffix = ee.getThrowable() != null ? Deobfuscator.deobfuscateToString(ee.getThrowable()) : "";
        te.message(prefix + suffix);
        te.type("error");
        te.category("exception");
        te.tag("omitted", ee.isOmitted());
        te.tag("terminal", ee.isTerminal());
        te.elements(ee.getAttachments().stream().map(Path::toString).toList());
        return te.build();
    }

    public List<TrackEvent> snapshotEvents() {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    @Override
    public void handle(TrackEvent te) {
        if (AppLogs.get() != null) {
            AppLogs.get().logEvent(te);
        } else {
            EventHandler.DEFAULT.handle(te);
        }

        synchronized (events) {
            events.add(te);
        }
    }

    private void handleBasic(ErrorEvent ee) {
        if (ee.getDescription() != null) {
            System.err.println(ee.getDescription());
        }
        if (ee.getThrowable() != null) {
            Deobfuscator.printStackTrace(ee.getThrowable());
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
            handleBasic(ee);
            return;
        }

        if (OperationMode.get() == null) {
            handleBasic(ee);
        } else {
            OperationMode.get().getErrorHandler().handle(ee);
        }

        var te = fromErrorEvent(ee);
        synchronized (events) {
            events.add(te);
        }
    }

    @Override
    public void modify(ErrorEvent ee) {
        if (AppLogs.get() != null && AppLogs.get().getSessionLogsDirectory() != null) {
            ee.addAttachment(AppLogs.get().getSessionLogsDirectory());
        }
    }
}
