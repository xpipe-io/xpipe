package io.xpipe.app.pwman;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.*;
import io.xpipe.app.secret.SecretManager;
import io.xpipe.app.secret.SecretPromptStrategy;
import io.xpipe.app.secret.SecretQueryState;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.AskpassAlert;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.core.SecretValue;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonTypeName("keeper")
@Getter
@Builder(toBuilder = true)
@ToString
@Jacksonized
public class KeeperPasswordManager implements PasswordManager {

    private static final UUID KEEPER_PASSWORD_ID = UUID.randomUUID();
    private static ShellControl SHELL;
    private final Boolean mfa;

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
                : (OsType.ofLocal() == OsType.WINDOWS ? "keeper-commander" : "keeper");
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<KeeperPasswordManager> p) {
        var mfa = new SimpleObjectProperty<>(p.getValue().getMfa());
        return new OptionsBuilder()
                .nameAndDescription("keeperUseMfa")
                .addToggle(mfa)
                .bind(
                        () -> {
                            return KeeperPasswordManager.builder().mfa(mfa.get()).build();
                        },
                        p);
    }

    @Override
    public synchronized CredentialResult retrieveCredentials(String key) {
        // The copy UID button copies the whole URL in the Keeper UI. Why? ...
        key = key.replaceFirst("https://\\w+\\.\\w+/vault/#detail/", "");

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
                        .alwaysKeepOpen(true)
                        .launch();
                return null;
            }

            var r = SecretManager.retrieve(
                    new SecretPromptStrategy(),
                    "Enter your Keeper master password to unlock",
                    KEEPER_PASSWORD_ID,
                    0,
                    true);
            if (r == null) {
                return null;
            }

            var b = CommandBuilder.of()
                    .add(getExecutable(sc), "get")
                    .addLiteral(key)
                    .add("--format", "json", "--unmask")
                    .add("--password")
                    .addLiteral(r.getSecretValue());
            CommandBuilder fullB;
            if (mfa != null && mfa) {
                var totp = AskpassAlert.queryRaw("Enter Keeper 2FA Code", null, true);
                if (totp.getState() != SecretQueryState.NORMAL) {
                    return null;
                }

                var input = """
                          
                          1
                          %s
                          """.formatted(totp.getSecret().getSecretValue());
                var escape = ShellDialects.isPowershell(sc) ? "`" : sc.getShellDialect() == ShellDialects.CMD ? "^" : "\\";
                var shellInput = input.replace("\n", escape + "\n");
                fullB = CommandBuilder.of().add("echo").addQuoted(shellInput).add("|").add(b);
            } else {
                fullB = b;
            }

            var queryCommand = sc.command(fullB);
            queryCommand.sensitive();
            if (mfa == null || !mfa) {
                queryCommand.killOnTimeout(CountDown.of().start(20_000));
            }
            var out = queryCommand.readStdoutOrThrow().replace("\r\n", "\n");
            var outStart = out.indexOf("\n{\n");
            var outPrefix = outStart == -1 ? out : out.substring(0, outStart + 1);
            var outSub = outStart == -1 ? out : out.substring(outStart + 1);

            JsonNode tree;
            try {
                tree = JacksonMapper.getDefault().readTree(outSub);
            } catch (JsonProcessingException e) {
                ErrorEventFactory.fromMessage(outPrefix).expected().handle();
                return null;
            }

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
