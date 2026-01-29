package io.xpipe.app.pwman;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.*;
import io.xpipe.app.secret.SecretManager;
import io.xpipe.app.secret.SecretPromptStrategy;
import io.xpipe.app.secret.SecretQueryState;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.AskpassAlert;
import io.xpipe.core.*;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.util.List;
import java.util.Random;
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
    private final String totpDuration;

    @JsonIgnore
    private boolean hasCompletedRequestInSession;

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
        var mfa = new SimpleBooleanProperty(
                p.getValue().getMfa() != null ? p.getValue().getMfa() : false);
        var duration = new SimpleStringProperty(p.getValue().getTotpDuration());
        return new OptionsBuilder()
                .nameAndDescription("keeperUseMfa")
                .addToggle(mfa)
                .name("keeperTotpDuration")
                .description(AppI18n.observable(
                        "keeperTotpDurationDescription", "login | 12_hours | 24_hours | 30_days | forever"))
                .addString(duration)
                .hide(mfa.not())
                .bind(
                        () -> {
                            return KeeperPasswordManager.builder()
                                    .mfa(mfa.get())
                                    .totpDuration(duration.get())
                                    .build();
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
                throw ErrorEventFactory.expected(
                        new IllegalArgumentException(
                                "Keeper password contains double quote \" character, which is not supported by the Keeper Commander application"));
            }

            var b = CommandBuilder.of()
                    .add(getExecutable(sc), "find-password")
                    .addLiteral(key)
                    .add("--password")
                    .addLiteral(r.getSecretValue());
            FilePath file = sc.getSystemTemporaryDirectory().join("keeper" + Math.abs(new Random().nextInt()) + ".txt");
            if (mfa != null && mfa) {
                var index = getTotpDurationIndex();
                if (hasCompletedRequestInSession && index > 0) {
                    var input = """

                          1

                          """;
                    sc.view().writeTextFile(file, input);
                } else {
                    var totp = AskpassAlert.queryRaw("Enter Keeper 2FA Code", null, true);
                    if (totp.getState() != SecretQueryState.NORMAL) {
                        return null;
                    }

                    var input = """

                                1%s
                                %s

                                """.formatted(
                                    index != -1 ? "\n" + getTotpDurationValues().get(index) : "",
                                    totp.getSecret().getSecretValue());
                    sc.view().writeTextFile(file, input);
                }
            } else {
                var input = "\n";
                sc.view().writeTextFile(file, input);
            }

            var fullB = CommandBuilder.of()
                    .add(sc.getShellDialect() == ShellDialects.CMD ? "type" : "cat")
                    .addFile(file)
                    .add("|")
                    .add(b);
            var queryCommand = sc.command(fullB);
            queryCommand.sensitive();
            queryCommand.killOnTimeout(CountDown.of().start(15_000));

            var result = queryCommand.readStdoutAndStderr();
            var exitCode = queryCommand.getExitCode();

            sc.view().deleteFileIfPossible(file);

            var out = result[0]
                    .replace("\r\n", "\n")
                    .replace("""
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
            var err = result[1].replace("\r\n", "\n").replace("""
                             EOF when reading a line
                             """, "").strip();

            var message = !err.isEmpty() ? out + "\n" + err : out;
            if (exitCode != 0) {
                // Another password prompt was made
                var wrongPw = out.contains("Enter password for") || exitCode == CommandControl.EXIT_TIMEOUT_EXIT_CODE;
                if (wrongPw) {
                    SecretManager.clearAll(KEEPER_PASSWORD_ID);
                    ErrorEventFactory.fromMessage("Master password was not accepted by Keeper. Is it correct?")
                            .expected()
                            .handle();
                    return null;
                }

                ErrorEventFactory.fromMessage(message).expected().handle();
                return null;
            }

            hasCompletedRequestInSession = true;

            var outLines = out.lines().toList();
            if (outLines.isEmpty()) {
                return null;
            }

            var lastLine = outLines.getLast();
            return new CredentialResult(null, InPlaceSecretValue.of(lastLine));
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
            return null;
        }
    }

    private List<String> getTotpDurationValues() {
        var values = List.of("login", "12_hours", "24_hours", "30_days", "forever");
        return values;
    }

    private int getTotpDurationIndex() {
        var values = getTotpDurationValues();
        var index = totpDuration != null ? values.indexOf(totpDuration) : -1;
        return index;
    }

    @Override
    public String getKeyPlaceholder() {
        return "Record UID";
    }

    @Override
    public String getWebsite() {
        return "https://www.keepersecurity.com";
    }

    @Override
    public Duration getCacheDuration() {
        return (mfa != null && mfa && getTotpDurationIndex() < 1) ? Duration.ofDays(10) : Duration.ofSeconds(3);
    }
}
