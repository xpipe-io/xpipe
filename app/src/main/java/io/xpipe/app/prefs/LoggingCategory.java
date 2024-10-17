package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LicensedFeature;
import io.xpipe.app.util.OptionsBuilder;

import java.io.IOException;
import java.nio.file.Files;

public class LoggingCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "logging";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var feature = LicenseProvider.get().getFeature("logging");
        var supported = feature.isSupported() || feature.isPreviewSupported();
        var title = AppI18n.observable("sessionLogging")
                .map(s -> s + (supported
                        ? ""
                        : " (Pro)"));
        return new OptionsBuilder()
                .addTitle(title)
                .sub(new OptionsBuilder()
                        .nameAndDescription("enableTerminalLogging")
                        .addToggle(prefs.enableTerminalLogging)
                        .disable(!supported)
                        .nameAndDescription("terminalLoggingDirectory")
                        .addComp(new ButtonComp(AppI18n.observable("openSessionLogs"), () -> {
                                    var dir = AppProperties.get().getDataDir().resolve("sessions");
                                    try {
                                        Files.createDirectories(dir);
                                        DesktopHelper.browsePathLocal(dir);
                                    } catch (IOException e) {
                                        ErrorEvent.fromThrowable(e).handle();
                                    }
                                })
                                .disable(prefs.enableTerminalLogging.not())))
                .buildComp();
    }
}
