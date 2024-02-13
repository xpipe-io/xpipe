package io.xpipe.core.process;

import java.io.IOException;

public interface ShellDumbMode {

    default boolean supportsAnyPossibleInteraction() {
        return true;
    }

    default ShellDialect getSwitchDialect() {
        return null;
    }

    default String getUnsupportedMessage() {
        return null;
    }

    default CommandBuilder prepareInlineDumbCommand(ShellControl self, ShellControl parent, ShellOpenFunction function) throws Exception {
        return function.prepareWithoutInitCommand();
    }

    default void prepareDumbInit(ShellControl shellControl) throws Exception {}

    default void prepareDumbExit(ShellControl shellControl) throws IOException {
        shellControl.writeLine("exit");
    }

    class Adjusted implements ShellDumbMode {

        private final ShellDialect replacementDialect;

        public Adjusted(ShellDialect replacementDialect) {
            this.replacementDialect = replacementDialect;
        }

        @Override
        public CommandBuilder prepareInlineDumbCommand(ShellControl self, ShellControl parent, ShellOpenFunction function) throws Exception {
            return function.prepareWithInitCommand(replacementDialect.getLoginOpenCommand(null));
        }
    }

    class Unsupported implements ShellDumbMode {

        private final String message;

        public Unsupported(String message) {this.message = message;}

        @Override
        public boolean supportsAnyPossibleInteraction() {
            return false;
        }

        @Override
        public void prepareDumbInit(ShellControl shellControl) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public void prepareDumbExit(ShellControl shellControl) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public CommandBuilder prepareInlineDumbCommand(ShellControl self, ShellControl parent, ShellOpenFunction function) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getUnsupportedMessage() {
            return message;
        }
    }
}
