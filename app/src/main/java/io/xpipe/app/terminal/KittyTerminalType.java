package io.xpipe.app.terminal;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.XPipeInstallation;

public class KittyTerminalType {

    public static final ExternalTerminalType KITTY_LINUX = new ExternalTerminalType() {

        @Override
        public String getId() {
            return "app.tabby";
        }

        @Override
        public boolean supportsTabs() {
            return true;
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            launchInstanceIfNeeded();
            open(configuration);
        }
    };

    private static FilePath getSocket() throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var temp = sc.getSystemTemporaryDirectory();
            return temp.join("xpipe_kitty");
        }
    }

    private static void launchInstanceIfNeeded() throws Exception {
        var socket = getSocket();
        try (var sc = LocalShell.getShell().start()) {
            if (sc.getShellDialect().createFileExistsCommand(sc,socket.toString()).executeAndCheck()) {
                return;
            }

            sc.executeSimpleCommand(CommandBuilder.of().add("kitty").add("-o", "allow_remote_control=socket-only", "--listen-on", "unix:" + getSocket(), "--detach"));
        }
    }

    private static void open(ExternalTerminalType.LaunchConfiguration configuration) throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var payload = JsonNodeFactory.instance.objectNode();
            payload.put("args", configuration.getDialectLaunchCommand().buildString(sc));
            payload.put("tab_title",configuration.getColoredTitle());
            payload.put("type", "tab");
            payload.put("logo_alpha", 0.01);
            payload.put("logo", XPipeInstallation.getLocalDefaultInstallationIcon().toString());

            var json = JsonNodeFactory.instance.objectNode();
            json.put("cmd", "launch");
            json.set("payload", payload);
            var jsonString = json.toString();
            var echoString = "'\\eP@kitty-cmd" + jsonString + "\\e\\\\'";

            sc.executeSimpleCommand(CommandBuilder.of().add("echo", "-en", echoString, "|", "socat", "-").addFile(getSocket()));
        }
    }
}
