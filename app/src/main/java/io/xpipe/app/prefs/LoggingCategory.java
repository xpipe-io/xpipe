package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.comp.store.StoreChoiceComp;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.terminal.TerminalMultiplexer;
import io.xpipe.app.terminal.TerminalProxyManager;
import io.xpipe.app.util.*;
import io.xpipe.core.process.OsType;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

public class LoggingCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "logging";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("sessionLogging")
                .sub(new OptionsBuilder()
                        .pref(prefs.enableTerminalLogging)
                        .addToggle(prefs.enableTerminalLogging)
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
