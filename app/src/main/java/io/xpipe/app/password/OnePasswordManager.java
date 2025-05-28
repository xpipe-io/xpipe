package io.xpipe.app.password;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.JacksonMapper;

@JsonTypeName("onePassword")
public class OnePasswordManager implements PasswordManager {

    private static ShellControl SHELL;

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    @Override
    public synchronized CredentialResult retrieveCredentials(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("1Password CLI", "op");
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e)
                    .expected()
                    .link("https://developer.1password.com/docs/cli/get-started/")
                    .handle();
            return null;
        }

        try {
            var r = getOrStartShell()
                    .command(CommandBuilder.of()
                            .add("op", "item", "get")
                            .addLiteral(key)
                            .add("--format", "json", "--fields", "username,password"))
                    .readStdoutOrThrow();
            var tree = JacksonMapper.getDefault().readTree(r);
            if (!tree.isArray() || tree.size() != 2) {
                return null;
            }

            var username = tree.get(0).get("value");
            var password = tree.get(1).get("value");
            return new CredentialResult(username != null ? username.asText() : null, password != null ? InPlaceSecretValue.of(password.asText()) : null);
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("onePasswordPlaceholder");
    }
}
