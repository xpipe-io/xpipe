package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.UserReportComp;
import io.xpipe.app.util.*;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.XPipeInstallation;

import java.util.List;

public class TroubleshootComp extends Comp<CompStructure<?>> {

    private Comp<?> createActions() {
        return new OptionsBuilder()
                .addTitle("troubleshootingOptions")
                .spacer(13)
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
//                .addComp(
//                        new TileButtonComp("restart", "restartDescription", "mdmz-refresh", e -> {
//                                    OperationMode.executeAfterShutdown(() -> {
//                                        try (var sc = ShellStore.createLocal()
//                                                .control()
//                                                .start()) {
//                                            var script = FileNames.join(
//                                                    XPipeInstallation.getCurrentInstallationBasePath()
//                                                            .toString(),
//                                                    XPipeInstallation.getDaemonExecutablePath(sc.getOsType()));
//                                            sc.executeSimpleCommand(
//                                                    ScriptHelper.createDetachCommand(sc, "\"" + script + "\""));
//                                        }
//                                    });
//                                    e.consume();
//                                })
//                                .grow(true, false),
//                        null)
//                .separator()
                .addComp(
                        new TileButtonComp("launchDebugMode", "launchDebugModeDescription", "mdmz-refresh", e -> {
                                    OperationMode.executeAfterShutdown(() -> {
                                        try (var sc = ShellStore.createLocal()
                                                .control()
                                                .start()) {
                                            var script = FileNames.join(
                                                    XPipeInstallation.getCurrentInstallationBasePath()
                                                            .toString(),
                                                    XPipeInstallation.getDaemonDebugScriptPath(sc.getOsType()));
                                            if (sc.getOsType().equals(OsType.WINDOWS)) {
                                                sc.executeSimpleCommand(
                                                        ScriptHelper.createDetachCommand(sc, "\"" + script + "\""));
                                            } else {
                                                TerminalHelper.open("XPipe Debug", "\"" + script + "\"");
                                            }
                                        }
                                    });
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .separator()
                .addComp(
                        new TileButtonComp(
                                        "openCurrentLogFile",
                                        "openCurrentLogFileDescription",
                                        "mdmz-text_snippet",
                                        e -> {
                                            FileOpener.openInTextEditor(AppLogs.get()
                                                    .getSessionLogsDirectory()
                                                    .resolve("xpipe.log")
                                                    .toString());
                                            e.consume();
                                        })
                                .grow(true, false),
                        null)
                .separator()
                .addComp(
                        new TileButtonComp(
                                        "openInstallationDirectory",
                                        "openInstallationDirectoryDescription",
                                        "mdomz-snippet_folder",
                                        e -> {
                                            DesktopHelper.browsePath(
                                                    XPipeInstallation.getCurrentInstallationBasePath());
                                            e.consume();
                                        })
                                .grow(true, false),
                        null)
                .separator()
                .addComp(
                        new TileButtonComp("clearCaches", "clearCachesDescription", "mdi2t-trash-can-outline", e -> {
                                    ClearCacheAlert.show();
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .buildComp();
    }

    @Override
    public CompStructure<?> createBase() {
        var box = new VerticalComp(List.of(createActions()))
                .apply(s -> s.get().setFillWidth(true))
                .apply(struc -> struc.get().setSpacing(15))
                .styleClass("troubleshoot-tab")
                .apply(struc -> struc.get().setPrefWidth(600));
        return box.createStructure();
    }
}
