package io.xpipe.app.storage;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppVersion;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.secret.EncryptionToken;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class DataStorageMigration {

    public static boolean isMigrated() {
        return !requiresMigration;
    }

    private static boolean requiresMigration = false;

    private static Optional<String> loadVaultVersion(Path dir) throws IOException {
        var file = dir.resolve("vaultversion");
        if (Files.exists(file)) {
            var s = Files.readString(file);
            return Optional.of(s);
        } else {
            return Optional.empty();
        }
    }

    public static void showLegacyVaultMigrationErrorIfNeeded() {
        if (!requiresMigration) {
            return;
        }

        DataStorageMigrationDialog.show();
    }

    public static void init() throws IOException {
        var dir = DataStorage.getStorageDirectory();
        var version = loadVaultVersion(dir);
        if (version.isPresent()) {
            var canonicalVersion = AppVersion.parse(version.get());
            if (canonicalVersion.isEmpty()) {
                requiresMigration = true;
            } else if (canonicalVersion.get().getMajor() < 23 || (canonicalVersion.get().getMajor() == 23 && canonicalVersion.get().getMinor() < 9)) {
                requiresMigration = true;
            }
        } else {
            requiresMigration = true;
        }

        if (isMigrated()) {
            EncryptionToken.setMigratedVaultToken(true);
        }
    }

    private static StandardStorage getStorage() {
        return (StandardStorage) DataStorage.get();
    }

    public static void migrate() throws IOException {
        var dir = DataStorage.getStorageDirectory();

        var file = dir.resolve("vaultkey");
        try {
            var vaultKey = DataStorageVaultKey.generate();
            DataStorageVaultKey.write(vaultKey, file);
            getStorage().vaultKey = vaultKey;
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(
                            "Unable to convert vault key file " + file, e)
                    .terminal(true)
                    .build()
                    .handle();
        }

        DataStorage.get().migrate();

        EncryptionToken.invalidateTokens();
        EncryptionToken.setMigratedVaultToken(true);

        DataStorageUserHandler.getInstance().migrate();

        getStorage().forceRewrite();
        getStorage().save(false);

        AppPrefs.get().save();

        var versionFile = dir.resolve("vaultversion");
        Files.writeString(versionFile, AppProperties.get().getCanonicalVersion().map(appVersion -> appVersion.toString()).orElse("23.9"));

        AppCache.update("vaultMigrated", true);

        DataStorage.get().pushManually();
    }
}
