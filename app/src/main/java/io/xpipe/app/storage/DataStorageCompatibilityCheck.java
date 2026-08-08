package io.xpipe.app.storage;

import io.xpipe.app.core.AppVersion;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.DocumentationLink;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class DataStorageCompatibilityCheck {

    private static Optional<String> loadVaultVersion(Path dir) throws IOException {
        var file = dir.resolve("vaultversion");
        if (Files.exists(file)) {
            var s = Files.readString(file);
            return Optional.of(s);
        } else {
            return Optional.empty();
        }
    }

    public static void showLegacyVaultMigrationErrorIfNeeded() throws IOException {
        var dir = DataStorage.getStorageDirectory();
        var version = loadVaultVersion(dir);
        if (version.isPresent()) {
            var canonicalVersion = AppVersion.parse(version.get());
            if (canonicalVersion.isEmpty()) {
                return;
            }

            if (canonicalVersion.get().getMajor() >= 24
                    || (canonicalVersion.get().getMajor() == 23
                            && canonicalVersion.get().getMinor() >= 10)) {
                return;
            }
        }

        ErrorEventFactory.fromMessage("The vault" + (version.isPresent() ? " from v" + version.get() + " " : " ")
                        + "comes from an XPipe version prior to v24."
                        + " This legacy format is unsupported in newer versions. To migrate your data, first install and launch XPipe v23.10."
                        + " This will start a migration for the vault data. Afterwards, you can upgrade to XPipe v24+ and launch it as normal."
                        + " XPipe will now exit.")
                .documentationLink(DocumentationLink.MIGRATION)
                .expected()
                .term()
                .handle();
        AppOperationMode.shutdown(false);
    }
}
