package io.xpipe.app.terminal;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ScriptHelper;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LicenseRequiredException;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import lombok.*;
import lombok.experimental.NonFinal;

import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Value
@RequiredArgsConstructor
public class TerminalLaunchConfiguration {

    DataStoreColor color;
    String coloredTitle;
    String cleanTitle;
    boolean preferTabs;
    List<TerminalPaneConfiguration> panes;

    public TerminalPaneConfiguration single() {
        if (panes.size() != 1) {
            throw new IllegalStateException("Not a single pane config");
        }

        return panes.getFirst();
    }

    public TerminalLaunchConfiguration withPanes(List<TerminalPaneConfiguration> panes) {
        return new TerminalLaunchConfiguration(color, coloredTitle, cleanTitle, preferTabs, panes);
    }
}
