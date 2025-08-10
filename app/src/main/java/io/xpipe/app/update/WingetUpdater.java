package io.xpipe.app.update;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppRestart;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.LocalShell;

import java.nio.file.Files;
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
            OperationMode.executeAfterShutdown(() -> {
                TerminalLaunch.builder().title("XPipe Updater").localScript(sc -> {
                    var systemWide = Files.exists(AppInstallation.ofCurrent()
                            .getBaseInstallationPath()
                            .resolve("system"));
                    var pkgId = "xpipe-io.xpipe";
                    if (systemWide) {
                        return ShellScript.lines(
                                "powershell -Command \"Start-Process -Verb runAs -FilePath winget -ArgumentList upgrade, --id, "
                                        + pkgId + "\"",
                                AppRestart.getTerminalRestartCommand());
                    } else {
                        return ShellScript.lines(
                                "winget upgrade --id " + pkgId, AppRestart.getTerminalRestartCommand());
                    }
                });
            });
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).handle();
            preparedUpdate.setValue(null);
        }
    }

    public synchronized AvailableRelease refreshUpdateCheckImpl(boolean first, boolean securityOnly) throws Exception {
        var rel = AppDownloads.queryLatestRelease(first, securityOnly);
        event("Determined latest suitable release " + rel.getTag());

        var wingetRelease = getOutdatedPackageUpdateVersion();
        // Use current release if the update is not available for winget yet
        if (wingetRelease.isPresent() && !wingetRelease.get().equals(rel.getTag())) {
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

    private Optional<String> getOutdatedPackageUpdateVersion() throws Exception {
        if (AppProperties.get().isStaging()) {
            return Optional.empty();
        }

        var pkgId = "xpipe-io.xpipe";
        var out = LocalShell.getShell()
                .command(CommandBuilder.of()
                        .add("winget", "list", "--upgrade-available", "--source=winget", "--id", pkgId))
                .readStdoutIfPossible();
        if (out.isEmpty()) {
            return Optional.empty();
        }

        var line = out.get()
                .lines()
                .filter(s -> {
                    var split = s.split("\\s+");
                    if (split.length != 4) {
                        return false;
                    } else {
                        return split[1].equals(pkgId);
                    }
                })
                .findFirst();
        if (line.isEmpty()) {
            return Optional.empty();
        }

        var v = line.get().split("\\s+")[3];
        return Optional.of(v);
    }
}
