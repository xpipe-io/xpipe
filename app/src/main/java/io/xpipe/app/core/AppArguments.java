package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.LogErrorHandler;
import io.xpipe.core.XPipeDaemonMode;

import lombok.Value;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

@Value
public class AppArguments {

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("^-[DP](.+)=(.+)$");
    List<String> rawArgs;
    List<String> resolvedArgs;
    List<String> openArgs;

    public static AppArguments init(String[] args) {
        var rawArgs = Arrays.asList(args);
        var resolvedArgs = Arrays.asList(parseProperties(args));
        var command = LauncherCommand.resolveLauncher(resolvedArgs.toArray(String[]::new));
        return new AppArguments(rawArgs, resolvedArgs, command.inputs);
    }

    private static String[] parseProperties(String[] args) {
        List<String> newArgs = new ArrayList<>();
        for (var a : args) {
            var m = PROPERTY_PATTERN.matcher(a);
            if (m.matches()) {
                var k = m.group(1);
                var v = m.group(2);
                System.setProperty(k, v);
            } else {
                newArgs.add(a);
            }
        }
        return newArgs.toArray(String[]::new);
    }

    public static class ModeConverter implements CommandLine.ITypeConverter<XPipeDaemonMode> {

        @Override
        public XPipeDaemonMode convert(String value) {
            return XPipeDaemonMode.get(value);
        }
    }

    @CommandLine.Command()
    public static class LauncherCommand implements Callable<Integer> {

        @CommandLine.Parameters(paramLabel = "<input>")
        final List<String> inputs = List.of();

        public static LauncherCommand resolveLauncher(String[] args) {
            var cmd = new CommandLine(new LauncherCommand());
            cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                var event = ErrorEventFactory.fromThrowable(ex).term().build();
                // Print error in case we launched from the command-line
                new LogErrorHandler().handle(event);
                event.handle();
                return 1;
            });
            cmd.setParameterExceptionHandler((ex, args1) -> {
                var event =
                        ErrorEventFactory.fromThrowable(ex).term().expected().build();
                // Print error in case we launched from the command-line
                new LogErrorHandler().handle(event);
                event.handle();
                return 1;
            });

            if (AppLogs.get() != null) {
                // Use original output streams for command output
                cmd.setOut(new PrintWriter(AppLogs.get().getOriginalSysOut()));
                cmd.setErr(new PrintWriter(AppLogs.get().getOriginalSysErr()));
            }

            try {
                cmd.parseArgs(args);
            } catch (Throwable t) {
                // Fix serialization issues with exception class
                var converted = t instanceof CommandLine.UnmatchedArgumentException u
                        ? new IllegalArgumentException(u.getMessage())
                        : t;
                var e = ErrorEventFactory.fromThrowable(converted)
                        .expected()
                        .term()
                        .build();
                // Print error in case we launched from the command-line
                new LogErrorHandler().handle(e);
                e.handle();
            }

            return cmd.getCommand();
        }

        @Override
        public Integer call() {
            return 0;
        }
    }
}
