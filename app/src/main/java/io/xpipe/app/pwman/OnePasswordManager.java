package io.xpipe.app.pwman;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.JacksonMapper;

import com.fasterxml.jackson.annotation.JsonTypeName;

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
            ErrorEventFactory.fromThrowable(e)
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
                    .sensitive()
                    .readStdoutOrThrow();
            var tree = JacksonMapper.getDefault().readTree(r);
            if (!tree.isArray() || tree.size() != 2) {
                return null;
            }

            var username = tree.get(0).get("value");
            var password = tree.get(1).get("value");
            return new CredentialResult(
                    username != null ? username.asText() : null,
                    password != null ? InPlaceSecretValue.of(password.asText()) : null);
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("onePasswordPlaceholder");
    }
}
