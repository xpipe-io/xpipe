package io.xpipe.core.process;

import java.io.IOException;

public interface ShellDumbMode {

    default boolean supportsAnyPossibleInteraction() {
        return true;
    }

    default void throwIfUnsupported() {}

    default ShellDialect getSwitchDialect() {
        return null;
    }

    default CommandBuilder prepareInlineDumbCommand(ShellControl self, ShellControl parent, ShellOpenFunction function)
            throws Exception {
        return function.prepareWithoutInitCommand();
    }

    default void prepareDumbInit(ShellControl shellControl) throws Exception {}

    default void prepareDumbExit(ShellControl shellControl) throws IOException {
        shellControl.writeLine("exit");
    }

    class Unsupported implements ShellDumbMode {

        private final String message;

        public Unsupported(String message) {
            this.message = message;
        }

        public void throwIfUnsupported() {
            throw new UnsupportedOperationException(message);
        }

        @Override
        public boolean supportsAnyPossibleInteraction() {
            return false;
        }

        @Override
        public CommandBuilder prepareInlineDumbCommand(
                ShellControl self, ShellControl parent, ShellOpenFunction function) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void prepareDumbInit(ShellControl shellControl) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void prepareDumbExit(ShellControl shellControl) {
            shellControl.kill();
        }
    }
}
