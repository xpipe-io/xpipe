package io.xpipe.app.terminal;

import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.TerminalInitFunction;
import io.xpipe.core.util.FailableFunction;

public class WaveTerminalType extends ExternalTerminalType.MacOsType {

    public WaveTerminalType() {
        super("app.wave", "Wave");
    }

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.TABBED;
    }

    @Override
    public int getProcessHierarchyOffset() {
        return 2;
    }

    @Override
    public String getWebsite() {
        return "https://www.waveterm.dev/";
    }

    @Override
    public boolean isRecommended() {
        return true;
    }

    @Override
    public boolean supportsColoredTitle() {
        return true;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var wsh = sc.command(sc.getShellDialect().getWhichCommand("wsh")).readStdoutIfPossible();
            var def = sc.getOsType().getUserHomeDirectory(sc) + "/Library/Application Support/waveterm/bin/wsh";
            var absPath = wsh.orElse(def);
            sc.command(CommandBuilder.of().addFile(absPath).add("run", "--").add(configuration.getDialectLaunchCommand())).execute();
        }
    }
}
