package io.xpipe.core.process;

import io.xpipe.core.util.SecretValue;
import lombok.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public interface ShellProcessControl extends ProcessControl {

    default String prepareConsoleOpen(boolean keepOpen) throws Exception {
        return prepareConsoleOpen(null, keepOpen);
    }

    String prepareConsoleOpen(String content, boolean keepOpen) throws Exception;

    default String executeSimpleCommand(String command) throws Exception {
        try (CommandProcessControl c = command(command).start()) {
            return c.readOrThrow();
        }
    }

    default boolean executeBooleanSimpleCommand(String command) throws Exception {
        try (CommandProcessControl c = command(command).start()) {
            return c.discardAndCheckExit();
        }
    }

    default String executeSimpleCommand(ShellType type, String command) throws Exception {
        try (var sub = subShell(type).start()) {
            return sub.executeSimpleCommand(command);
        }
    }

    void restart() throws Exception;

    boolean isLocal();

    int getProcessId();

    OsType getOsType();

    ShellProcessControl elevated(Predicate<ShellProcessControl> elevationFunction);

    ShellProcessControl elevation(SecretValue value);

    ShellProcessControl startTimeout(Integer timeout);

    SecretValue getElevationPassword();

    default ShellProcessControl subShell(@NonNull ShellType type) {
        return subShell(type.openCommand()).elevation(getElevationPassword());
    }

    default ShellProcessControl subShell(@NonNull List<String> command) {
        return subShell(shellProcessControl -> shellProcessControl.getShellType().flatten(command));
    }

    default ShellProcessControl subShell(@NonNull String command) {
        return subShell(processControl -> command);
    }

    ShellProcessControl subShell(@NonNull Function<ShellProcessControl, String> command);

    default ShellProcessControl consoleCommand(@NonNull String command) {
        return consoleCommand(shellProcessControl -> command);
    }

    ShellProcessControl consoleCommand(@NonNull Function<ShellProcessControl, String> command);

    void executeCommand(String command) throws Exception;

    @Override
    ShellProcessControl start() throws Exception;

    default CommandProcessControl commandListFunction(Function<ShellProcessControl, List<String>> command) {
        return commandFunction(shellProcessControl -> shellProcessControl.getShellType().flatten(command.apply(shellProcessControl)));
    }

    CommandProcessControl commandFunction(Function<ShellProcessControl, String> command);

    default CommandProcessControl command(String command){
        return commandFunction(shellProcessControl -> command);
    }

    default CommandProcessControl command(List<String> command) {
        return commandFunction(shellProcessControl -> shellProcessControl.getShellType().flatten(command));
    }

    void exitAndWait() throws IOException;
}
