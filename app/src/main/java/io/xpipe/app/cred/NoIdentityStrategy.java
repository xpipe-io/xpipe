package io.xpipe.app.cred;

import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.KeyValue;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

import java.util.List;

@JsonTypeName("none")
@Value
public class NoIdentityStrategy implements SshIdentityStrategy {

    @Override
    public void prepareParent(ShellControl parent) {}

    @Override
    public void buildCommand(CommandBuilder builder) {}

    @Override
    public List<KeyValue> configOptions(ShellControl sc) {
        // Don't use any agent keys to prevent too many authentication failures
        return List.of(
                KeyValue.raw("IdentitiesOnly", "yes"),
                KeyValue.raw("IdentityAgent", "none"),
                KeyValue.raw("IdentityFile", "none"),
                KeyValue.raw("PKCS11Provider", "none"));
    }

    @Override
    public PublicKeyStrategy getPublicKeyStrategy() {
        return null;
    }

    @Override
    public boolean providesKey() {
        return false;
    }
}
