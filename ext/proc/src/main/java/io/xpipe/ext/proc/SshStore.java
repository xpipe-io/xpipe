package io.xpipe.ext.proc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.MachineStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.core.util.SecretValue;
import io.xpipe.ext.proc.augment.SshCommandAugmentation;
import io.xpipe.ext.proc.util.SshToolHelper;
import io.xpipe.extension.util.Validators;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@Jacksonized
@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@JsonTypeName("ssh")
public class SshStore extends JacksonizedValue implements MachineStore {

    ShellStore proxy;
    String host;
    Integer port;
    String user;
    SecretValue password;
    SshKey key;
    public SshStore(ShellStore proxy, String host, Integer port, String user, SecretValue password, SshKey key) {
        this.proxy = proxy;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.key = key;
    }

    @Override
    public void checkComplete() throws Exception {
        Validators.nonNull(proxy, "Proxy");
        Validators.nonNull(host, "Host");
        Validators.nonNull(port, "Port");
        Validators.nonNull(user, "User");

        proxy.checkComplete();
    }

    @Override
    public ShellProcessControl create() {
        if (ShellStore.isLocal(proxy)) {
            return new SshProcessControlImpl(this);
        }

        return proxy.create()
                .subShell(
                        shellProcessControl -> {
                            var command = SshToolHelper.toCommand(this, shellProcessControl);
                            var augmentedCommand = new SshCommandAugmentation().prepareNonTerminalCommand(shellProcessControl, command);
                            var passwordCommand = SshToolHelper.passPassword(
                                    augmentedCommand,
                                    getPassword(),
                                    getKey() != null ? getKey().getPassword() : null,
                                    shellProcessControl);
                            return passwordCommand;
                        },
                        (shellProcessControl, s) -> {
                            var command = SshToolHelper.toCommand(this, shellProcessControl);
                            var augmentedCommand = new SshCommandAugmentation().prepareTerminalCommand(shellProcessControl, command, s);
                            var passwordCommand = SshToolHelper.passPassword(
                                    augmentedCommand,
                                    getPassword(),
                                    getKey() != null ? getKey().getPassword() : null,
                                    shellProcessControl);

                            if (true) return passwordCommand;
                            var operator = shellProcessControl.getShellType().getOrConcatenationOperator();
                            var consoleCommand = passwordCommand + operator + shellProcessControl.getShellType().getPauseCommand();
                            try {
                                return shellProcessControl.prepareIntermediateTerminalOpen(consoleCommand);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })
                .elevated(shellProcessControl -> true);
    }

    @SuperBuilder
    @Jacksonized
    @Getter
    public static class SshKey {
        @NonNull
        String file;

        SecretValue password;
    }
}
