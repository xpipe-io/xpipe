package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.FailableSupplier;

import java.io.IOException;
import java.util.Optional;

public class CommandSupport {

    public static Optional<FilePath> findProgram(ShellControl processControl, String name) throws Exception {
        var out = processControl
                .command(processControl.getShellDialect().getWhichCommand(name))
                .readStdoutIfPossible();
        return out.flatMap(s -> s.lines().findFirst()).map(String::trim).map(FilePath::of);
    }

    public static boolean isInPath(ShellControl processControl, String executable) throws Exception {
        return processControl.executeSimpleBooleanCommand(
                processControl.getShellDialect().getWhichCommand(executable));
    }

    public static void isInPathOrThrow(
            ShellControl processControl, String executable)
            throws Exception {
        isInPathOrThrow(processControl, executable, null);
    }

    public static void isInPathOrThrow(
            ShellControl processControl, String executable, String displayName)
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
        if (!isInPath(processControl, executable)) {
            var prefix = displayName != null ? displayName + " executable " + executable : executable + " executable";
            throw ErrorEvent.expected(new IOException(prefix + " not found in PATH"
                    + (connection != null ? " on system " + connection.getName() : "")));
        }
    }

    public static void isSupported(FailableSupplier<Boolean> supplier, String displayName, DataStoreEntry connection)
            throws Exception {
        if (!supplier.get()) {
            throw ErrorEvent.expected(new IOException(displayName + " is not supported"
                    + (connection != null ? " on system " + connection.getName() : "")));
        }
    }
}
