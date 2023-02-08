package io.xpipe.ext.proc.augment;

import io.xpipe.core.process.ShellProcessControl;
import org.apache.commons.exec.CommandLine;

import java.util.*;
import java.util.stream.Collectors;

public abstract class CommandAugmentation {

    private static final Set<CommandAugmentation> ALL = new HashSet<>();

    public static CommandAugmentation get(String cmd) {
        var parsed = CommandLine.parse(cmd);
        var executable = parsed.getExecutable().toLowerCase(Locale.ROOT).replaceAll("\\.exe$", "");
        if (ALL.isEmpty()) {
            ALL.addAll(
                    ServiceLoader.load(CommandAugmentation.class.getModule().getLayer(), CommandAugmentation.class)
                            .stream()
                            .map(commandAugmentationProvider -> commandAugmentationProvider.get())
                            .collect(Collectors.toSet()));
        }

        return ALL.stream()
                .filter(commandAugmentation -> commandAugmentation.matches(executable))
                .findFirst()
                .orElse(new NoCommandAugmentation());
    }

    private static List<String> split(String cmd) {
        var parsed = CommandLine.parse(cmd);
        var splitCommand = new ArrayList<>(Arrays.asList(parsed.getArguments()));
        splitCommand.add(0, parsed.getExecutable().replaceAll("\\.exe$", ""));
        return splitCommand;
    }

    public abstract boolean matches(String executable);

    protected void addIfNeeded(List<String> baseCommand, String arg) {
        if (!baseCommand.contains(arg)) {
            baseCommand.add(1, arg);
        }
    }

    protected void remove(List<String> baseCommand, String... args) {
        for (var arg : args) {
            baseCommand.removeIf(s -> s.toLowerCase(Locale.ROOT).equals(arg));
        }
    }

    public String prepareTerminalCommand(ShellProcessControl proc, String cmd, String subCommand) {
        var split = split(cmd);
        prepareBaseCommand(split);
        modifyTerminalCommand(split, subCommand != null);
        return proc.getShellType().flatten(split) + (subCommand != null ? " " + subCommand : "");
    }

    public String prepareNonTerminalCommand(ShellProcessControl proc, String cmd) {
        var split = split(cmd);
        prepareBaseCommand(split);
        modifyNonTerminalCommand(split);
        return proc.getShellType().flatten(split);
    }

    protected abstract void prepareBaseCommand(List<String> baseCommand);

    protected abstract void modifyTerminalCommand(List<String> baseCommand, boolean hasSubCommand);

    protected abstract void modifyNonTerminalCommand(List<String> baseCommand);
}
