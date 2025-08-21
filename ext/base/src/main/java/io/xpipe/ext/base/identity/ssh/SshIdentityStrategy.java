package io.xpipe.ext.base.identity.ssh;

import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.*;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = NoneStrategy.class),
    @JsonSubTypes.Type(value = KeyFileStrategy.class),
    @JsonSubTypes.Type(value = OpenSshAgentStrategy.class),
    @JsonSubTypes.Type(value = PasswordManagerAgentStrategy.class),
    @JsonSubTypes.Type(value = PageantStrategy.class),
    @JsonSubTypes.Type(value = GpgAgentStrategy.class),
    @JsonSubTypes.Type(value = YubikeyPivStrategy.class),
    @JsonSubTypes.Type(value = CustomPkcs11LibraryStrategy.class),
    @JsonSubTypes.Type(value = OtherExternalStrategy.class)
})
public interface SshIdentityStrategy {

    static List<Class<?>> getSubclasses() {
        var l = new ArrayList<Class<?>>();
        l.add(NoneStrategy.class);
        l.add(KeyFileStrategy.class);
        l.add(OpenSshAgentStrategy.class);
        if (AppPrefs.get().passwordManager().getValue() != null) {
            l.add(PasswordManagerAgentStrategy.class);
        }
        if (GpgAgentStrategy.isSupported()) {
            l.add(GpgAgentStrategy.class);
        }
        if (PageantStrategy.isSupported()) {
            l.add(PageantStrategy.class);
        }
        l.add(YubikeyPivStrategy.class);
        l.add(CustomPkcs11LibraryStrategy.class);
        l.add(OtherExternalStrategy.class);

        return l;
    }

    static Optional<FilePath> getPublicKeyPath(String publicKey) throws Exception {
        if (publicKey == null || publicKey.isBlank()) {
            return Optional.empty();
        }

        var isFile = OsFileSystem.ofLocal().isProbableFilePath(publicKey);
        if (isFile && Files.exists(Paths.get(publicKey))) {
            return Optional.ofNullable(FilePath.parse(publicKey));
        }

        var base = LocalShell.getShell().getSystemTemporaryDirectory().join("key.pub");
        var file = LocalShell.getShell().view().writeTextFileDeterministic(base, publicKey.strip() + "\n");

        if (OsType.getLocal() != OsType.WINDOWS) {
            LocalShell.getShell()
                    .command(CommandBuilder.of().add("chmod", "400").addFile(file))
                    .executeAndCheck();
        }

        return Optional.of(file);
    }

    default void checkComplete() throws ValidationException {}

    void prepareParent(ShellControl parent) throws Exception;

    void buildCommand(CommandBuilder builder);

    List<KeyValue> configOptions(ShellControl parent) throws Exception;

    default SecretRetrievalStrategy getAskpassStrategy() {
        return new SecretRetrievalStrategy.None();
    }

}
