package io.xpipe.ext.base.identity.ssh;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.Validator;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

@JsonTypeName("customAgent")
@Value
@Jacksonized
@Builder
public class CustomAgentStrategy implements SshIdentityStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<CustomAgentStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var forward = new SimpleBooleanProperty(p.getValue() != null && p.getValue().isForwardAgent());
        var publicKey = new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);

        var socket = AppPrefs.get().sshAgentSocket();
        var socketBinding = BindingsHelper.map(socket, s -> {
            return s != null ? s.toString() : AppI18n.get("agentSocketNotConfigured");
        });
        var socketProp = new SimpleStringProperty();
        socketProp.bind(socketBinding);
        var socketDisplay = new HorizontalComp(List.of(
                new TextFieldComp(socketProp)
                        .apply(struc -> struc.get().setEditable(false)).hgrow(),
                new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
                    AppPrefs.get().selectCategory("ssh");
                })
                        .padding(new Insets(7))
                        .grow(false, true)))
                .spacing(9);

        return new OptionsBuilder()
                .nameAndDescription("agentSocket")
                .addComp(socketDisplay)
                .check(val -> Validator.create(val, AppI18n.observable("agentSocketNotConfigured"), AppPrefs.get().sshAgentSocket(), i -> {
                    return i != null;
                }))
                .nameAndDescription("forwardAgent")
                .addToggle(forward)
                .nonNull()
                .hide(!config.isAllowAgentForward())
                .nameAndDescription("publicKey")
                .addComp(new TextFieldComp(publicKey).apply(
                        struc -> struc.get().setPromptText("ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIBmhLUTJiP...== Your Comment")), publicKey)
                .bind(() -> {
                    return new CustomAgentStrategy(forward.get(), publicKey.get());
                }, p);
    }


    boolean forwardAgent;
    String publicKey;

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        if (parent.isLocal()) {
            SshIdentityStateManager.prepareLocalCustomAgent(parent, AppPrefs.get().sshAgentSocket().getValue());
        }
    }

    @Override
    public void buildCommand(CommandBuilder builder) {
        builder.environment("SSH_AUTH_SOCK", sc -> {
            if (sc.getOsType() == OsType.WINDOWS) {
                return null;
            }

            if (AppPrefs.get() != null) {
                var socket = AppPrefs.get().sshAgentSocket().getValue();
                if (socket != null) {
                    return socket.resolveTildeHome(sc.view().userHome()).toString();
                }
            }

            return null;
        });
    }

    @Override
    public List<KeyValue> configOptions() {
        var file =  SshIdentityStrategy.getPublicKeyPath(publicKey);
        return List.of(new KeyValue("IdentitiesOnly", file.isPresent() ? "yes" : "no"), new KeyValue("ForwardAgent", forwardAgent ? "yes" : "no"),
                new KeyValue("IdentityFile", file.isPresent() ? file.get().toString() : "none"), new KeyValue("PKCS11Provider", "none"));
    }
}
