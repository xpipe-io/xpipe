package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.update.GitHubUpdater;
import io.xpipe.app.update.PortableUpdater;
import io.xpipe.app.update.UpdateHandler;
import io.xpipe.app.util.LocalExec;
import io.xpipe.app.util.Translatable;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.XPipeInstallation;

import javafx.beans.value.ObservableValue;
import lombok.Getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Supplier;

public enum AppDistributionType implements Translatable {
    UNKNOWN("unknown", false, () -> new GitHubUpdater(false)),
    DEVELOPMENT("development", true, () -> new GitHubUpdater(false)),
    PORTABLE("portable", false, () -> new PortableUpdater(true)),
    NATIVE_INSTALLATION("install", true, () -> new GitHubUpdater(true)),
    HOMEBREW("homebrew", true, () -> new PortableUpdater(true)),
    APT_REPO("apt", true, () -> new PortableUpdater(true)),
    RPM_REPO("rpm", true, () -> new PortableUpdater(true)),
    WEBTOP("webtop", true, () -> new PortableUpdater(false)),
    CHOCO("choco", true, () -> new PortableUpdater(true));

    private static AppDistributionType type;

    @Getter
    private final String id;

    @Getter
    private final boolean supportsUrls;

    private final Supplier<UpdateHandler> updateHandlerSupplier;
    private UpdateHandler updateHandler;

    AppDistributionType(String id, boolean supportsUrls, Supplier<UpdateHandler> updateHandlerSupplier) {
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

        if (!AppProperties.get().isNewBuildSession() && !isDifferentDaemonExecutable()) {
            var cached = AppCache.getNonNull("dist", String.class, () -> null);
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

    private static boolean isDifferentDaemonExecutable() {
        var cached = AppCache.getNonNull("daemonExecutable", String.class, () -> null);
        var current = XPipeInstallation.getCurrentInstallationBasePath().resolve(XPipeInstallation.getDaemonExecutablePath(OsType.getLocal())).toString();
        if (current.equals(cached)) {
            return false;
        }

        AppCache.update("daemonExecutable", current);
        return true;
    }

    public static AppDistributionType get() {
        if (type == null) {
            TrackEvent.withWarn("Distribution type requested before init").handle();
            return UNKNOWN;
        }

        return type;
    }

    public static AppDistributionType determine() {
        var base = XPipeInstallation.getCurrentInstallationBasePath();
        if (OsType.getLocal().equals(OsType.MACOS)) {
            if (!base.toString().equals(XPipeInstallation.getLocalDefaultInstallationBasePath())) {
                return PORTABLE;
            }

            try {
                var r = LocalExec.readStdoutIfPossible("pkgutil", "--pkg-info", AppProperties.get().isStaging() ? "io.xpipe.xpipe-ptb" : "io.xpipe.xpipe");
                if (r.isEmpty()) {
                    return PORTABLE;
                }
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).omit().handle();
                return PORTABLE;
            }
        } else {
            var file = base.resolve("installation");
            if (!Files.exists(file)) {
                return PORTABLE;
            }
        }

        if (OsType.getLocal() == OsType.LINUX && Files.isDirectory(Path.of("/kclient"))) {
            return WEBTOP;
        }

        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            var out = LocalExec.readStdoutIfPossible("choco", "list", "xpipe");
            if (out.isPresent()) {
                if (out.get().contains("xpipe")) {
                    return CHOCO;
                }
            }
        }

        // In theory, we can also add  && !AppProperties.get().isStaging() here, but we want to replicate the
        // production behavior
        if (OsType.getLocal().equals(OsType.MACOS)) {
            var out = LocalExec.readStdoutIfPossible("brew", "list", "--casks", "--versions");
            if (out.isPresent()) {
                if (out.get().lines().anyMatch(s -> {
                    var split = s.split(" ");
                    return split.length == 2 && split[0].equals("xpipe") && split[1].equals(AppProperties.get().getVersion());
                })) {
                    return HOMEBREW;
                }
            }
        }

        if (OsType.getLocal() == OsType.LINUX) {
            if (base.startsWith("/opt")) {
                var aptOut = LocalExec.readStdoutIfPossible("apt", "show", "xpipe");
                if (aptOut.isPresent()) {
                    var fromRepo = aptOut.get().lines().anyMatch(s -> {
                        return s.contains("APT-Sources") && s.contains("apt.xpipe.io");
                    });
                    if (fromRepo) {
                        return APT_REPO;
                    }
                }

                var yumRepo = LocalExec.readStdoutIfPossible("test", "-f", "/etc/yum.repos.d/xpipe.repo");
                if (yumRepo.isPresent()) {
                    return RPM_REPO;
                }
            }
        }

        return AppDistributionType.NATIVE_INSTALLATION;
    }

    public UpdateHandler getUpdateHandler() {
        if (updateHandler == null) {
            updateHandler = updateHandlerSupplier.get();
        }
        return updateHandler;
    }

    @Override
    public ObservableValue<String> toTranslatedString() {
        return AppI18n.observable(getId() + "Dist");
    }
}
