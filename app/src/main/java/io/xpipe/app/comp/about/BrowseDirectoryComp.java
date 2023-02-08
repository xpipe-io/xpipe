package io.xpipe.app.comp.about;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.util.ExternalEditor;
import io.xpipe.app.issue.UserReportComp;
import io.xpipe.core.util.XPipeInstallation;
import io.xpipe.extension.I18n;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.DesktopHelper;
import javafx.scene.layout.Region;

public class BrowseDirectoryComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        return new DynamicOptionsBuilder(false)
                .addComp(
                        "issueReporter",
                        new ButtonComp(I18n.observable("reportIssue"), () -> {
                            var event = ErrorEvent.fromMessage("User Report");
                            if (AppLogs.get().isWriteToFile()) {
                                event.attachment(AppLogs.get().getSessionLogsDirectory());
                            }
                            UserReportComp.show(event.build());
                        }),
                        null)
                .addComp(
                        "logFile",
                        new ButtonComp(I18n.observable("openCurrentLogFile"), () -> {
                            ExternalEditor.get().openInEditor(AppLogs.get().getSessionLogsDirectory().resolve("xpipe.log").toString());
                        }),
                        null)
                .addComp(
                        "logFiles",
                        new ButtonComp(I18n.observable("openLogsDirectory"), () -> {
                            DesktopHelper.browsePath(AppLogs.get().getSessionLogsDirectory());
                        }),
                        null)
                .addComp(
                        "installationFiles",
                        new ButtonComp(I18n.observable("openInstallationDirectory"), () -> {
                            DesktopHelper.browsePath(XPipeInstallation.getLocalInstallationBasePath());
                        }),
                        null)
                .build();
    }
}
