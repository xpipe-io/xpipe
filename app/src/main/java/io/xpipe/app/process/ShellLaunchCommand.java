package io.xpipe.app.process;

import io.xpipe.core.OsType;

import java.util.List;

public interface ShellLaunchCommand {

    String inlineCdCommand(String cd);

    List<String> localCommand();

    default String loginCommand() {
        return String.join(" ", loginCommand(null));
    }

    List<String> loginCommand(OsType.Any os);
}
