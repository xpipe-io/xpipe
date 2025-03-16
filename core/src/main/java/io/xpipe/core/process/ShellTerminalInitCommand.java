package io.xpipe.core.process;

import lombok.NonNull;

import java.util.Optional;

public interface ShellTerminalInitCommand {

    default void runDumb(ShellControl shellControl) throws Exception {
        throw new UnsupportedOperationException();
    }

    default Optional<String> terminalContent(ShellControl shellControl) throws Exception {
        throw new UnsupportedOperationException();
    }

    default boolean runInDumb() {
        return false;
    }

    boolean canPotentiallyRunInDialect(ShellDialect dialect);

    default boolean runInTerminal() {
        return false;
    }

    interface Terminal extends ShellTerminalInitCommand {

        Optional<String> terminalContent(ShellControl shellControl) throws Exception;

        default boolean runInTerminal() {
            return true;
        }
    }

    class Simple implements ShellTerminalInitCommand {

        @NonNull
        private final String content;

        private final ShellDialect dialect;

        private final boolean dumb;

        private final boolean terminal;

        public Simple(@NonNull String content, ShellDialect dialect, boolean dumb, boolean terminal) {
            this.content = content;
            this.dialect = dialect;
            this.dumb = dumb;
            this.terminal = terminal;
        }

        @Override
        public void runDumb(ShellControl shellControl) throws Exception {
            shellControl.executeSimpleCommand(content);
        }

        @Override
        public Optional<String> terminalContent(ShellControl shellControl) {
            return Optional.of(content);
        }

        @Override
        public boolean runInDumb() {
            return dumb;
        }

        @Override
        public boolean canPotentiallyRunInDialect(ShellDialect dialect) {
            return this.dialect == null || this.dialect.isCompatibleTo(dialect);
        }

        @Override
        public boolean runInTerminal() {
            return terminal;
        }
    }
}
