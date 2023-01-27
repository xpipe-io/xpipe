package io.xpipe.ext.proc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.MachineStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.core.util.SecretValue;
import io.xpipe.extension.util.Validators;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;

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
        return new SshProcessControlImpl(this);
    }

    @SuperBuilder
    @Jacksonized
    @Getter
    public static class SshKey {
        @NonNull
        Path file;

        SecretValue password;
    }
}
