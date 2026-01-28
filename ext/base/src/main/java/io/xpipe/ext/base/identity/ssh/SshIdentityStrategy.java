package io.xpipe.ext.base.identity.ssh;

import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
        l.add(InPlaceKeyStrategy.class);
        l.add(KeyFileStrategy.class);
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

    static Optional<FilePath> getPublicKeyPath(ShellControl sc, String publicKey) throws Exception {
        if (publicKey == null || publicKey.isBlank()) {
            return Optional.empty();
        }

        var isFile = OsFileSystem.of(sc.getOsType()).isProbableFilePath(publicKey);
        if (isFile && sc.view().fileExists(FilePath.of(publicKey))) {
            return Optional.of(FilePath.of(publicKey));
        }

        try {
            var base = sc.getSystemTemporaryDirectory().join("key.pub");
            var file = sc.view().writeTextFileDeterministic(base, publicKey.strip() + "\n");

            if (sc.getOsType() != OsType.WINDOWS) {
                sc.command(CommandBuilder.of().add("chmod", "400").addFile(file))
                        .executeAndCheck();
            }

            return Optional.of(file);
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return Optional.empty();
        }
    }

    default boolean providesKey() {
        return true;
    }

    default void checkComplete() throws ValidationException {}

    void prepareParent(ShellControl parent) throws Exception;

    void buildCommand(CommandBuilder builder);

    List<KeyValue> configOptions(ShellControl sc) throws Exception;

    default SecretRetrievalStrategy getAskpassStrategy() {
        return new SecretNoneStrategy();
    }

    String getPublicKey();
}
