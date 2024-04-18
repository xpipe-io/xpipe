package io.xpipe.app.update;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.XPipeSession;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.XPipeInstallation;

import lombok.Getter;

import java.util.Arrays;
import java.util.function.Supplier;

public enum XPipeDistributionType {
    UNKNOWN("unknown", false, () -> new GitHubUpdater(false)),
    DEVELOPMENT("development", true, () -> new GitHubUpdater(false)),
    PORTABLE("portable", false, () -> new PortableUpdater()),
    NATIVE_INSTALLATION("install", true, () -> new GitHubUpdater(true)),
    HOMEBREW("homebrew", true, () -> new HomebrewUpdater()),
    CHOCO("choco", true, () -> new ChocoUpdater());

    private static XPipeDistributionType type;

    @Getter
    private final String id;

    @Getter
    private final boolean supportsUrls;

    private final Supplier<UpdateHandler> updateHandlerSupplier;
    private UpdateHandler updateHandler;

    XPipeDistributionType(String id, boolean supportsUrls, Supplier<UpdateHandler> updateHandlerSupplier) {
        this.id = id;
        this.supportsUrls = supportsUrls;
        this.updateHandlerSupplier = updateHandlerSupplier;
    }

    public static void init() {
        if (type != null) {
            return;
        }

        if (!ModuleHelper.isImage()) {
            type = DEVELOPMENT;
            return;
        }

        if (!XPipeSession.get().isNewBuildSession()) {
            var cached = AppCache.get("dist", String.class, () -> null);
            var cachedType = Arrays.stream(values())
                    .filter(xPipeDistributionType ->
                            xPipeDistributionType.getId().equals(cached))
                    .findAny()
                    .orElse(null);
            if (cachedType != null) {
                type = cachedType;
                return;
            }
        }

        var det = determine();

        // Don't cache unknown type
        if (det == UNKNOWN) {
            return;
        }

        type = det;
        AppCache.update("dist", type.getId());
        TrackEvent.withInfo("Determined distribution type")
                .tag("type", type.getId())
                .handle();
    }

    public static XPipeDistributionType get() {
        if (type == null) {
            TrackEvent.withWarn("Distribution type requested before init").handle();
            return UNKNOWN;
        }

        return type;
    }

    public static XPipeDistributionType determine() {
        if (!XPipeInstallation.isInstallationDistribution()) {
            return PORTABLE;
        }

        if (!LocalShell.isLocalShellInitialized()) {
            return UNKNOWN;
        }

        try (var sc = LocalShell.getShell()) {
            // In theory, we can also add  && !AppProperties.get().isStaging() here, but we want to replicate the
            // production behavior
            if (OsType.getLocal().equals(OsType.WINDOWS)) {
                try (var chocoOut =
                        sc.command("choco search --local-only -r xpipe").start()) {
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
            }

            // In theory, we can also add  && !AppProperties.get().isStaging() here, but we want to replicate the
            // production behavior
            if (OsType.getLocal().equals(OsType.MACOS)) {
                try (var brewOut = sc.command("brew list --casks --versions").start()) {
                    var out = brewOut.readStdoutDiscardErr();
                    if (brewOut.getExitCode() == 0) {
                        if (out.lines().anyMatch(s -> {
                            var split = s.split(" ");
                            return split.length == 2
                                    && split[0].equals("xpipe")
                                    && split[1].equals(AppProperties.get().getVersion());
                        })) {
                            return HOMEBREW;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }

        return XPipeDistributionType.NATIVE_INSTALLATION;
    }

    public UpdateHandler getUpdateHandler() {
        if (updateHandler == null) {
            updateHandler = updateHandlerSupplier.get();
        }
        return updateHandler;
    }
}
