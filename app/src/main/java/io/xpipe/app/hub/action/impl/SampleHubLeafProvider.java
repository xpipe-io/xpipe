package io.xpipe.app.hub.action.impl;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.process.CommandControl;
import io.xpipe.app.process.ElevationFunction;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.FilePath;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.BufferedReader;
import java.io.StringReader;

public class SampleHubLeafProvider implements HubLeafProvider<ShellStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.OPEN;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
        // Allows you to individually check whether this action should be available for the specific store.
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
        // The displayed name of the action, allows you to use translation keys.
        return AppI18n.observable("installConnector");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
        // The ikonli icon of the button.
        return new LabelGraphic.IconGraphic("mdi2c-code-greater-than");
    }

    @Override
    public Class<ShellStore> getApplicableClass() {
        // For which general type of connection store to make this action available.
        return ShellStore.class;
    }

    @Override
    public String getId() {
        return "sample";
    }

    @Jacksonized
    @SuperBuilder
    @SuppressWarnings("unused")
    public static class Action extends StoreAction<ShellStore> {

        @Override
        public void executeImpl() throws Exception {
            // Start a shell control
            try (ShellControl sc = ref.getStore().standaloneControl().start()) {
                // Once we are here, the shell connection is initialized and we can query all kinds of information

                // Query the detected shell dialect, e.g. cmd, powershell, sh, bash, etc.
                System.out.println(sc.getShellDialect());

                // Query the os type
                System.out.println(sc.getOsType());

                // Simple commands can be executed in one line
                // The shell dialects also provide the appropriate commands for common operations like echo for all
                // supported shells
                String echoOut =
                        sc.executeSimpleStringCommand(sc.getShellDialect().getEchoCommand("hello!", false));

                // You can also implement custom handling for more complex commands
                var lsOut = sc.command("ls").readStdoutOrThrow();
                // Read the stdout lines as a stream
                BufferedReader reader = new BufferedReader(new StringReader(lsOut));
                // We don't have to close this stream here, that will be automatically done by the command control
                // after the try-with block
                reader.lines().filter(s -> !s.isBlank()).forEach(s -> {
                    System.out.println(s);
                });

                // Commands can also be more complex and span multiple lines.
                // In this case, XPipe will internally write a command to a script file and then execute the script
                try (CommandControl cc = sc.command(
                                """
                                                    VAR="value"
                                                    echo "$VAR"
                                                    """)
                        .start()) {
                    // Reads stdout, stashes stderr. If the exit code is not 0, it will throw an exception with the
                    // stderr contents.
                    var output = cc.readStdoutOrThrow();
                }

                // More customization options
                // If the command should be run as root, the command will be executed with
                // sudo and the optional sudo password automatically provided by XPipe
                // by using the information from the connection store.
                // You can also set a custom working directory.
                try (CommandControl cc = sc.command("kill <pid>")
                        .elevated(ElevationFunction.elevated("kill"))
                        .withWorkingDirectory(FilePath.of("/"))
                        .start()) {
                    // Discard any output but throw an exception with the stderr contents if the exit code is not 0
                    cc.discardOrThrow();
                }

                // Start a bash sub shell. Useful if the login shell is different
                try (ShellControl bash = sc.subShell(ShellDialects.BASH).start()) {
                    // Let's write to a file
                    try (CommandControl cc = bash.command("cat > myfile.txt").start()) {
                        // Writing into stdin can also easily be done
                        cc.getStdin().write("my file content".getBytes(cc.getCharset()));
                        // Close stdin to send EOF. It will be reopened by the shell control after the command is done
                        cc.closeStdin();
                    }
                }
            }
        }
    }
}
