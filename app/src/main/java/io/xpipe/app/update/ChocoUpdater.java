package io.xpipe.app.update;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppRestart;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.util.XPipeInstallation;

import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChocoUpdater extends UpdateHandler {

    public ChocoUpdater() {
        super(true);
    }

    @Override
    public boolean supportsDirectInstallation() {
        return true;
    }

    @Override
    public List<ModalButton> createActions() {
        var l = new ArrayList<ModalButton>();
        l.add(new ModalButton("ignore", null, true, false));
        l.add(new ModalButton(
                "checkOutUpdate",
                () -> {
                    if (getPreparedUpdate().getValue() == null) {
                        return;
                    }

                    Hyperlinks.open(getPreparedUpdate().getValue().getReleaseUrl());
                },
                false,
                false));
        l.add(new ModalButton(
                "install",
                () -> {
                    executeUpdateAndClose();
                },
                true,
                true));
        return l;
    }

    private Optional<String> getOutdatedPackageUpdateVersion() throws Exception {
        if (AppProperties.get().isStaging()) {
            return Optional.empty();
        }

        var pkg = "xpipe";
        var out = LocalShell.getShell()
                .command(CommandBuilder.of().add("choco", "outdated"))
                .readStdoutIfPossible();
        if (out.isEmpty()) {
            return Optional.empty();
        }

        var line = out.get()
                .lines()
                .filter(s -> {
                    var split = s.split("\\|");
                    if (split.length != 4) {
                        return false;
                    } else {
                        return split[0].equals(pkg);
                    }
                })
                .findFirst();
        if (line.isEmpty()) {
            return Optional.empty();
        }

        var v = line.get().split("\\|")[2];
        return Optional.of(v);
    }

    public synchronized AvailableRelease refreshUpdateCheckImpl(boolean first, boolean securityOnly) throws Exception {
        var rel = AppDownloads.queryLatestRelease(first, securityOnly);
        event("Determined latest suitable release " + rel.getTag());

        var chocoRelease = getOutdatedPackageUpdateVersion();
        // Use current release if the update is not available for choco yet
        if (chocoRelease.isEmpty() || !chocoRelease.get().equals(rel.getTag())) {
            rel = AppRelease.of(AppProperties.get().getVersion());
        }

        var isUpdate = isUpdate(rel.getTag());
        lastUpdateCheckResult.setValue(new AvailableRelease(
                AppProperties.get().getVersion(),
                AppDistributionType.get().getId(),
                rel.getTag(),
                rel.getBrowserUrl(),
                null,
                null,
                Instant.now(),
                isUpdate,
                securityOnly));
        return lastUpdateCheckResult.getValue();
    }

    @Override
    public void executeUpdate() {
        try {
            var p = preparedUpdate.getValue();
            var performedUpdate = new PerformedUpdate(p.getVersion(), p.getBody(), p.getVersion());
            AppCache.update("performedUpdate", performedUpdate);
            OperationMode.executeAfterShutdown(() -> {
                var systemWide = Files.exists(
                        XPipeInstallation.getCurrentInstallationBasePath().resolve("system"));
                var propertiesArguments = systemWide ? ", --install-arguments=\"'ALLUSERS=1'\"" : "";
                TerminalLauncher.openDirectFallback("XPipe Updater", sc -> {
                    var pkg = "xpipe";
                    var commandToRun = "Start-Process -Wait -Verb runAs -FilePath choco -ArgumentList upgrade, " + pkg
                            + ", -y" + propertiesArguments;
                    var powershell = ShellDialects.isPowershell(sc);
                    var powershellCommand = powershell
                            ? "powershell -Command " + sc.getShellDialect().quoteArgument(commandToRun)
                            : "powershell -Command " + commandToRun;
                    return ShellScript.lines(powershellCommand, AppRestart.getTerminalRestartCommand());
                });
            });
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).handle();
            preparedUpdate.setValue(null);
        }
    }
}
