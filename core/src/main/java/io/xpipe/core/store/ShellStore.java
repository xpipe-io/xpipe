package io.xpipe.core.store;

import java.util.List;

public interface ShellStore extends MachineFileStore {

    static ShellStore local() {
        return new LocalStore();
    }

    default ProcessBuilder prepare(List<String> cmd) throws Exception {
        var toExec = createCommand(cmd);
        return new ProcessBuilder(toExec);
    }

    String executeAndRead(List<String> cmd) throws Exception;

    List<String> createCommand(List<String> cmd);
}
