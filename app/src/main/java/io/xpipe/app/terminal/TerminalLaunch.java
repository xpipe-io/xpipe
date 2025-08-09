package io.xpipe.app.terminal;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandControl;
import io.xpipe.app.process.ProcessControl;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.FailableFunction;
import io.xpipe.core.FilePath;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class TerminalLaunch {

    DataStoreEntry entry;
    String title;
    FilePath directory;
    ProcessControl command;
    UUID request;
    @Builder.Default
    boolean preferTabs = true;
    @Builder.Default
    boolean logIfEnabled = true;
    ExternalTerminalType terminal;

    public String getFullTitle() {
        return entry != null ? DataStorage.get().getStoreEntryDisplayName(entry) + (title != null ? " - " + title : "") : title != null ? title : "?";
    }

    public void launch() throws Exception {
        var type = AppPrefs.get().terminalType().getValue();
        if (type == null) {
            throw ErrorEventFactory.expected(new IllegalStateException(AppI18n.get("noTerminalSet")));
        }

        if (OperationMode.get() == null) {
            if (command instanceof CommandControl cc) {
                TerminalLauncher.openDirect(getFullTitle(), sc -> new ShellScript(cc.getTerminalCommand().buildFull(sc)), ExternalTerminalType.determineFallbackTerminalToOpen(type));
            }
            return;
        }

        TerminalLauncher.open(entry, getFullTitle(), directory, command, request != null ? request : UUID.randomUUID(), preferTabs, logIfEnabled, type);
    }

    public static class TerminalLaunchBuilder {

        public void launch() throws Exception {
            var l = build();
            l.launch();
        }

        public TerminalLaunchBuilder localScript(ShellScript script) {
            var c = LocalShell.getShell().command(script);
            return command(c);
        }

        public TerminalLaunchBuilder localScript(FailableFunction<ShellControl, ShellScript, Exception> script) throws Exception {
            var c = LocalShell.getShell().command(script.apply(LocalShell.getShell()));
            return command(c);
        }
    }
}
