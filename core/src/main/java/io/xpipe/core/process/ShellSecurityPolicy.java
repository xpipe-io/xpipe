package io.xpipe.core.process;

public interface ShellSecurityPolicy {

    boolean checkElevate(ShellControl shellControl);

    default void elevateOrThrow(ShellControl shellControl) {
        if (!checkElevate(shellControl)) {
            throw new UnsupportedOperationException("Elevation is not allowed for this system");
        }
    }

    boolean permitTempScriptCreation();
}
