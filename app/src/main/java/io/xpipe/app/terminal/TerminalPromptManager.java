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

        try {
            sc.withInitSnippet(p.terminalCommand());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }
}
