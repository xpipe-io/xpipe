package io.xpipe.ext.base.identity.ssh;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@JsonTypeName("pageant")
@Value
@Jacksonized
@Builder
public class PageantStrategy implements SshIdentityStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<PageantStrategy> p, SshIdentityStrategyChoiceConfig config) {
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
                    return new PageantStrategy(forward.get(), publicKey.get());
                }, p);
    }

    private static Boolean supported;

    public static boolean isSupported() {
        if (supported != null) {
            return supported;
        }

        if (OsType.getLocal() == OsType.WINDOWS) {
            return true;
        } else {
            try {
                var found = LocalShell.getShell().view().findProgram("pageant").isPresent();
                return (supported = found);
            } catch (Exception ex) {
                return (supported = false);
            }
        }
    }

    boolean forwardAgent;
    String publicKey;

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        if (parent.getOsType() != OsType.WINDOWS) {
            var out = parent.executeSimpleStringCommand("pageant -l");
            if (out.isBlank()) {
                throw ErrorEventFactory.expected(new IllegalStateException("Pageant is not running or has no identities"));
            }

            var socket = AppPrefs.get().defaultSshAgentSocket().getValue();
            if (socket == null || !socket.toString().contains("pageant")) {
                throw ErrorEventFactory.expected(
                        new IllegalStateException("Pageant is not running as the primary agent via the $SSH_AUTH_SOCK variable."));
            }
        }
    }

    @Override
    public void buildCommand(CommandBuilder builder) {
        builder.environment("SSH_AUTH_SOCK", parent -> {
            if (parent.getOsType() == OsType.WINDOWS) {
                return getPageantWindowsPipe(parent);
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

    private String getPageantWindowsPipe(ShellControl parent) throws Exception {
        var name = parent.enforceDialect(ShellDialects.POWERSHELL, powershell -> {
            var pipe = powershell.executeSimpleStringCommand(
                    "Get-ChildItem \"\\\\.\\pipe\\\" -recurse | Where-Object {$_.Name -match \"pageant\"} | foreach {echo $_.Name}");
            var lines = pipe.lines().toList();
            if (lines.isEmpty()) {
                throw ErrorEventFactory.expected(new IllegalStateException("Pageant is not running"));
            }

            if (lines.size() > 1) {
                var uname = powershell.getShellDialect().printUsernameCommand(powershell).readStdoutOrThrow();
                return lines.stream().filter(s -> s.contains(uname)).findFirst().orElse(lines.getFirst());
            }

            return lines.getFirst();
        });

        var file = "\\\\.\\pipe\\" + name;
        return file;
    }
}
