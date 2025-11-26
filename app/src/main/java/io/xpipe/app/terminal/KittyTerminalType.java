package io.xpipe.app.terminal;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppNames;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.process.ShellTemp;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FilePath;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;

public interface KittyTerminalType extends ExternalTerminalType, TrackableTerminalType {

    ExternalTerminalType KITTY_LINUX = new Linux();
    ExternalTerminalType KITTY_MACOS = new MacOs();

    private static FilePath getSocket() throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var temp = ShellTemp.createUserSpecificTempDataDirectory(sc, null);
            sc.executeSimpleCommand(sc.getShellDialect().getMkdirsCommand(temp.toString()));
            return temp.join(AppNames.ofCurrent().getSnakeName() + "_kitty");
        }
    }

    private static void open(TerminalLaunchConfiguration configuration, CommandBuilder socketWrite, boolean preferTab)
            throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            for (int i = 0; i < configuration.getPanes().size(); i++) {
                var payload = JsonNodeFactory.instance.objectNode();
                var args = configuration.single().getDialectLaunchCommand().buildBaseParts(sc);
                var argsArray = payload.putArray("args");
                args.forEach(argsArray::add);
                payload.put("tab_title", configuration.getColoredTitle());
                
                var type = i == 0 ? (preferTab ? "tab" : "os-window") : "window";
                payload.put("type", type);

                var json = JsonNodeFactory.instance.objectNode();
                json.put("cmd", "launch");
                json.set("payload", payload);
                json.putArray("version").add(0).add(14).add(2);
                var jsonString = json.toString();
                var echoString = "'\\eP@kitty-cmd" + jsonString + "\\e\\\\'";

                sc.command(CommandBuilder.of()
                        .add("printf", echoString, "|")
                        .add(socketWrite)
                        .addFile(getSocket()))
                        .execute();

                if (i == 0) {
                    setLayout(socketWrite);
                }
            }
        }
    }

    private static void setLayout(CommandBuilder socketWrite) throws Exception {
        var layout = switch (AppPrefs.get().terminalSplitStrategy().getValue()) {
            case HORIZONTAL -> "horizontal";
            case VERTICAL -> "vertical";
            case BALANCED -> "grid";
        };

        try (var sc = LocalShell.getShell().start()) {
            var payload = JsonNodeFactory.instance.objectNode();
            payload.put("layout", layout);

            var json = JsonNodeFactory.instance.objectNode();
            json.put("cmd", "set-enabled-layouts");
            json.set("payload", payload);
            json.putArray("version").add(0).add(14).add(2);

            var jsonString = json.toString();
            var echoString = "'\\eP@kitty-cmd" + jsonString + "\\e\\\\'";

            sc.command(CommandBuilder.of()
                            .add("printf", echoString, "|")
                            .add(socketWrite)
                            .addFile(getSocket()))
                    .execute();
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
    default TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
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
    default boolean useColoredTitle() {
        return true;
    }

    class Linux implements KittyTerminalType {

        @Override
        public int getProcessHierarchyOffset() {
            return LocalShell.getDialect() == ShellDialects.BASH ? 0 : 1;
        }

        public boolean isAvailable() {
            try (ShellControl pc = LocalShell.getShell()) {
                return pc.view().findProgram("kitty").isPresent();
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
            open(configuration, socketWrite, configuration.isPreferTabs());
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

                sc.command(CommandBuilder.of()
                                .add("kitty")
                                .add(
                                        "-o",
                                        "allow_remote_control=socket-only",
                                        "--listen-on",
                                        "unix:" + getSocket(),
                                        "--detach"))
                        .execute();

                for (int i = 0; i < 50; i++) {
                    ThreadHelper.sleep(100);
                    try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
                        if (channel.connect(UnixDomainSocketAddress.of(socket.asLocalPath()))) {
                            break;
                        }
                    } catch (IOException ignored) {
                    }
                }

                return true;
            }
        }
    }

    class MacOs implements ExternalApplicationType.MacApplication, KittyTerminalType {

        @Override
        public int getProcessHierarchyOffset() {
            return LocalShell.getDialect() == ShellDialects.ZSH ? 1 : 0;
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
            open(configuration, socketWrite, configuration.isPreferTabs());
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

                sc.command(CommandBuilder.of()
                                .add("open", "-n", "-a", "kitty.app", "--args")
                                .add("-o", "allow_remote_control=socket-only", "--listen-on", "unix:" + getSocket()))
                        .execute();

                for (int i = 0; i < 50; i++) {
                    ThreadHelper.sleep(100);
                    try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
                        if (channel.connect(UnixDomainSocketAddress.of(socket.asLocalPath()))) {
                            break;
                        }
                    } catch (IOException ignored) {
                    }
                }

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
