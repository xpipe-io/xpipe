package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.util.XPipeInstallation;

public interface WaveTerminalType extends ExternalTerminalType, TrackableTerminalType {

    ExternalTerminalType WAVE_WINDOWS = new Windows();
    ExternalTerminalType WAVE_LINUX = new Linux();
    ExternalTerminalType WAVE_MAC_OS = new MacOs();

    @Override
    default String getWebsite() {
        return "https://www.waveterm.dev/";
    }

    @Override
    default TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
    }

    @Override
    default boolean isRecommended() {
        return false;
    }

    @Override
    default boolean supportsColoredTitle() {
        return true;
    }

    @Override
    default String getId() {
        return "app.wave";
    }

    @Override
    default void launch(TerminalLaunchConfiguration configuration) throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var wsh = CommandSupport.findProgram(sc, "wsh");
            var env = sc.command(sc.getShellDialect().getPrintEnvironmentVariableCommand("WAVETERM_JWT")).readStdoutOrThrow();
            if (wsh.isEmpty() || env.isEmpty()) {
                var inPath = CommandSupport.findProgram(sc, "xpipe").isPresent();
                var msg = """
                The Wave integration requires XPipe to be launched from Wave itself to have access to its environment variables. Otherwise, XPipe does not have access to the token to control Wave.
                
                You can do this by running the command "%s" in a local terminal block inside Wave.
                """.formatted(inPath ? "xpipe open" : XPipeInstallation.getLocalDefaultCliExecutable() + " open");
                throw ErrorEvent.expected(new IllegalStateException(msg));
            }

            sc.command(CommandBuilder.of()
                            .addFile("wsh")
                            .add("run", "--forceexit", "--")
                            .add(configuration.getDialectLaunchCommand()))
                    .execute();
        }
    }

    class Windows implements WaveTerminalType {}

    class Linux implements WaveTerminalType {}

    class MacOs implements WaveTerminalType {}
}
