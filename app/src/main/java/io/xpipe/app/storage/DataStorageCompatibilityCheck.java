package io.xpipe.app.storage;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.core.AppVersion;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.issue.ErrorAction;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.update.AppDownloads;
import io.xpipe.app.update.AppRelease;
import io.xpipe.app.util.OsType;

import org.apache.commons.io.FileUtils;

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
                            && canonicalVersion.get().getMinor() >= 99)) {
                return;
            }
        }

        ErrorEventFactory.fromMessage(
                        "The vault" + (version.isPresent() ? " from v" + version.get() + " " : " ")
                                + "comes from an XPipe version prior to v24."
                                + " This legacy format is unsupported in newer versions. To migrate your data, you need to first install and launch XPipe v23.99.x."
                                + " This will start a migration for the vault data. Afterwards, you can upgrade to XPipe v24+ and launch it as normal.")
                .customAction(new ErrorAction() {
                    @Override
                    public String getName() {
                        return "Launch automatically";
                    }

                    @Override
                    public String getDescription() {
                        return "Download and launch XPipe v23.99.x to perform the migration";
                    }

                    @Override
                    public boolean handle(ErrorEvent event) throws Exception {
                        runTransitoryBuild();
                        return true;
                    }
                })
                .expected()
                .handle();
        AppOperationMode.halt(0);
    }

    private static void runTransitoryBuild() throws Exception {
        var version = AppProperties.get().isStaging() ? "23.99-5" : "23.99.2";
        var downloaded = AppDownloads.downloadArtifact(AppRelease.ofPortable(version));
        var tempTarget = AppSystemInfo.ofCurrent().getTemp().resolve("xpipe-v23.99");
        var shell = LocalShell.getInstance(DataStorageCompatibilityCheck.class);
        switch (OsType.ofLocal()) {
            case OsType.Linux ignored -> {
                Files.createDirectories(tempTarget);
                FileUtils.cleanDirectory(tempTarget.toFile());
                shell.command(CommandBuilder.of()
                                .add("tar", "-f")
                                .addFile(downloaded)
                                .add("--strip-components", "1")
                                .add("-C")
                                .addFile(tempTarget)
                                .add("-xz"))
                        .execute();
                var executable = tempTarget.resolve("bin", "xpiped");
                AppOperationMode.executeAfterShutdown(() -> {
                    ExternalApplicationHelper.startAsync(CommandBuilder.of()
                            .addFile(executable)
                            .addQuoted("-Dio.xpipe.app.portableMigration=true")
                            .addQuoted("-Dio.xpipe.app.dataDir="
                                    + AppProperties.get().getDataDir()));
                });
            }
            case OsType.MacOs ignored -> {
                AppOperationMode.executeAfterShutdown(() -> {
                    ExternalApplicationHelper.startAsync(
                            CommandBuilder.of().add("open").addFile(downloaded));
                });
            }
            case OsType.Windows ignored -> {
                Files.createDirectories(tempTarget);
                FileUtils.cleanDirectory(tempTarget.toFile());
                shell.enforceDialect(ShellDialects.POWERSHELL, pw -> {
                    var command = CommandBuilder.of().add("Expand-Archive", "-Force");
                    command.add("-DestinationPath").addFile(tempTarget);
                    command.add("-Path").addFile(downloaded);
                    pw.command(command).execute();
                    return null;
                });
                var executable = tempTarget.resolve("xpipe-" + version).resolve("xpiped.exe");
                AppOperationMode.executeAfterShutdown(() -> {
                    ExternalApplicationHelper.startAsync(CommandBuilder.of()
                            .addFile(executable)
                            .addQuoted("-Dio.xpipe.app.portableMigration=true")
                            .addQuoted("-Dio.xpipe.app.dataDir="
                                    + AppProperties.get().getDataDir()));
                });
            }
        }
    }
}
