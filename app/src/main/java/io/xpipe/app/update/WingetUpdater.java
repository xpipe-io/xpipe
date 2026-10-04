package io.xpipe.app.update;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppRestart;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.Hyperlinks;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WingetUpdater extends UpdateHandler {

    public WingetUpdater() {
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

    @Override
    public void executeUpdate() {
        try {
            var p = preparedUpdate.getValue();
            var performedUpdate = new PerformedUpdate(p.getVersion(), p.getBody(), p.getVersion());
            AppCache.update("performedUpdate", performedUpdate);
            AppOperationMode.executeAfterShutdown(() -> {
                TerminalLaunch.builder().title("XPipe Updater").localScript(sc -> {
                    var systemWide = AppInstallation.ofWindows().isSystemWide();
                    var pkgId = "xpipe-io.xpipe";
                    if (systemWide) {
                        return ShellScript.lines(
                                "powershell -Command \"Start-Process -Wait -Verb runAs -FilePath winget -ArgumentList upgrade, --id, "
                                        + pkgId + "\"",
                                AppRestart.getTerminalRestartCommand());
                    } else {
                        return ShellScript.lines(
                                "winget upgrade --id " + pkgId, AppRestart.getTerminalRestartCommand());
                    }
                }).launch();
            });
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).handle();
            preparedUpdate.setValue(null);
        }
    }

    public synchronized AvailableRelease refreshUpdateCheckImpl(boolean first, boolean securityOnly) throws Exception {
        var rel = AppDownloads.queryLatestVersion(first, securityOnly);
        event("Determined latest suitable release " + rel.getTag());

        if (!AppProperties.get().isStaging()) {
            var wingetRelease = getOutdatedPackageUpdateVersion();
            // Use current release if the update is not available for winget yet
            if (wingetRelease.isEmpty() || !wingetRelease.get().equals(rel.getTag())) {
                rel = AppRelease.ofInstaller(AppProperties.get().getVersion());
            }
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

    private Optional<String> getOutdatedPackageUpdateVersion() throws Exception {
        var pkgId = "xpipe-io.xpipe";
        var out = LocalShell.getShell()
                .command(CommandBuilder.of()
                        .add("winget", "list", "--upgrade-available", "--source=winget", "--id", pkgId))
                .readStdoutIfPossible();
        if (out.isEmpty()) {
            return Optional.empty();
        }

        // Columns are Name, Id, Version, Available. The name can contain spaces
        for (var line : out.get().lines().toList()) {
            var split = List.of(line.strip().split("\\s+"));
            var idIndex = split.indexOf(pkgId);
            if (idIndex != -1 && idIndex + 2 < split.size()) {
                return Optional.of(split.get(idIndex + 2));
            }
        }
        return Optional.empty();
    }
}
