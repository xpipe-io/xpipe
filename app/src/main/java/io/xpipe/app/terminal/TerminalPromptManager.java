package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.ShellControl;

public class TerminalPromptManager {

    public static void configurePromptScript(ShellControl sc) {
        var p = AppPrefs.get().terminalPrompt().getValue();
        if (p == null) {
            return;
        }

        var d = p.getSupportedDialects();
        if (!d.contains(sc.getShellDialect())) {
            return;
        }

        try {
            p.installIfNeeded(sc);
            sc.withInitSnippet(p.terminalCommand(sc));
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }
}
