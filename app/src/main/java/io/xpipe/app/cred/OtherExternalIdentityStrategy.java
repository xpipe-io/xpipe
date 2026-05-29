package io.xpipe.app.cred;

import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.KeyValue;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@JsonTypeName("otherExternal")
@Value
@Jacksonized
@Builder
public class OtherExternalIdentityStrategy implements SshIdentityStrategy {

    @Override
    public void prepareParent(ShellControl parent) throws Exception {}

    @Override
    public void checkComplete() {}

    @Override
    public void buildCommand(CommandBuilder builder) {}

    @Override
    public List<KeyValue> configOptions(ShellControl sc) throws Exception {
        return List.of();
    }

    public PublicKeyStrategy getPublicKeyStrategy() {
        return null;
    }
}
