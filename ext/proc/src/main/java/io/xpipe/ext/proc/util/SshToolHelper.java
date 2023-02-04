package io.xpipe.ext.proc.util;

import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.core.util.SecretValue;
import io.xpipe.ext.proc.SshStore;
import io.xpipe.extension.util.ScriptHelper;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SshToolHelper {

    public static String toCommand(SshStore store, ShellProcessControl parent) {
        var list = new ArrayList<>(List.of(
                "ssh",
                store.getUser() + "@" + store.getHost(),
                "-p",
                store.getPort().toString()));
        if (store.getKey() != null) {
            list.add("-i");
            list.add(store.getKey().getFile());
        }
        return parent.getShellType().flatten(list);
    }

    @SneakyThrows
    public static String passPassword(
            String command, SecretValue userPassword, SecretValue keyPassword, ShellProcessControl parent) {
        if (userPassword == null && keyPassword == null) {
            return command;
        }

        if (userPassword != null && keyPassword != null) {
            throw new UnsupportedOperationException(
                    "Passing a user password and key passphrase is currently not supported via askpass");
        }

        var passwordToUse = userPassword != null ? userPassword : keyPassword;

        var scriptType = parent.getShellType();

        // Fix for power shell as there are permission issues when executing a powershell askpass script
        if (parent.getShellType().equals(ShellTypes.POWERSHELL)) {
            scriptType = parent.getOsType().equals(OsType.WINDOWS) ? ShellTypes.CMD : ShellTypes.BASH;
        }

        var file = ScriptHelper.createAskPassScript(passwordToUse, parent, scriptType);
        var variables = Map.of(
                "DISPLAY", "localhost:0.0",
                "SSH_ASKPASS", file,
                "SSH_ASKPASS_REQUIRE", "force");
        var variableCommand = parent.getShellType().addInlineVariablesToCommand(variables, command);
        return variableCommand;
    }
}
