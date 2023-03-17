package io.xpipe.app.comp.about;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.UserReportComp;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.DynamicOptionsBuilder;
import io.xpipe.app.util.FileOpener;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.XPipeInstallation;
import javafx.scene.layout.Region;

public class BrowseDirectoryComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        return new DynamicOptionsBuilder(false)
                .addComp(
                        "issueReporter",
                        new ButtonComp(AppI18n.observable("reportIssue"), () -> {
                            var event = ErrorEvent.fromMessage("User Report");
                            if (AppLogs.get().isWriteToFile()) {
                                event.attachment(AppLogs.get().getSessionLogsDirectory());
                            }
                            UserReportComp.show(event.build());
                        }),
                        null)
                .addComp(
                        "logFile",
                        new ButtonComp(AppI18n.observable("openCurrentLogFile"), () -> {
                            FileOpener.openInTextEditor(AppLogs.get()
                                            .getSessionLogsDirectory()
                                            .resolve("xpipe.log")
                                            .toString());
                        }),
                        null)
                .addComp(
                        "launchDebugMode",
                        new ButtonComp(AppI18n.observable("launchDebugMode"), () -> {
                            OperationMode.executeAfterShutdown(() -> {
                                try (var sc = ShellStore.createLocal().create().start()) {
                                    var script = FileNames.join(XPipeInstallation.getCurrentInstallationBasePath().toString(), XPipeInstallation.getDaemonDebugScriptPath(sc.getOsType()));
                                    sc.executeSimpleCommand(ScriptHelper.createDetachCommand(sc, script));
                                }
                            });
                            DesktopHelper.browsePath(AppLogs.get().getSessionLogsDirectory());
                        }),
                        null)
                .addComp(
                        "installationFiles",
                        new ButtonComp(AppI18n.observable("openInstallationDirectory"), () -> {
                            DesktopHelper.browsePath(XPipeInstallation.getCurrentInstallationBasePath());
                        }),
                        null)
                .build();
    }
}
