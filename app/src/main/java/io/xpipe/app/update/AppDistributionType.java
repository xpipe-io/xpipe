package io.xpipe.app.update;

import io.xpipe.app.core.*;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.LocalExec;
import io.xpipe.app.util.Translatable;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellScript;
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
    HOMEBREW("homebrew", true, () -> {
        var pkg = AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
        return new CommandUpdater(
                ShellScript.lines("brew upgrade --cask xpipe-io/tap/" + pkg, AppRestart.getTerminalRestartCommand()));
    }),
    APT_REPO("apt", true, () -> {
        var pkg = AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
        return new CommandUpdater(ShellScript.lines(
                "echo \"+ sudo apt update && sudo apt install -y " + pkg + "\"",
                "sudo apt update",
                "sudo apt install -y " + pkg,
                AppRestart.getTerminalRestartCommand()));
    }),
    RPM_REPO("rpm", true, () -> {
        var pkg = AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
        return new CommandUpdater(ShellScript.lines(
                "echo \"+ sudo yum upgrade " + pkg + " --refresh -y\"",
                "sudo yum upgrade " + pkg + " --refresh -y",
                AppRestart.getTerminalRestartCommand()));
    }),
    AUR("aur", true, () -> {
        var pkg = AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
        return new CommandUpdater(ShellScript.lines(
                "echo \"+ git clone https://aur.archlinux.org/" + pkg + ".  && makepkg -si\"",
                "cd $(mktemp -d) && git clone https://aur.archlinux.org/" + pkg + " . && makepkg -si --noconfirm",
                AppRestart.getTerminalRestartCommand()));
    }),
    WEBTOP("webtop", true, () -> new WebtopUpdater()),
    CHOCO("choco", true, () -> new ChocoUpdater()),
    WINGET("winget", true, () -> new WingetUpdater());

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

        if (!AppProperties.get().isImage()) {
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
        var current = XPipeInstallation.getCurrentInstallationBasePath()
                .resolve(XPipeInstallation.getDaemonExecutablePath(OsType.getLocal()))
                .toString();
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
            if (!base.equals(XPipeInstallation.getLocalDefaultInstallationBasePath())) {
                return PORTABLE;
            }

            try {
                var r = LocalExec.readStdoutIfPossible(
                        "pkgutil",
                        "--pkg-info",
                        AppProperties.get().isStaging() ? "io.xpipe.xpipe-ptb" : "io.xpipe.xpipe");
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
                if (OsType.getLocal() == OsType.LINUX && Files.exists(base.resolve("aur"))) {
                    return AUR;
                }

                return PORTABLE;
            }
        }

        if (OsType.getLocal() == OsType.LINUX && Files.isDirectory(Path.of("/kclient"))) {
            return WEBTOP;
        }

        if (OsType.getLocal().equals(OsType.WINDOWS) && !AppProperties.get().isStaging()) {
            var chocoOut = LocalExec.readStdoutIfPossible("choco", "list", "xpipe");
            if (chocoOut.isPresent()) {
                if (chocoOut.get().contains("xpipe") && chocoOut.get().contains(AppProperties.get().getVersion())) {
                    return CHOCO;
                }
            }

//            var wingetOut = LocalExec.readStdoutIfPossible("winget", "show", "--id", "xpipe-io.xpipe", "--source", "--winget");
//            if (wingetOut.isPresent()) {
//                if (wingetOut.get().contains("xpipe-io.xpipe") && wingetOut.get().contains(AppProperties.get().getVersion())) {
//                    return WINGET;
//                }
//            }
        }

        if (OsType.getLocal().equals(OsType.MACOS)) {
            var out = LocalExec.readStdoutIfPossible("/opt/homebrew/bin/brew", "list", "--casks", "--versions");
            if (out.isPresent()) {
                if (out.get().lines().anyMatch(s -> {
                    var split = s.split(" ");
                    return split.length == 2
                            && split[0].equals(AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe")
                            && split[1].equals(AppProperties.get().getVersion());
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

        // Fix for community AUR builds that use the RPM dist
        if (OsType.getLocal() == OsType.LINUX && Files.exists(Path.of("/etc/arch-release"))) {
            return PORTABLE;
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
