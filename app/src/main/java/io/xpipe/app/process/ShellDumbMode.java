package io.xpipe.app.process;

import java.io.IOException;

public interface ShellDumbMode {

    default boolean supportsAnyPossibleInteraction() {
        return true;
    }

    default void throwIfUnsupported() {}

    default ShellDialect getSwitchDialect() {
        return null;
    }

    default CommandBuilder prepareInlineDumbCommand(ShellControl self, ShellOpenFunction function) throws Exception {
        return function.prepareWithoutInitCommand();
    }

    default void prepareInlineShellSwitch(ShellControl shellControl) throws Exception {}

    default void prepareDumbInit(ShellControl shellControl) throws Exception {}

    default void prepareDumbExit(ShellControl shellControl) throws IOException {
        shellControl.writeLine(" exit");
    }
}
