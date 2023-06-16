package io.xpipe.ext.base.actions;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.CommandControl;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.ShellStore;
import javafx.beans.value.ObservableValue;
import lombok.Value;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SampleAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public boolean requiresJavaFXPlatform() {
            // Do we require the JavaFX platform to be running?
            return false;
        }

        @Override
        public void execute() throws Exception {
            var docker = new LocalStore();
            // Start a shell control from the docker connection store
            try (ShellControl sc = docker.control().start()) {
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
                try (CommandControl cc = sc.command("ls").start()) {
                    // Discard stderr
                    cc.discardErr();

                    // Read the stdout lines as a stream
                    BufferedReader reader = new BufferedReader(new InputStreamReader(cc.getStdout(), cc.getCharset()));
                    // We don't have to close this stream here, that will be automatically done by the command control
                    // after the try-with block
                    reader.lines().filter(s -> !s.isBlank()).forEach(s -> {
                        System.out.println(s);
                    });

                    // Waits for command completion and returns exit code
                    if (cc.getExitCode() != 0) {
                        // Handle failure
                    }
                }

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
                        .elevated("kill")
                        .withWorkingDirectory("/")
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

    @Override
    public DataStoreCallSite<?> getDataStoreCallSite() {
        // Call sites represent different ways of invoking the action.
        // In this case, this represents a button that is shown for all stored shell connections.
        return new DataStoreCallSite<ShellStore>() {

            @Override
            public Action createAction(ShellStore store) {
                return new Action(DataStorage.get().getStoreEntry(store));
            }

            @Override
            public Class<ShellStore> getApplicableClass() {
                // For which general type of connection store to make this action available.
                return ShellStore.class;
            }

            @Override
            public boolean isApplicable(ShellStore o) {
                // Allows you to individually check whether this action should be available for the specific store.
                // In this case it should only be available for remote shell connections, not local ones.
                return !ShellStore.isLocal(o);
            }

            @Override
            public ObservableValue<String> getName(ShellStore store) {
                // The displayed name of the action, allows you to use translation keys.
                return AppI18n.observable("installConnector");
            }

            @Override
            public String getIcon(ShellStore store) {
                // The ikonli icon of the button.
                return "mdi2c-code-greater-than";
            }
        };
    }
}
