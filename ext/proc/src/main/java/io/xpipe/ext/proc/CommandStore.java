package io.xpipe.ext.proc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.ShellType;
import io.xpipe.core.store.CommandExecutionStore;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.ext.proc.augment.CommandAugmentation;
import io.xpipe.extension.util.Validators;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.InputStream;
import java.io.OutputStream;

@JsonTypeName("cmd")
@SuperBuilder
@Jacksonized
@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CommandStore extends JacksonizedValue implements StreamDataStore, CommandExecutionStore {

    String cmd;

    ShellStore host;

    ShellType shell;

    DataFlow flow;

    boolean requiresElevation;

    public DataFlow getFlow() {
        return flow;
    }

    @Override
    public void validate() throws Exception {
        host.validate();
    }

    @Override
    public void checkComplete() throws Exception {
        Validators.nonNull(cmd, "Command");
        Validators.nonNull(host, "Host");
        host.checkComplete();
        Validators.nonNull(flow, "Flow");
    }

    @Override
    public InputStream openInput() throws Exception {
        if (!flow.hasInput()) {
            throw new UnsupportedOperationException();
        }

        var cmd = create().start();
        return cmd.getStdout();
    }

    @Override
    public OutputStream openOutput() throws Exception {
        if (!flow.hasOutput()) {
            throw new UnsupportedOperationException();
        }

        var cmd = create().start();
        return cmd.getStdin();
    }

    @Override
    public CommandProcessControl create() throws Exception {
        var augmentation = CommandAugmentation.get(getCmd());
        var base = shell != null ? host.create().subShell(shell) : host.create();
        var command = base.command(
                        proc -> augmentation.prepareNonTerminalCommand(proc, getCmd()),
                        proc -> augmentation.prepareTerminalCommand(proc, getCmd(), null))
                .complex();
        if (requiresElevation) {
            command = command.elevated();
        }
        return command;
    }
}
