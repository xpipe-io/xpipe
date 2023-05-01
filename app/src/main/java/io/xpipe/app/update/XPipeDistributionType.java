package io.xpipe.app.update;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.XPipeSession;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.XPipeInstallation;
import lombok.Getter;

import java.util.Arrays;
import java.util.function.Supplier;

public enum XPipeDistributionType {
    DEVELOPMENT("development", () -> new GitHubUpdater(false)),
    PORTABLE("portable", () -> new PortableUpdater()),
    NATIVE_INSTALLATION("install", () -> new GitHubUpdater(true)),
    HOMEBREW("homebrew", () -> new HomebrewUpdater()),
    CHOCO("choco", () -> new ChocoUpdater());

    private static XPipeDistributionType type;

    XPipeDistributionType(String id, Supplier<UpdateHandler> updateHandlerSupplier) {
        this.id = id;
        this.updateHandlerSupplier = updateHandlerSupplier;
    }

    public static XPipeDistributionType get() {
        if (type != null) {
            return type;
        }

        if (!ModuleHelper.isImage()) {
            return (type = DEVELOPMENT);
        }

        if (!XPipeSession.get().isNewBuildSession()) {
            var cached = AppCache.get("dist", String.class, () -> null);
            var cachedType = Arrays.stream(values())
                    .filter(xPipeDistributionType ->
                            xPipeDistributionType.getId().equals(cached))
                    .findAny()
                    .orElse(null);
            if (cachedType != null) {
                return (type = cachedType);
            }
        }

        type = determine();
        AppCache.update("dist", type.getId());
        return type;
    }

    public static XPipeDistributionType determine() {
        if (!XPipeInstallation.isInstallationDistribution()) {
            return PORTABLE;
        }

        try (var sc = LocalStore.getShell()) {
            try (var chocoOut = sc.command("choco search --local-only -r xpipe").start()) {
                var out = chocoOut.readStdoutDiscardErr();
                if (chocoOut.getExitCode() == 0) {
                    var split = out.split("\\|");
                    if (split.length == 2) {
                        var version = split[1];
                        if (AppProperties.get().getVersion().equals(version)) {
                            return CHOCO;
                        }
                    }
                }
            }

            if (OsType.getLocal().equals(OsType.MACOS)) {
                try (var brewOut = sc.command("brew info xpipe").start()) {
                    var out = brewOut.readStdoutDiscardErr();
                    if (brewOut.getExitCode() == 0) {
                        var split = out.split("\\|");
                        if (split.length == 2) {
                            var version = split[1];
                            if (AppProperties.get().getVersion().equals(version)) {
                                return HOMEBREW;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }

        return XPipeDistributionType.NATIVE_INSTALLATION;
    }

    @Getter
    private final String id;
    private UpdateHandler updateHandler;
    private final Supplier<UpdateHandler> updateHandlerSupplier;

    public UpdateHandler getUpdateHandler() {
        if (updateHandler == null) {
            updateHandler = updateHandlerSupplier.get();
        }
        return updateHandler;
    }
}
