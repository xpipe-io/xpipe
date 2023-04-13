package io.xpipe.app.update;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.XPipeSession;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.XPipeInstallation;
import lombok.Getter;

import java.util.Arrays;
import java.util.function.Supplier;

public enum XPipeDistributionType {
    DEVELOPMENT("development", () -> new GitHubUpdater(false)),
    PORTABLE("portable", () -> new PortableUpdater()),
    INSTALLATION("install", () -> new GitHubUpdater(true)),
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
            return (type = PORTABLE);
        }

        try (var sc = LocalStore.getShell()) {
            try (var chocoOut = sc.command("choco search --local-only -r xpipe").start()) {
                var out = chocoOut.readStdoutDiscardErr();
                if (chocoOut.getExitCode() == 0) {
                    var split = out.split("\\|");
                    if (split.length == 2) {
                        return CHOCO;
                    }
                }
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }

        return XPipeDistributionType.INSTALLATION;
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
