package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.ContextualFileReference;
import io.xpipe.core.store.ShellStore;
import lombok.EqualsAndHashCode;
import lombok.Value;

public abstract class StringSource {

    public abstract String get() throws Exception;

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class InPlace extends StringSource {

        String value;

        @Override
        public String get() {
            return value;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class File extends StringSource {

        ShellStore host;
        ContextualFileReference file;

        @Override
        public String get() throws Exception {
            if (host == null || file == null) {
                return "";
            }

            try (var sc = host.control().start()) {
                var path = file.toAbsoluteFilePath(sc);
                if (!sc.getShellDialect().createFileExistsCommand(sc, path).executeAndCheck()) {
                    throw ErrorEvent.expected(
                            new IllegalArgumentException("File " + path + " does not exist"));
                }

                var abs = file.toAbsoluteFilePath(sc);
                var content = sc.getShellDialect().getFileReadCommand(sc, abs).readStdoutOrThrow();
                return content;
            }
        }
    }
}
