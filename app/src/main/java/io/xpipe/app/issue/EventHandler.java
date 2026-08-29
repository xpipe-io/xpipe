package io.xpipe.app.issue;

import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.AppOperationMode;

public class EventHandler {

    private static final EventHandler INSTANCE = new EventHandler();

    public static EventHandler get() {
        return INSTANCE;
    }

    public void handle(TrackEvent te) {
        if (AppLogs.get() != null) {
            AppLogs.get().logEvent(te);
        } else {
            System.out.println(te);
            System.out.flush();
        }
    }

    public void handle(ErrorEvent ee) {
        if ((AppProperties.get() != null && AppProperties.get().isAotTrainMode())
                || (AppProperties.get() != null && AppProperties.get().isCli())) {
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

    public void modify(ErrorEvent ee) {
        if (AppLogs.get() != null && AppLogs.get().getSessionLogsDirectory() != null) {
            ee.addAttachment(AppLogs.get().getSessionLogsDirectory());
        }
    }

    private void handleOnShutdown(ErrorEvent ee) {
        ErrorAction.ignore().handle(ee);
        handle(TrackEvent.fromErrorEvent(ee));
    }
}
