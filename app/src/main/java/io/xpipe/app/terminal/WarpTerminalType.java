package io.xpipe.app.terminal;

import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.TerminalInitFunction;

public class WarpTerminalType extends ExternalTerminalType.MacOsType {

    public WarpTerminalType() {
        super("app.warp", "Warp");
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
        return "https://www.warp.dev/";
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
    public boolean shouldClear() {
        return false;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        LocalShell.getShell()
                .executeSimpleCommand(CommandBuilder.of()
                        .add("open", "-a")
                        .addQuoted("Warp.app")
                        .addFile(configuration.getScriptFile()));
    }

    @Override
    public TerminalInitFunction additionalInitCommands() {
        return TerminalInitFunction.of(sc -> {
            if (sc.getShellDialect() == ShellDialects.ZSH) {
                return "printf '\\eP$f{\"hook\": \"SourcedRcFileForWarp\", \"value\": { \"shell\": \"zsh\"}}\\x9c'";
            }
            if (sc.getShellDialect() == ShellDialects.BASH) {
                return "printf '\\eP$f{\"hook\": \"SourcedRcFileForWarp\", \"value\": { \"shell\": \"bash\"}}\\x9c'";
            }
            if (sc.getShellDialect() == ShellDialects.FISH) {
                return "printf '\\eP$f{\"hook\": \"SourcedRcFileForWarp\", \"value\": { \"shell\": \"fish\"}}\\x9c'";
            }
            return null;
        });
    }
}
