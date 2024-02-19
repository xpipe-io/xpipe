package io.xpipe.core.process;

import io.xpipe.core.store.ShellStore;
import io.xpipe.core.store.StatefulDataStore;
import io.xpipe.core.util.FailableConsumer;
import io.xpipe.core.util.FailableFunction;
import lombok.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ShellControl extends ProcessControl {

    UUID getElevationSecretId();

    void setParentSystemAccess(ParentSystemAccess access);

    List<UUID> getExitUuids();

    Optional<ShellStore> getSourceStore();

    ShellControl withSourceStore(ShellStore store);

    List<ScriptSnippet> getInitCommands();

    ParentSystemAccess getParentSystemAccess();

    ParentSystemAccess getLocalSystemAccess();

    boolean isLocal();

    ShellControl getMachineRootSession();

    ShellControl withoutLicenseCheck();

    String getOsName();

    boolean isLicenseCheck();

    ReentrantLock getLock();

    void setOriginalShellDialect(ShellDialect dialect);

    ShellDialect getOriginalShellDialect();

    ShellControl onInit(FailableConsumer<ShellControl, Exception> pc);

    default <T extends ShellStoreState> ShellControl withShellStateInit(StatefulDataStore<T> store) {
        return onInit(shellControl -> {
            var s = store.getState();
            s.setOsType(shellControl.getOsType());
            s.setShellDialect(shellControl.getOriginalShellDialect());
            s.setRunning(true);
            s.setOsName(shellControl.getOsName());
            store.setState(s);
        });
    }

    default <T extends ShellStoreState> ShellControl withShellStateFail(StatefulDataStore<T> store) {
        return onFail(shellControl -> {
            var s = store.getState();
            s.setRunning(false);
            store.setState(s);
        });
    }

    ShellControl onExit(Consumer<ShellControl> pc);

    ShellControl onFail(Consumer<Throwable> t);

    ShellControl withExceptionConverter(ExceptionConverter converter);

    ShellControl withErrorFormatter(Function<String, String> formatter);

    String prepareIntermediateTerminalOpen(String content, TerminalInitScriptConfig config, FailableFunction<ShellControl, String, Exception> workingDirectory) throws Exception;

    String getSystemTemporaryDirectory();

    default CommandControl osascriptCommand(String script) {
        return command(String.format(
                """
                osascript - "$@" <<EOF
                %s
                EOF
                """,
                script));
    }

    default byte[] executeSimpleRawBytesCommand(String command) throws Exception {
        try (CommandControl c = command(command).start()) {
            return c.readRawBytesOrThrow();
        }
    }

    default String executeSimpleStringCommand(String command) throws Exception {
        try (CommandControl c = command(command).start()) {
            return c.readStdoutOrThrow();
        }
    }

    default Optional<String> executeSimpleStringCommandAndCheck(String command) throws Exception {
        try (CommandControl c = command(command).start()) {
            var out = c.readStdoutDiscardErr();
            return c.getExitCode() == 0 ? Optional.of(out) : Optional.empty();
        }
    }

    default boolean executeSimpleBooleanCommand(String command) throws Exception {
        try (CommandControl c = command(command).start()) {
            return c.discardAndCheckExit();
        }
    }

    default void executeSimpleCommand(CommandBuilder command) throws Exception {
        try (CommandControl c = command(command).start()) {
            c.discardOrThrow();
        }
    }

    default void executeSimpleCommand(String command) throws Exception {
        try (CommandControl c = command(command).start()) {
            c.discardOrThrow();
        }
    }

    default void executeSimpleCommand(String command, String failMessage) throws Exception {
        try (CommandControl c = command(command).start()) {
            c.discardOrThrow();
        } catch (ProcessOutputException out) {
            throw ProcessOutputException.withPrefix(failMessage, out);
        }
    }

    default String executeSimpleStringCommand(ShellDialect type, String command) throws Exception {
        try (var sub = subShell(type).start()) {
            return sub.executeSimpleStringCommand(command);
        }
    }

    ShellControl withSecurityPolicy(ShellSecurityPolicy policy);

    ShellSecurityPolicy getEffectiveSecurityPolicy();

    String buildElevatedCommand(CommandConfiguration input, String prefix, UUID requestId, CountDown countDown) throws Exception;

    void restart() throws Exception;

    OsType.Any getOsType();

    ShellControl elevated(String message, FailableFunction<ShellControl, Boolean, Exception> elevationFunction);

    ShellControl withInitSnippet(ScriptSnippet snippet);

    default ShellControl subShell(@NonNull ShellDialect type) {
        var o = new ShellOpenFunction() {

            @Override
            public CommandBuilder prepareWithoutInitCommand() {
                return CommandBuilder.of().add(sc -> type.getLoginOpenCommand(sc));
            }

            @Override
            public CommandBuilder prepareWithInitCommand(@NonNull String command) {
                return CommandBuilder.ofString(command);
            }
        };
        var s = singularSubShell(o);
        s.setParentSystemAccess(ParentSystemAccess.identity());
        return s;
    }

    default ShellControl identicalSubShell() {
        var o = new ShellOpenFunction() {

            @Override
            public CommandBuilder prepareWithoutInitCommand() {
                return CommandBuilder.of().add(sc -> sc.getShellDialect().getLoginOpenCommand(sc));
            }

            @Override
            public CommandBuilder prepareWithInitCommand(@NonNull String command) {
                return CommandBuilder.ofString(command);
            }
        };
        return singularSubShell(o);
    }

    default <T> T enforceDialect(@NonNull ShellDialect type, FailableFunction<ShellControl, T, Exception> sc) throws Exception {
        if (isRunning() && getShellDialect().equals(type)) {
            return sc.apply(this);
        } else {
            try (var sub = subShell(type).start()) {
                return sc.apply(sub);
            }
        }
    }

    ShellControl subShell(
            ShellOpenFunction command, ShellOpenFunction terminalCommand);

    ShellControl singularSubShell(ShellOpenFunction command);

    void writeLineAndReadEcho(String command) throws Exception;

    void writeLineAndReadEcho(String command, boolean log) throws Exception;

    void cd(String directory) throws Exception;

    @Override
    ShellControl start();

    default CommandControl command(String command) {
        return command(CommandBuilder.ofFunction(shellProcessControl -> command));
    }

    default CommandControl command(Consumer<CommandBuilder> builder) {
        var b = CommandBuilder.of();
        builder.accept(b);
        return command(b);
    }

    default CommandControl command(CommandBuilder builder) {
        var sc = ProcessControlProvider.get().command(this, builder, builder);
        return sc;
    }

    void exitAndWait() throws IOException;
}
