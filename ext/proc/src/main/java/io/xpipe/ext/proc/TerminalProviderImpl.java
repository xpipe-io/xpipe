package io.xpipe.ext.proc;

import io.xpipe.app.util.TerminalProvider;
import io.xpipe.extension.prefs.PrefsProvider;

public class TerminalProviderImpl extends TerminalProvider {

    @Override
    public void openInTerminal(String title, String command) throws Exception {
        var type = PrefsProvider.get(ProcPrefs.class).terminalType().getValue();
        type.launch(title, command);
    }
}
