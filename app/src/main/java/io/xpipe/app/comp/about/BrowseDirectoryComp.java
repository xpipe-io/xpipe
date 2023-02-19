package io.xpipe.app.comp.about;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.UserReportComp;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.DynamicOptionsBuilder;
import io.xpipe.app.util.ExternalEditor;
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
                            ExternalEditor.get()
                                    .openInEditor(AppLogs.get()
                                            .getSessionLogsDirectory()
                                            .resolve("xpipe.log")
                                            .toString());
                        }),
                        null)
                .addComp(
                        "logFiles",
                        new ButtonComp(AppI18n.observable("openLogsDirectory"), () -> {
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
