package io.xpipe.core.process;

import java.util.List;

public interface ShellLaunchCommand {

    List<String> localCommand();

    default String loginCommand() {
        return String.join(" ", loginCommand(null));
    }

    List<String> loginCommand(OsType.Any os);

    List<String> openCommand();

}
