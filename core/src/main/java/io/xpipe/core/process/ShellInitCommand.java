package io.xpipe.core.process;

import lombok.NonNull;

import java.util.Optional;

public interface ShellInitCommand {

    default void runDumb(ShellControl shellControl) throws Exception {
        throw new UnsupportedOperationException();
    }

    default Optional<String> terminalContent(ShellControl shellControl) throws Exception {
        throw new UnsupportedOperationException();
    }

    default boolean runInDumb() {
        return false;
    }

    default boolean runInTerminal() {
        return false;
    }

    interface Terminal extends ShellInitCommand {

        Optional<String> terminalContent(ShellControl shellControl);

        default boolean runInTerminal() {
            return true;
        }
    }

    class Simple implements ShellInitCommand {

        @NonNull
        private final String content;

        private final boolean dumb;

        private final boolean terminal;

        public Simple(@NonNull String content, boolean dumb, boolean terminal) {
            this.content = content;
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
        public boolean runInTerminal() {
            return terminal;
        }
    }
}
