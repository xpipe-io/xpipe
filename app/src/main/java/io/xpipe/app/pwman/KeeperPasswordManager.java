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
import io.xpipe.core.*;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
        return OsType.ofLocal() == OsType.WINDOWS ? "keeper-commander" : "keeper";
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
            CommandSupport.isInLocalPathOrThrow("Keeper Commander CLI", "keeper-commander");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .link("https://docs.keeper.io/en/keeperpam/commander-cli/commander-installation-setup")
                    .handle();
            return null;
        }

        try {
            var sc = getOrStartShell();
            var config = sc.view().userHome().join(".keeper", "config.json");
            if (!sc.view().fileExists(config)) {
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

            if (r.getSecretValue().contains("\"")) {
                SecretManager.clearAll(KEEPER_PASSWORD_ID);
                throw ErrorEventFactory.expected(new IllegalArgumentException("Keeper password contains double quote \" character, which is not supported by the Keeper Commander application"));
            }

            var b = CommandBuilder.of()
                    .add(getExecutable(sc), "get")
                    .addLiteral(key)
                    .add("--format", "json", "--unmask")
                    .add("--password")
                    .addLiteral(r.getSecretValue());
            FilePath file = null;
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
                file = sc.getSystemTemporaryDirectory().join("keeper.txt");
                sc.view().writeTextFile(file, input);
                fullB = CommandBuilder.of().add(sc.getShellDialect() == ShellDialects.CMD ? "type" : "cat").addFile(file).add("|").add(b);
            } else {
                fullB = b;
            }

            var queryCommand = sc.command(fullB);
            queryCommand.sensitive();
            queryCommand.killOnTimeout(CountDown.of().start(15_000));

            var result = queryCommand.readStdoutAndStderr();
            var exitCode = queryCommand.getExitCode();

            if (file != null) {
                sc.view().deleteFileIfPossible(file);
            }

            var out = result[0].replace("\r\n", "\n").replace("""
                      Selection: Invalid entry, additional factors of authentication shown may be configured if not currently enabled.
                      Selection:\s
                      2FA Code Duration: Require Every Login.
                      To change duration: 2fa_duration=login|12_hours|24_hours|30_days|forever
                      """, "")
                    .replace("""
                             This account requires 2FA Authentication
                             
                               1. TOTP (Google and Microsoft Authenticator) \s
                               q. Quit login attempt and return to Commander prompt
                             """, "")
                    .replace("Selection:", "")
                    .strip();
            var err = result[1].replace("\r\n", "\n")
                    .replace("""
                             EOF when reading a line
                             """, "")
                    .strip();

            var jsonStart = out.indexOf("{\n");
            var jsonEnd = out.indexOf("\n}");
            if (jsonEnd != -1) {
                jsonEnd += 2;
            }

            var outPrefix = jsonStart <= 0 ? out : out.substring(0, jsonStart);
            var outJson = jsonStart <= 0 ? (jsonEnd != -1 ? out.substring(0, jsonEnd) : out) :
                    (jsonEnd != -1 ? out.substring(jsonStart, jsonEnd) : out.substring(jsonStart));

            if (exitCode != 0) {
                var wrongPw = outPrefix.contains("Enter password for");
                if (wrongPw) {
                    SecretManager.clearAll(KEEPER_PASSWORD_ID);
                    ErrorEventFactory.fromMessage("Master password was not accepted by Keeper. Is it correct?").expected().handle();
                    return null;
                }

                var message = !err.isEmpty() ? outPrefix + "\n" + err : outPrefix;
                ErrorEventFactory.fromMessage(message).expected().handle();
                return null;
            }

            JsonNode tree;
            try {
                tree = JacksonMapper.getDefault().readTree(outJson);
            } catch (JsonProcessingException e) {
                var message = !err.isEmpty() ? outPrefix + "\n" + err : outPrefix;
                ErrorEventFactory.fromMessage(message).expected().handle();
                return null;
            }

            var fields = tree.get("fields");
            // There multiple schemas
            if (fields == null || !fields.isArray()) {
                String login = null;
                String password = null;

                var l = tree.get("login");
                if (l != null && l.isTextual()) {
                    login = l.asText();
                }

                var p = tree.get("password");
                if (p != null && p.isTextual()) {
                    password = p.asText();
                }

                if (login == null && password == null) {
                    var message = !err.isEmpty() ? out + "\n" + err : out;
                    ErrorEventFactory.fromMessage(message).description("Received invalid response").expected().handle();
                    return null;
                }

                return new CredentialResult(login, password != null ? InPlaceSecretValue.of(password) : null);
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
