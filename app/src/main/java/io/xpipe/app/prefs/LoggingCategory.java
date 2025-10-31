package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.util.*;

import java.io.IOException;
import java.nio.file.Files;

public class LoggingCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "logging";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2t-text-box-search-outline");
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("sessionLogging")
                .sub(new OptionsBuilder()
                        .pref(prefs.enableTerminalLogging)
                        .documentationLink(DocumentationLink.API)
                        .addToggle(prefs.enableTerminalLogging)
                        .nameAndDescription("terminalLoggingDirectory")
                        .addComp(new ButtonComp(AppI18n.observable("openSessionLogs"), () -> {
                                    var dir = AppProperties.get().getDataDir().resolve("sessions");
                                    try {
                                        Files.createDirectories(dir);
                                        DesktopHelper.browseFile(dir);
                                    } catch (IOException e) {
                                        ErrorEventFactory.fromThrowable(e).handle();
                                    }
                                })
                                .disable(prefs.enableTerminalLogging.not())))
                .buildComp();
    }
}
