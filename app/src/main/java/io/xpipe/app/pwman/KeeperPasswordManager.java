package io.xpipe.app.pwman;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.*;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.SecretManager;
import io.xpipe.app.util.SecretRetrievalStrategy;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

@JsonTypeName("keeper")
public class KeeperPasswordManager implements PasswordManager {

    private static final UUID KEEPER_PASSWORD_ID = UUID.randomUUID();
    private static ShellControl SHELL;

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    private String getExecutable(ShellControl sc) {
        return sc.getShellDialect() == ShellDialects.CMD
                ? "@keeper"
                : (OsType.getLocal() == OsType.WINDOWS ? "keeper-commander" : "keeper");
    }

    @Override
    public synchronized CredentialResult retrieveCredentials(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("Keeper Commander CLI", "keeper");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .link("https://docs.keeper.io/en/keeperpam/commander-cli/commander-installation-setup")
                    .handle();
            return null;
        }

        try {
            var sc = getOrStartShell();
            var file = sc.view().userHome().join(".keeper", "config.json");
            if (!sc.view().fileExists(file)) {
                var script = ShellScript.lines(
                        sc.getShellDialect().getEchoCommand("Log in into your Keeper account from the CLI:", false),
                        getExecutable(sc) + " login");
                TerminalLaunch.builder()
                        .title("Keeper login")
                        .localScript(script)
                        .logIfEnabled(false)
                        .launch();
                return null;
            }

            var r = SecretManager.retrieve(
                    new SecretRetrievalStrategy.Prompt(),
                    "Enter your Keeper master password to unlock",
                    KEEPER_PASSWORD_ID,
                    0,
                    true);
            if (r == null) {
                return null;
            }

            var out = sc.command(CommandBuilder.of()
                            .add(getExecutable(sc), "get")
                            .addLiteral(key)
                            .add("--format", "json", "--unmask")
                            .add("--password")
                            .addLiteral(r.getSecretValue()))
                    .sensitive()
                    .readStdoutOrThrow();
            var tree = JacksonMapper.getDefault().readTree(out);
            var fields = tree.required("fields");
            if (!fields.isArray()) {
                return null;
            }

            String login = null;
            String password = null;
            for (JsonNode field : fields) {
                var type = field.required("type").asText();
                if (type.equals("login")) {
                    var v = field.required("value");
                    if (v.size() > 0) {
                        login = v.get(0).asText();
                    }
                }
                if (type.equals("password")) {
                    var v = field.required("value");
                    if (v.size() > 0) {
                        password = v.get(0).asText();
                    }
                }
            }

            return new CredentialResult(login, password != null ? InPlaceSecretValue.of(password) : null);
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return "Record UID";
    }

    @Override
    public String getWebsite() {
        return "https://www.keepersecurity.com";
    }
}
