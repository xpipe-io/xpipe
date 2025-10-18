package io.xpipe.app.process;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.FailableSupplier;

import java.io.IOException;

public class CommandSupport {

    public static void isInPathOrThrow(ShellControl processControl, String executable) throws Exception {
        isInPathOrThrow(processControl, executable, null);
    }

    public static void isInPathOrThrow(ShellControl processControl, String executable, String displayName)
            throws Exception {
        var source = processControl.getSourceStoreId();
        if (source.isPresent()) {
            var entry = DataStorage.get().getStoreEntryIfPresent(source.get());
            if (entry.isPresent()) {
                isInPathOrThrow(processControl, executable, displayName, entry.get());
                return;
            }
        }
        isInPathOrThrow(processControl, executable, displayName, null);
    }

    public static void isInPathOrThrow(
            ShellControl processControl, String executable, String displayName, DataStoreEntry connection)
            throws Exception {
        if (!processControl.view().findProgram(executable).isPresent()) {
            var prefix = displayName != null ? displayName + " executable " + executable : executable + " executable";
            throw ErrorEventFactory.expected(new IOException(
                    prefix + " not found in PATH" + (connection != null ? " on system " + connection.getName() : "")));
        }
    }

    public static void isSupported(FailableSupplier<Boolean> supplier, String displayName, DataStoreEntry connection)
            throws Exception {
        if (!supplier.get()) {
            throw ErrorEventFactory.expected(new IOException(displayName + " is not supported"
                    + (connection != null ? " on system " + connection.getName() : "")));
        }
    }

    public static boolean isInLocalPath(String executable) throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            return sc.view().findProgram(executable).isPresent();
        }
    }

    public static void isInLocalPathOrThrow(String displayName, String executable) throws Exception {
        var present = isInLocalPath(executable);
        var prefix = displayName != null
                ? displayName + " executable \"" + executable + "\""
                : "\"" + executable + "\" executable";
        if (present) {
            return;
        }
        throw ErrorEventFactory.expected(
                new IOException(
                        prefix
                                + " not found in PATH. Install the executable, add it to the PATH, and refresh the environment by restarting XPipe to fix this."));
    }
}
