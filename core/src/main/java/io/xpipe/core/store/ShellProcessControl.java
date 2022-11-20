package io.xpipe.core.store;

import io.xpipe.core.util.SecretValue;
import lombok.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface ShellProcessControl extends ProcessControl {

    ShellProcessControl elevated(Predicate<ShellProcessControl> elevationFunction);

    ShellProcessControl elevation(SecretValue value);

    ShellProcessControl startTimeout(Integer timeout);

    SecretValue getElevationPassword();

    default ShellProcessControl shell(@NonNull ShellType type) {
        return shell(type.openCommand());
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

    default void executeCommand(List<String> command) throws Exception {
        executeCommand(
                command.stream().map(s -> s.contains(" ") ? "\"" + s + "\"" : s).collect(Collectors.joining(" ")));
    }

    @Override
    ShellProcessControl start() throws Exception;

    default CommandProcessControl commandListFunction(Function<ShellProcessControl, List<String>> command) {
        return commandFunction(shellProcessControl -> command.apply(shellProcessControl).stream()
                .map(s -> s.contains(" ") ? "\"" + s + "\"" : s)
                .collect(Collectors.joining(" ")));
    }

    CommandProcessControl commandFunction(Function<ShellProcessControl, String> command);

    CommandProcessControl command(String command);

    default CommandProcessControl command(List<String> command) {
        return command(
                command.stream().map(s -> s.contains(" ") ? "\"" + s + "\"" : s).collect(Collectors.joining(" ")));
    }

    void exit() throws IOException;
}
