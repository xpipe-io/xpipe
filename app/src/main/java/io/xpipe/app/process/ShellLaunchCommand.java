package io.xpipe.app.process;

import io.xpipe.app.util.OsType;

import java.util.List;

public interface ShellLaunchCommand {

    String inlineCdCommand(OsType.Any os, String cd);

    List<String> localCommand();

    default String loginCommand() {
        return String.join(" ", loginCommand(null));
    }

    List<String> loginCommand(OsType.Any os);
}
