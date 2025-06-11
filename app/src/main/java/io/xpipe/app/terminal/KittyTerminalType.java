package io.xpipe.app.terminal;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.XPipeInstallation;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public interface KittyTerminalType extends ExternalTerminalType, TrackableTerminalType {

    ExternalTerminalType KITTY_LINUX = new Linux();
    ExternalTerminalType KITTY_MACOS = new MacOs();

    private static FilePath getSocket() throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var temp = ShellTemp.createUserSpecificTempDataDirectory(sc, null);
            sc.executeSimpleCommand(sc.getShellDialect().getMkdirsCommand(temp.toString()));
            return temp.join("xpipe_kitty");
        }
    }

    private static void open(TerminalLaunchConfiguration configuration, CommandBuilder socketWrite) throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var payload = JsonNodeFactory.instance.objectNode();
            var args = configuration.getDialectLaunchCommand().buildBaseParts(sc);
            var argsArray = payload.putArray("args");
            args.forEach(argsArray::add);
            payload.put("tab_title", configuration.getColoredTitle());
            payload.put("type", "tab");
            payload.put("logo_alpha", 0.01);
            payload.put(
                    "logo", XPipeInstallation.getLocalDefaultInstallationIcon().toString());

            var json = JsonNodeFactory.instance.objectNode();
            json.put("cmd", "launch");
            json.set("payload", payload);
            json.putArray("version").add(0).add(14).add(2);
            var jsonString = json.toString();
            var echoString = "'\\eP@kitty-cmd" + jsonString + "\\e\\\\'";

            sc.executeSimpleCommand(CommandBuilder.of()
                    .add("printf", echoString, "|")
                    .add(socketWrite)
                    .addFile(getSocket()));
        }
    }

    private static void closeInitial(CommandBuilder socketWrite) throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var payload = JsonNodeFactory.instance.objectNode();
            payload.put("match", "not recent:0");

            var json = JsonNodeFactory.instance.objectNode();
            json.put("cmd", "close-tab");
            json.set("payload", payload);
            json.putArray("version").add(0).add(14).add(2);
            var jsonString = json.toString();
            var echoString = "'\\eP@kitty-cmd" + jsonString + "\\e\\\\'";

            sc.executeSimpleCommand(CommandBuilder.of()
                    .add("printf", echoString, "|")
                    .add(socketWrite)
                    .addFile(getSocket()));
        }
    }

    @Override
    default String getWebsite() {
        return "https://github.com/kovidgoyal/kitty";
    }

    @Override
    default boolean isRecommended() {
        // There are some race conditions with the socket, although that should be fixed to some degree
        return true;
    }

    @Override
    default TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.TABBED;
    }

    @Override
    default boolean useColoredTitle() {
        return true;
    }

    class Linux implements KittyTerminalType {

        @Override
        public int getProcessHierarchyOffset() {
            return ProcessControlProvider.get().getEffectiveLocalDialect() == ShellDialects.BASH ? 0 : 1;
        }

        public boolean isAvailable() {
            try (ShellControl pc = LocalShell.getShell()) {
                return CommandSupport.findProgram(pc, "kitty").isPresent();
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().handle();
                return false;
            }
        }

        @Override
        public String getId() {
            return "app.kitty";
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            try (var sc = LocalShell.getShell().start()) {
                CommandSupport.isInPathOrThrow(sc, "kitty", "Kitty", null);
                CommandSupport.isInPathOrThrow(sc, "socat", "socat", null);
            }

            var toClose = prepare();
            var socketWrite = CommandBuilder.of().add("socat", "-");
            open(configuration, socketWrite);
            if (toClose) {
                closeInitial(socketWrite);
            }
        }

        private boolean prepare() throws Exception {
            var socket = getSocket();
            try (var sc = LocalShell.getShell().start()) {
                if (sc.executeSimpleBooleanCommand(
                        "test -w " + sc.getShellDialect().fileArgument(socket))) {
                    return false;
                }

                var time = System.currentTimeMillis();
                sc.executeSimpleCommand(CommandBuilder.of()
                        .add("kitty")
                        .add(
                                "-o",
                                "allow_remote_control=socket-only",
                                "--listen-on",
                                "unix:" + getSocket(),
                                "--detach"));
                var elapsed = System.currentTimeMillis() - time;
                // Good heuristic on how long to wait
                ThreadHelper.sleep(5 * elapsed);
                return true;
            }
        }
    }

    class MacOs implements ExternalApplicationType.MacApplication, KittyTerminalType {

        @Override
        public int getProcessHierarchyOffset() {
            return ProcessControlProvider.get().getEffectiveLocalDialect() == ShellDialects.ZSH ? 1 : 0;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            // We use the absolute path to force the usage of macOS netcat
            // Homebrew versions have different option formats
            try (var sc = LocalShell.getShell().start()) {
                CommandSupport.isInPathOrThrow(sc, "/usr/bin/nc", "Netcat", null);
            }

            var toClose = prepare();
            var socketWrite = CommandBuilder.of().add("/usr/bin/nc", "-U");
            open(configuration, socketWrite);
            if (toClose) {
                closeInitial(socketWrite);
            }
        }

        private boolean prepare() throws Exception {
            var socket = getSocket();
            try (var sc = LocalShell.getShell().start()) {
                if (sc.executeSimpleBooleanCommand(
                        "test -w " + sc.getShellDialect().fileArgument(socket))) {
                    return false;
                }

                var time = System.currentTimeMillis();
                sc.executeSimpleCommand(CommandBuilder.of()
                        .add("open", "-n", "-a", "kitty.app", "--args")
                        .add("-o", "allow_remote_control=socket-only", "--listen-on", "unix:" + getSocket()));
                var elapsed = System.currentTimeMillis() - time;
                // Good heuristic on how long to wait
                ThreadHelper.sleep(15 * elapsed);
                return true;
            }
        }

        @Override
        public String getApplicationName() {
            return "kitty";
        }

        @Override
        public String getId() {
            return "app.kitty";
        }
    }
}
