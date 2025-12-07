package io.xpipe.ext.base.identity.ssh;

import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.app.secret.SecretRetrievalStrategy;
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
    @JsonSubTypes.Type(value = NoIdentityStrategy.class),
    @JsonSubTypes.Type(value = KeyFileStrategy.class),
    @JsonSubTypes.Type(value = InPlaceKeyStrategy.class),
    @JsonSubTypes.Type(value = OpenSshAgentStrategy.class),
    @JsonSubTypes.Type(value = PageantStrategy.class),
    @JsonSubTypes.Type(value = CustomAgentStrategy.class),
    @JsonSubTypes.Type(value = GpgAgentStrategy.class),
    @JsonSubTypes.Type(value = YubikeyPivStrategy.class),
    @JsonSubTypes.Type(value = CustomPkcs11LibraryStrategy.class),
    @JsonSubTypes.Type(value = OtherExternalAgentStrategy.class)
})
public interface SshIdentityStrategy {

    static List<Class<?>> getSubclasses() {
        var l = new ArrayList<Class<?>>();
        l.add(NoIdentityStrategy.class);
        l.add(KeyFileStrategy.class);
        l.add(InPlaceKeyStrategy.class);
        l.add(OpenSshAgentStrategy.class);
        if (OsType.ofLocal() != OsType.WINDOWS) {
            l.add(CustomAgentStrategy.class);
        }
        if (GpgAgentStrategy.isSupported()) {
            l.add(GpgAgentStrategy.class);
        }
        if (PageantStrategy.isSupported()) {
            l.add(PageantStrategy.class);
        }
        l.add(YubikeyPivStrategy.class);
        l.add(CustomPkcs11LibraryStrategy.class);
        l.add(OtherExternalAgentStrategy.class);

        return l;
    }

    static Optional<FilePath> getPublicKeyPath(String publicKey) {
        if (publicKey == null || publicKey.isBlank()) {
            return Optional.empty();
        }

        var isFile = OsFileSystem.ofLocal().isProbableFilePath(publicKey);
        if (isFile && Files.exists(Paths.get(publicKey))) {
            return Optional.ofNullable(FilePath.parse(publicKey));
        }

        try {
            var base = LocalShell.getShell().getSystemTemporaryDirectory().join("key.pub");
            var file = LocalShell.getShell().view().writeTextFileDeterministic(base, publicKey.strip() + "\n");

            if (OsType.ofLocal() != OsType.WINDOWS) {
                LocalShell.getShell()
                        .command(CommandBuilder.of().add("chmod", "400").addFile(file))
                        .executeAndCheck();
            }

            return Optional.of(file);
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return Optional.empty();
        }
    }

    default void checkComplete() throws ValidationException {}

    void prepareParent(ShellControl parent) throws Exception;

    void buildCommand(CommandBuilder builder);

    List<KeyValue> configOptions();

    default SecretRetrievalStrategy getAskpassStrategy() {
        return new SecretNoneStrategy();
    }

    String getPublicKey() throws Exception;
}
