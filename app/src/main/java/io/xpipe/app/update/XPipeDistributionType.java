package io.xpipe.app.update;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.XPipeInstallation;

import lombok.Getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Supplier;

public enum XPipeDistributionType {
    UNKNOWN("unknown", false, () -> new GitHubUpdater(false)),
    DEVELOPMENT("development", true, () -> new GitHubUpdater(false)),
    PORTABLE("portable", false, () -> new PortableUpdater(true)),
    NATIVE_INSTALLATION("install", true, () -> new GitHubUpdater(true)),
    HOMEBREW("homebrew", true, () -> new PortableUpdater(true)),
    APT_REPO("apt", true, () -> new PortableUpdater(true)),
    RPM_REPO("rpm", true, () -> new PortableUpdater(true)),
    WEBTOP("webtop", true, () -> new PortableUpdater(false)),
    CHOCO("choco", true, () -> new PortableUpdater(true));

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

        var det = determine();

        // Don't cache unknown type
        if (det == UNKNOWN) {
            return;
        }

        type = det;
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
        var base = XPipeInstallation.getCurrentInstallationBasePath();
        if (OsType.getLocal().equals(OsType.MACOS)) {
            if (!base.toString().equals(XPipeInstallation.getLocalDefaultInstallationBasePath())) {
                return PORTABLE;
            }

            try {
                var process = new ProcessBuilder("pkgutil", "--pkg-info", AppProperties.get().isStaging() ? "io.xpipe.xpipe-ptb" : "io.xpipe.xpipe")
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                process.waitFor();
                if (process.exitValue() != 0) {
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

        if (!LocalShell.isLocalShellInitialized()) {
            return UNKNOWN;
        }

        if (OsType.getLocal() == OsType.LINUX && Files.isDirectory(Path.of("/kclient"))) {
            return WEBTOP;
        }

            try (var sc = LocalShell.getShell().start()) {
                // In theory, we can also add  && !AppProperties.get().isStaging() here, but we want to replicate the
                // production behavior
                if (OsType.getLocal().equals(OsType.WINDOWS)) {
                    var out = sc.command("choco search --local-only -r xpipe").readStdoutIfPossible();
                    if (out.isPresent()) {
                        var split = out.get().split("\\|");
                        if (split.length == 2) {
                            var version = split[1];
                            if (AppProperties.get().getVersion().equals(version)) {
                                return CHOCO;
                            }
                        }
                    }
                }

                // In theory, we can also add  && !AppProperties.get().isStaging() here, but we want to replicate the
                // production behavior
                if (OsType.getLocal().equals(OsType.MACOS)) {
                    var out = sc.command("brew list --casks --versions").readStdoutIfPossible();
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
                        var aptOut = sc.command("apt show xpipe").readStdoutIfPossible();
                        if (aptOut.isPresent()) {
                            var fromRepo = aptOut.get().lines().anyMatch(s -> {
                                return s.contains("APT-Sources") && s.contains("apt.xpipe.io");
                            });
                            if (fromRepo) {
                                return APT_REPO;
                            }
                        }

                        var yumRepo = sc.command(CommandBuilder.of().add("test", "-f").addFile("/etc/yum.repos.d/xpipe.repo")).executeAndCheck();
                        if (yumRepo) {
                            return RPM_REPO;
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
