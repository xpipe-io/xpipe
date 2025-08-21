package io.xpipe.ext.base.identity.ssh;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.KeyValue;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@JsonTypeName("otherExternal")
@Value
@Jacksonized
@Builder
public class OtherExternalStrategy implements SshIdentityStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<OtherExternalStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var forward = new SimpleBooleanProperty(p.getValue() != null && p.getValue().isForwardAgent());
        var publicKey = new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);
        return new OptionsBuilder().nameAndDescription("forwardAgent")
                .addToggle(forward)
                .nonNull()
                .hide(!config.isAllowAgentForward())
                .nameAndDescription("publicKey")
                .addComp(new TextFieldComp(publicKey).apply(
                        struc -> struc.get().setPromptText("ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIBmhLUTJiP...== Your Comment")), publicKey)
                .bind(() -> {
                    return new OtherExternalStrategy(forward.get(), publicKey.get());
                }, p);
    }

    boolean forwardAgent;
    String publicKey;

    @Override
    public void prepareParent(ShellControl parent) {}

    @Override
    public void buildCommand(CommandBuilder builder) {}

    @Override
    public List<KeyValue> configOptions(ShellControl parent) throws Exception {
        var file =  SshIdentityStrategy.getPublicKeyPath(publicKey);
        return List.of(new KeyValue("IdentitiesOnly", file.isPresent() ? "yes" : "no"), new KeyValue("ForwardAgent", forwardAgent ? "yes" : "no"),
                new KeyValue("IdentityFile", file.isPresent() ? file.get().toString() : "none"), new KeyValue("PKCS11Provider", "none"));
    }
}
