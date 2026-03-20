package io.xpipe.app.cred;

import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.LocalExec;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.sun.jna.Memory;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("pageant")
@Value
@Jacksonized
@Builder
public class PageantStrategy implements SshIdentityAgentStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<PageantStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var publicKey =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);
        return new OptionsBuilder()
                .nameAndDescription("publicKey")
                .addComp(new SshAgentKeyListComp(config.getFileSystem(), p, publicKey, false), publicKey)
                .bind(
                        () -> {
                            return new PageantStrategy(publicKey.get());
                        },
                        p);
    }

    private static Boolean supported;

    public static boolean isSupported() {
        if (supported != null) {
            return supported;
        }

        if (OsType.ofLocal() == OsType.WINDOWS) {
            return true;
        } else {
            try {
                var found = LocalExec.readStdoutIfPossible("which", "pageant").isPresent();
                return (supported = found);
            } catch (Exception ex) {
                return (supported = false);
            }
        }
    }

    String publicKey;

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        if (parent.getOsType() != OsType.WINDOWS) {
            var out = parent.executeSimpleStringCommand("pageant -l");
            if (out.isBlank()) {
                throw ErrorEventFactory.expected(
                        new IllegalStateException("Pageant is not running or has no identities"));
            }

            var socket = AppPrefs.get().defaultSshAgentSocket().getValue();
            if (socket == null || !socket.toString().contains("pageant")) {
                throw ErrorEventFactory.expected(new IllegalStateException(
                        "Pageant is not running as the primary agent via the $SSH_AUTH_SOCK variable."));
            }
        } else if (parent.isLocal()) {
            // Check if it exists
            getPageantWindowsPipe();
        }
    }

    @Override
    public FilePath determinetAgentSocketLocation(ShellControl sc) {
        if (sc.isLocal() && sc.getOsType() == OsType.WINDOWS) {
            return FilePath.of(getPageantWindowsPipe());
        }

        return null;
    }

    @Override
    public void buildCommand(CommandBuilder builder) {}

    @Override
    public List<KeyValue> configOptions(ShellControl sc) throws Exception {
        var file = SshIdentityStrategy.getPublicKeyPath(sc, publicKey);
        var l = new ArrayList<>(List.of(
                new KeyValue("IdentitiesOnly", file.isPresent() ? "yes" : "no"),
                new KeyValue("IdentityFile", file.isPresent() ? file.get().toString() : "none"),
                new KeyValue("PKCS11Provider", "none")));

        var agent = determinetAgentSocketLocation(sc);
        if (agent != null) {
            l.add(new KeyValue("IdentityAgent", "\"" + agent + "\""));
        }

        return l;
    }

    private String getPageantWindowsPipe() {
        Memory p = new Memory(WinBase.WIN32_FIND_DATA.sizeOf());
        var r = Kernel32.INSTANCE.FindFirstFile(
                "\\\\.\\pipe\\*pageant." + AppSystemInfo.ofCurrent().getUser() + "*", p);
        if (r == WinBase.INVALID_HANDLE_VALUE) {
            throw ErrorEventFactory.expected(new IllegalStateException("Pageant is not running"));
        }

        WinBase.WIN32_FIND_DATA fd = new WinBase.WIN32_FIND_DATA(p);
        Kernel32.INSTANCE.FindClose(r);

        var file = "\\\\.\\pipe\\" + fd.getFileName();
        return file;
    }

    public PublicKeyStrategy getPublicKeyStrategy() {
        return PublicKeyStrategy.Fixed.of(publicKey);
    }
}
