package io.xpipe.app.cred;

import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FilePath;

public interface SshIdentityAgentStrategy extends SshIdentityKeyListStrategy {

    @Override
    default CommandBuilder createListCommand() {
        return CommandBuilder.of().add("ssh-add", "-L")
                .environment("SSH_AUTH_SOCK", sc -> {
                    var socket = determineAgentSocketLocation(sc);
                    return socket != null ? socket.toString() : null;
                });
    }

    FilePath determineAgentSocketLocation(ShellControl parent) throws Exception;
}
