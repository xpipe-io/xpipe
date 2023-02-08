package io.xpipe.core.process;

import io.xpipe.core.util.SecretValue;
import lombok.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface ShellProcessControl extends ProcessControl {

    void onInit(Consumer<ShellProcessControl> pc);

    String prepareTerminalOpen() throws Exception;

    String prepareIntermediateTerminalOpen(String content) throws Exception;

    String getTemporaryDirectory() throws Exception;

    public void checkRunning() throws Exception;


    default String executeStringSimpleCommand(String command) throws Exception {
        try (CommandProcessControl c = command(command).start()) {
            return c.readOrThrow();
        }
    }

    default boolean executeBooleanSimpleCommand(String command) throws Exception {
        try (CommandProcessControl c = command(command).start()) {
            return c.discardAndCheckExit();
        }
    }

    default void executeSimpleCommand(String command) throws Exception {
        try (CommandProcessControl c = command(command).start()) {
            c.discardOrThrow();
        }
    }

    default void executeSimpleCommand(String command, String failMessage) throws Exception {
        try (CommandProcessControl c = command(command).start()) {
            c.discardOrThrow();
        } catch (ProcessOutputException out) {
            var message = out.getMessage();
            throw new ProcessOutputException(message != null ? failMessage + ": " + message : failMessage);
        }
    }

    default String executeStringSimpleCommand(ShellType type, String command) throws Exception {
        try (var sub = subShell(type).start()) {
            return sub.executeStringSimpleCommand(command);
        }
    }

    void restart() throws Exception;

    boolean isLocal();

    OsType getOsType();

    ShellProcessControl elevated(Predicate<ShellProcessControl> elevationFunction);

    ShellProcessControl elevation(SecretValue value);

    ShellProcessControl initWith(List<String> cmds);

    SecretValue getElevationPassword();

    default ShellProcessControl subShell(@NonNull ShellType type) {
        return subShell(p -> type.getNormalOpenCommand(), (shellProcessControl, s) -> {
                    return s == null ? type.getNormalOpenCommand() : type.executeCommandWithShell(s);
                })
                .elevation(getElevationPassword());
    }

    default ShellProcessControl subShell(@NonNull List<String> command) {
        return subShell(
                shellProcessControl -> shellProcessControl.getShellType().flatten(command), null);
    }

    default ShellProcessControl subShell(@NonNull String command) {
        return subShell(processControl -> command, null);
    }

    ShellProcessControl subShell(
            @NonNull Function<ShellProcessControl, String> command,
            BiFunction<ShellProcessControl, String, String> terminalCommand);

    void executeLine(String command) throws Exception;

    @Override
    ShellProcessControl start() throws Exception;

    CommandProcessControl command(Function<ShellProcessControl, String> command);

    CommandProcessControl command(
            Function<ShellProcessControl, String> command, Function<ShellProcessControl, String> terminalCommand);

    default CommandProcessControl command(String command) {
        return command(shellProcessControl -> command);
    }

    default CommandProcessControl command(List<String> command) {
        return command(shellProcessControl -> shellProcessControl.getShellType().flatten(command));
    }

    void exitAndWait() throws IOException;
}
