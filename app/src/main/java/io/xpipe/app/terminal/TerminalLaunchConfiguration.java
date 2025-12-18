package io.xpipe.app.terminal;

import io.xpipe.app.storage.DataStoreColor;

import lombok.*;

import java.util.List;

@Value
@AllArgsConstructor
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
