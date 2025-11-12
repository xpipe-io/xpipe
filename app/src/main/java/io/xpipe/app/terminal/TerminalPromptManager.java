package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.ShellControl;

public class TerminalPromptManager {

    public static void configurePromptScript(ShellControl sc) {
        var p = AppPrefs.get().terminalPrompt().getValue();
        if (p == null) {
            return;
        }

        try {
            sc.withInitSnippet(p.terminalCommand(), false);
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }
    }
}
