package io.xpipe.ext.base.actions;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.CommandControl;
import io.xpipe.core.process.ShellControl;
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
            // Start a shell control from the shell connection store
            try (ShellControl sc = ((ShellStore) entry.getStore()).control().start()) {
                // Simple commands can be executed in one line
                // The shell dialects also provide the proper command syntax for common commands like echo
                String echoOut =
                        sc.executeSimpleStringCommand(sc.getShellDialect().getEchoCommand("hello!", false));

                // You can also implement custom handling for more complex commands
                try (CommandControl cc = sc.command("ls").start()) {
                    // Discard stderr
                    cc.discardErr();

                    // Read the stdout lines as a stream
                    BufferedReader reader = new BufferedReader(new InputStreamReader(cc.getStdout(), cc.getCharset()));
                    reader.lines().filter(s -> s != null).forEach(s -> {
                        System.out.println(s);
                    });

                    // Waits for command completion and returns exit code
                    if (cc.getExitCode() != 0) {
                        // Handle failure
                    }
                }

                // Commands can also be more complex and span multiple lines.
                // In this case, X-Pipe will internally write a command to a script file and then execute the script
                try (CommandControl cc = sc.command(
                    """
                    VAR = "value"
                    echo "$VAR"
                    """
                    ).start()) {
                    var output = cc.readOrThrow();
                }

                // More customization options
                // If the command should be run as root, the command will be executed with
                // sudo and the optional sudo password automatically provided by X-Pipe.
                // You can also set a custom working directory
                try (CommandControl cc = sc.command("kill <pid>").elevated().workingDirectory("/").start()) {
                    // Discard any output but throw an exception the exit code is not 0
                    cc.discardOrThrow();
                }

                // Start a bash sub shell. Useful if the login shell is different
                try (ShellControl bash = sc.subShell("bash").start()) {
                    // ...
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
            public boolean isApplicable(ShellStore o) throws Exception {
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
