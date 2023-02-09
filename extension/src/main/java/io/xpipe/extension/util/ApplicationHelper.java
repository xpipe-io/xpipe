package io.xpipe.extension.util;

import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.event.TrackEvent;

import java.io.IOException;
import java.util.List;

public class ApplicationHelper {

    public static void executeLocalApplication(String s) throws Exception {
        var args = ShellTypes.getPlatformDefault().executeCommandListWithShell(s);
        TrackEvent.withDebug("proc", "Executing local application")
                .elements(args)
                .handle();
        try (var c = ShellStore.local().create().command(s).start()) {
            c.discardOrThrow();
        }
    }

    public static void executeLocalApplication(List<String> s) throws Exception {
        var args = ShellTypes.getPlatformDefault().executeCommandListWithShell(s);
        TrackEvent.withDebug("proc", "Executing local application")
                .elements(args)
                .handle();
        try (var c = ShellStore.local().create().command(s).start()) {
            c.discardOrThrow();
        }
    }

    public static boolean isInPath(ShellProcessControl processControl, String executable) throws Exception {
        return processControl.executeBooleanSimpleCommand(
                processControl.getShellType().getWhichCommand(executable));
    }

    public static void checkSupport(ShellProcessControl processControl, String executable, String displayName)
            throws Exception {
        if (!isInPath(processControl, executable)) {
            throw new IOException(displayName + " executable " + executable + " not found in PATH");
        }
    }
}
