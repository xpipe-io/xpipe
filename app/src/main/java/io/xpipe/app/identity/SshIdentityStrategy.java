package io.xpipe.app.identity;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.util.FilePath;
import io.xpipe.app.util.KeyValue;
import io.xpipe.app.util.OsType;
import io.xpipe.app.util.ValidationException;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface SshIdentityStrategy {

    static FilePath getPublicKeyPath(FilePath file) {
        if (file.getExtension().isEmpty() || file.isDotFile()) {
            return FilePath.of(file + ".pub");
        } else {
            return FilePath.of(file.getBaseName() + ".pub");
        }
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

    default boolean supportsIdentityApply() {
        return true;
    }

    PublicKeyStrategy getPublicKeyStrategy();
}
