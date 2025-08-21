package io.xpipe.ext.base.identity.ssh;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.KeyValue;
import lombok.Value;

import java.util.List;

@JsonTypeName("none")
@Value
public class NoneStrategy implements SshIdentityStrategy {

    @Override
    public void prepareParent(ShellControl parent) {}

    @Override
    public void buildCommand(CommandBuilder builder) {}

    @Override
    public List<KeyValue> configOptions(ShellControl parent) {
        // Don't use any agent keys to prevent too many authentication failures
        return List.of(new KeyValue("IdentitiesOnly", "yes"), new KeyValue("IdentityAgent", "none"), new KeyValue("IdentityFile", "none"),
                new KeyValue("PKCS11Provider", "none"));
    }
}
