package io.xpipe.core.process;

import io.xpipe.core.util.SecretValue;
import lombok.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface ShellProcessControl extends ProcessControl {

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
        return executeSimpleCommand(type.switchTo(command));
    }

    default void restart() throws Exception {
        exitAndWait();
        start();
    }

    boolean isLocal();

    int getProcessId();

    OsType getOsType();

    ShellProcessControl elevated(Predicate<ShellProcessControl> elevationFunction);

    ShellProcessControl elevation(SecretValue value);

    ShellProcessControl startTimeout(Integer timeout);

    SecretValue getElevationPassword();

    default ShellProcessControl shell(@NonNull ShellType type) {
        return shell(type.openCommand()).elevation(getElevationPassword());
    }

    default CommandProcessControl command(@NonNull ShellType type, String command) {
        return command(type.switchTo(command));
    }

    default ShellProcessControl shell(@NonNull List<String> command) {
        return shell(
                command.stream().map(s -> s.contains(" ") ? "\"" + s + "\"" : s).collect(Collectors.joining(" ")));
    }

    default ShellProcessControl shell(@NonNull String command) {
        return shell(processControl -> command);
    }

    ShellProcessControl shell(@NonNull Function<ShellProcessControl, String> command);

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
