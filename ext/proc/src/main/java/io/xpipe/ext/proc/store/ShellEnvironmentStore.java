package io.xpipe.ext.proc.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellType;
import io.xpipe.core.store.MachineStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.extension.util.Validators;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@Getter
@Jacksonized
@JsonTypeName("shellEnvironment")
public class ShellEnvironmentStore extends JacksonizedValue implements MachineStore {

    private final String commands;
    private final ShellStore host;
    private final ShellType shell;

    public ShellEnvironmentStore(String commands, ShellStore host, ShellType shell) {
        this.commands = commands;
        this.host = host;
        this.shell = shell;
    }

    @Override
    public void checkComplete() throws Exception {
        Validators.nonNull(commands, "Commands");
        Validators.nonNull(host, "Host");
        Validators.namedStoreExists(host, "Host");
        host.checkComplete();
    }

    @Override
    public void validate() throws Exception {
        try (var ignored = create().start()) {}
    }

    @Override
    public ShellProcessControl createControl() {
        var pc = host.create();
        if (shell != null) {
            pc = pc.subShell(shell);
        }
        return pc.initWith(commands.lines().toList());
    }
}
