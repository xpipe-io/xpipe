package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.UserReportComp;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.FileOpener;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.util.XPipeInstallation;

import com.sun.management.HotSpotDiagnosticMXBean;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import javax.management.MBeanServer;

public class TroubleshootCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "troubleshoot";
    }

    @Override
    protected Comp<?> create() {
        OptionsBuilder b = new OptionsBuilder()
                .addTitle("troubleshootingOptions")
                .spacer(25)
                .addComp(
                        new TileButtonComp("reportIssue", "reportIssueDescription", "mdal-bug_report", e -> {
                                    var event = ErrorEvent.fromMessage("User Report");
                                    if (AppLogs.get().isWriteToFile()) {
                                        event.attachment(AppLogs.get().getSessionLogsDirectory());
                                    }
                                    UserReportComp.show(event.build());
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .separator()
                .addComp(
                        new TileButtonComp("launchDebugMode", "launchDebugModeDescription", "mdmz-refresh", e -> {
                                    OperationMode.executeAfterShutdown(() -> {
                                        var script = FileNames.join(
                                                XPipeInstallation.getCurrentInstallationBasePath()
                                                        .toString(),
                                                XPipeInstallation.getDaemonDebugScriptPath(OsType.getLocal()));
                                        // We can't use the SSH bridge
                                        var type = ExternalTerminalType.determineNonSshBridgeFallback(
                                                AppPrefs.get().terminalType().getValue());
                                        TerminalLauncher.openDirect(
                                                "XPipe Debug",
                                                sc -> sc.getShellDialect().runScriptCommand(sc, script),
                                                type);
                                    });
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .separator();

        if (AppLogs.get().isWriteToFile()) {
            b.addComp(
                            new TileButtonComp(
                                            "openCurrentLogFile",
                                            "openCurrentLogFileDescription",
                                            "mdmz-text_snippet",
                                            e -> {
                                                AppLogs.get().flush();
                                                FileOpener.openInTextEditor(AppLogs.get()
                                                        .getSessionLogsDirectory()
                                                        .resolve("xpipe.log")
                                                        .toString());
                                                e.consume();
                                            })
                                    .grow(true, false),
                            null)
                    .separator();
        }

        b.addComp(
                        new TileButtonComp(
                                        "openInstallationDirectory",
                                        "openInstallationDirectoryDescription",
                                        "mdomz-snippet_folder",
                                        e -> {
                                            DesktopHelper.browsePathLocal(
                                                    XPipeInstallation.getCurrentInstallationBasePath());
                                            e.consume();
                                        })
                                .grow(true, false),
                        null)
                .separator()
                .addComp(
                        new TileButtonComp(
                                        "clearUserData", "clearUserDataDescription", "mdi2t-trash-can-outline", e -> {
                                            var modal = ModalOverlay.of(
                                                    "clearUserDataTitle",
                                                    AppDialog.dialogTextKey("clearUserDataContent"));
                                            modal.withDefaultButtons(() -> {
                                                ThreadHelper.runFailableAsync(() -> {
                                                    var dir =
                                                            AppProperties.get().getDataDir();
                                                    try (var stream = Files.list(dir)) {
                                                        var dirs = stream.toList();
                                                        for (var path : dirs) {
                                                            if (path.getFileName()
                                                                            .toString()
                                                                            .equals("logs")
                                                                    || path.getFileName()
                                                                            .toString()
                                                                            .equals("shell")) {
                                                                continue;
                                                            }

                                                            FileUtils.deleteQuietly(path.toFile());
                                                        }
                                                    }
                                                    OperationMode.halt(0);
                                                });
                                            });
                                            modal.show();
                                            e.consume();
                                        })
                                .grow(true, false),
                        null)
                .separator()
                .addComp(
                        new TileButtonComp("clearCaches", "clearCachesDescription", "mdi2t-trash-can-outline", e -> {
                                    var modal = ModalOverlay.of(
                                            "clearCachesAlertTitle",
                                            AppDialog.dialogTextKey("clearCachesAlertContent"));
                                    modal.withDefaultButtons(() -> {
                                        AppCache.clear();
                                    });
                                    modal.show();
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .separator()
                .addComp(
                        new TileButtonComp("createHeapDump", "createHeapDumpDescription", "mdi2m-memory", e -> {
                                    heapDump();
                                    e.consume();
                                })
                                .grow(true, false),
                        null);
        return b.buildComp();
    }

    @SneakyThrows
    private static void heapDump() {
        var file = DesktopHelper.getDesktopDirectory().resolve("xpipe.hprof");
        FileUtils.deleteQuietly(file.toFile());
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
                server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
        mxBean.dumpHeap(file.toString(), true);
        DesktopHelper.browseFileInDirectory(file);
    }
}
