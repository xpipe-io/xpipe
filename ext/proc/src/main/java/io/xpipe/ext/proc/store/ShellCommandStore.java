package io.xpipe.ext.proc.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellType;
import io.xpipe.core.store.MachineStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.ext.proc.augment.CommandAugmentation;
import io.xpipe.extension.util.Validators;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@Getter
@Jacksonized
@JsonTypeName("shellCommand")
public class ShellCommandStore extends JacksonizedValue implements MachineStore {

    private final String cmd;
    private final ShellStore host;

    public ShellCommandStore(String cmd, ShellStore host) {
        this.cmd = cmd;
        this.host = host;
    }

    public static ShellCommandStore shell(ShellStore host, ShellType type) {
        return ShellCommandStore.builder()
                .host(host)
                .cmd(type.getNormalOpenCommand())
                .build();
    }

    @Override
    public void checkComplete() throws Exception {
        Validators.nonNull(cmd, "Command");
        Validators.nonNull(host, "Host");
        Validators.namedStoreExists(host, "Host");
        host.checkComplete();
    }

    @Override
    public ShellProcessControl createControl() {
        var augmentation = CommandAugmentation.get(getCmd());
        return host.create()
                .subShell(
                        proc -> augmentation.prepareNonTerminalCommand(proc, getCmd()),
                        (proc, s) -> augmentation.prepareTerminalCommand(proc, getCmd(), s));
    }
}
