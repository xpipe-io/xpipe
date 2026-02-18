package io.xpipe.app.pwman;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.process.*;
import io.xpipe.app.secret.SecretManager;
import io.xpipe.app.secret.SecretPromptStrategy;
import io.xpipe.app.secret.SecretQueryState;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.AskpassAlert;
import io.xpipe.core.*;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JsonTypeName("keeper")
@Getter
@Builder(toBuilder = true)
@ToString
@Jacksonized
public class KeeperPasswordManager implements PasswordManager {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public interface KeeperAuth {

        static List<Class<?>> getClasses() {
            var l = new ArrayList<Class<?>>();
            l.add(None.class);
            l.add(Sms.class);
            l.add(AuthenticatorApp.class);
            l.add(SecurityKey.class);
            l.add(Other.class);
            return l;
        }


        default List<String> getTotpDurationValues() {
            var values = List.of("login", "12_hours", "24_hours", "30_days", "forever");
            return values;
        }

        String constructKeeperInput(KeeperPasswordManager passwordManager) throws Exception;

        Duration getCacheDuration();

        Duration getCommandTimeout();

        String cleanMessage(String output);

        @JsonTypeName("sms")
        @Value
        @Jacksonized
        @Builder
        class Sms implements KeeperAuth {

            @SuppressWarnings("unused")
            public static OptionsBuilder createOptions(Property<Sms> p) {
                var duration = new SimpleStringProperty(p.getValue().getTotpDuration());
                return new OptionsBuilder()
                        .name("keeperTotpDuration")
                        .description(AppI18n.observable(
                                "keeperTotpDurationDescription", "login | 12_hours | 24_hours | 30_days | forever"))
                        .addString(duration)
                        .bind(
                                () -> {
                                    return Sms.builder()
                                            .totpDuration(duration.get())
                                            .build();
                                },
                                p);
            }

            String totpDuration;

            private int getTotpDurationIndex() {
                var values = getTotpDurationValues();
                var index = totpDuration != null ? values.indexOf(totpDuration) : -1;
                return index;
            }

            private void sendInitialSms() throws Exception {
                var sc = getOrStartShell();
                var b = CommandBuilder.of()
                        .add(getExecutable(), "get")
                        .addLiteral("test");
                var file = sc.getSystemTemporaryDirectory().join("keeper" + Math.abs(new Random().nextInt()) + ".txt");
                var input = """
                            
                            1
                            -
                            q
                            """;
                sc.view().writeTextFile(file, input);

                var fullCommand = CommandBuilder.of()
                        .add(sc.getShellDialect() == ShellDialects.CMD ? "type" : "cat")
                        .addFile(file)
                        .add("|")
                        .add(b);
                sc.command(fullCommand).sensitive().execute();
            }

            @Override
            public String constructKeeperInput(KeeperPasswordManager passwordManager) throws Exception {
                sendInitialSms();

                var index = getTotpDurationIndex();
                if (passwordManager.isHasCompletedRequestInSession() && index > 0) {
                    var input = """

                          1

                          """;
                    return input;
                } else {
                    var totp = AskpassAlert.queryRaw("Enter Keeper Commander SMS Code", null, true);
                    if (totp.getState() != SecretQueryState.NORMAL) {
                        return null;
                    }

                    var input = """

                                1%s
                                %s

                                """.formatted(
                            index != -1 ? "\n" + getTotpDurationValues().get(index) : "",
                            totp.getSecret().getSecretValue());
                    return input;
                }
            }

            @Override
            public Duration getCacheDuration() {
                return getTotpDurationIndex() < 1 ? Duration.ofDays(1) : Duration.ofSeconds(30);
            }

            @Override
            public Duration getCommandTimeout() {
                return Duration.ofSeconds(25);
            }

            @Override
            public String cleanMessage(String output) {
                return output
                        .replaceFirst("""
                             Select your 2FA method:
                               1. Send SMS Code.+
                               q. Cancel login
                             """, "")
                        .replace(" Invalid entry, additional factors of authentication shown may be configured if not currently enabled.", "")
                        .replace("""
                                2FA Code Duration: Require Every Login.
                                To change duration: 2fa_duration=login|12_hours|24_hours|30_days|forever
                                """, "");
            }
        }

        @JsonTypeName("authenticatorApp")
        @Value
        @Jacksonized
        @Builder
        class AuthenticatorApp implements KeeperAuth {

            @SuppressWarnings("unused")
            public static OptionsBuilder createOptions(Property<AuthenticatorApp> p) {
                var duration = new SimpleStringProperty(p.getValue().getTotpDuration());
                return new OptionsBuilder()
                        .name("keeperTotpDuration")
                        .description(AppI18n.observable(
                                "keeperTotpDurationDescription", "login | 12_hours | 24_hours | 30_days | forever"))
                        .addString(duration)
                        .bind(
                                () -> {
                                    return AuthenticatorApp.builder()
                                            .totpDuration(duration.get())
                                            .build();
                                },
                                p);
            }

            String totpDuration;

            private int getTotpDurationIndex() {
                var values = getTotpDurationValues();
                var index = totpDuration != null ? values.indexOf(totpDuration) : -1;
                return index;
            }

            @Override
            public String constructKeeperInput(KeeperPasswordManager passwordManager) {
                var index = getTotpDurationIndex();
                if (passwordManager.isHasCompletedRequestInSession() && index > 0) {
                    var input = """

                          1

                          """;
                    return input;
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
                    return input;
                }
            }

            @Override
            public Duration getCacheDuration() {
                return getTotpDurationIndex() < 1 ? Duration.ofDays(1) : Duration.ofSeconds(30);
            }

            @Override
            public Duration getCommandTimeout() {
                return Duration.ofSeconds(25);
            }

            @Override
            public String cleanMessage(String output) {
                return output.replace("""
                             Select your 2FA method:
                               1. TOTP (Google and Microsoft Authenticator) \s
                               q. Cancel login
                             """, "")
                        .replace(
                        """
                        Selection: Invalid entry, additional factors of authentication shown may be configured if not currently enabled.
                        Selection:\s
                        2FA Code Duration: Require Every Login.
                        To change duration: 2fa_duration=login|12_hours|24_hours|30_days|forever
                        """, "")
                        .replace(
                        """
                        This account requires 2FA Authentication

                          1. TOTP (Google and Microsoft Authenticator) \s
                          q. Quit login attempt and return to Commander prompt
                        """, "");
            }

        }

        @JsonTypeName("securityKey")
        @Value
        @Jacksonized
        @Builder
        class SecurityKey implements KeeperAuth {

            @Override
            public String constructKeeperInput(KeeperPasswordManager passwordManager) {
                var input = """

                          1

                          """;
                return input;
            }

            @Override
            public Duration getCacheDuration() {
                return Duration.ofDays(1);
            }

            @Override
            public Duration getCommandTimeout() {
                return null;
            }

            @Override
            public String cleanMessage(String output) {
                return output.replace("""
                               Select your 2FA method:
                                 1. WebAuthN (FIDO2 Security Key) \s
                                 q. Cancel login
                               """, "")
                        .replace(" Invalid entry, additional factors of authentication shown may be configured if not currently enabled.", "");
            }
        }


        @JsonTypeName("other")
        @Value
        @Jacksonized
        @Builder
        class Other implements KeeperAuth {

            @SuppressWarnings("unused")
            public static String getOptionsNameKey() {
                return "keeperOtherAuth";
            }

            @Override
            public Duration getCommandTimeout() {
                return null;
            }

            @Override
            public String cleanMessage(String output) {
                return output;
            }

            @Override
            public String constructKeeperInput(KeeperPasswordManager passwordManager) {
                var input = """

                          1

                          """;
                return input;
            }

            @Override
            public Duration getCacheDuration() {
                return Duration.ofDays(1);
            }
        }

        @JsonTypeName("none")
        @Value
        @Jacksonized
        @Builder
        class None implements KeeperAuth {

            @Override
            public Duration getCommandTimeout() {
                return Duration.ofSeconds(25);
            }

            @Override
            public String cleanMessage(String output) {
                return output;
            }

            @Override
            public String constructKeeperInput(KeeperPasswordManager passwordManager) {
                var input = """

                          1

                          """;
                return input;
            }

            @Override
            public Duration getCacheDuration() {
                return Duration.ofSeconds(30);
            }
        }
    }

    private static final UUID KEEPER_PASSWORD_ID = UUID.randomUUID();
    private static ShellControl SHELL;
    private final KeeperAuth twoFactorAuth;
    @JsonIgnore
    private boolean hasCompletedRequestInSession;

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    private static String getExecutable() {
        return OsType.ofLocal() == OsType.WINDOWS ? "keeper-commander" : "keeper";
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<KeeperPasswordManager> p) {
        var mfa = new SimpleObjectProperty<>(p.getValue().getTwoFactorAuth() != null ? p.getValue().getTwoFactorAuth() : new KeeperAuth.None());

        var choice = OptionsChoiceBuilder.builder()
                .allowNull(false)
                .available(KeeperAuth.getClasses())
                .property(mfa)
                .build();

        return new OptionsBuilder()
                .nameAndDescription("keeper2fa")
                .sub(choice.build(), mfa)
                .bind(
                        () -> {
                            return KeeperPasswordManager.builder()
                                    .twoFactorAuth(mfa.get())
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
                        getExecutable() + " login");
                TerminalLaunch.builder()
                        .title("Keeper login")
                        .localScript(script)
                        .logIfEnabled(false)
                        .pauseOnExit(true)
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
                    .add(getExecutable(), "get")
                    .addLiteral(key)
                    .add("--format", "json", "--unmask")
                    .add("--password")
                    .addLiteral(r.getSecretValue());
            FilePath file = sc.getSystemTemporaryDirectory().join("keeper" + Math.abs(new Random().nextInt()) + ".txt");

            var effectiveTwoFactor = twoFactorAuth != null ? twoFactorAuth : new KeeperAuth.None();
            var input = effectiveTwoFactor.constructKeeperInput(this);
            if (input == null) {
                return null;
            }
            sc.view().writeTextFile(file, input);

            var fullB = CommandBuilder.of()
                    .add(sc.getShellDialect() == ShellDialects.CMD ? "type" : "cat")
                    .addFile(file)
                    .add("|")
                    .add(b);
            var queryCommand = sc.command(fullB);
            queryCommand.sensitive();

            if (effectiveTwoFactor.getCommandTimeout() != null) {
                var timeout = effectiveTwoFactor.getCommandTimeout().toMillis();
                queryCommand.killOnTimeout(CountDown.of().start(timeout));
            }

            var result = queryCommand.readStdoutAndStderr();
            var exitCode = queryCommand.getExitCode();

            sc.view().deleteFileIfPossible(file);

            var out = result[0]
                    .replace("\r\n", "\n");
            out = effectiveTwoFactor.cleanMessage(out);
            out = out.replace("Selection:", "")
                    .strip();

            var err = result[1]
                    .replace("\r\n", "\n")
                    .replace("EOF when reading a line", "")
                    .strip();

            var jsonStart = out.indexOf("{\n");
            var jsonEnd = out.indexOf("\n}");
            if (jsonEnd != -1) {
                jsonEnd += 2;
            }

            var outPrefix = jsonStart <= 0 ? out : out.substring(0, jsonStart);
            outPrefix = outPrefix.lines().filter(s -> !s.isBlank()).map(s -> s.strip()).collect(Collectors.joining("\n"));

            var outJson = jsonStart <= 0
                    ? (jsonEnd != -1 ? out.substring(0, jsonEnd) : out)
                    : (jsonEnd != -1 ? out.substring(jsonStart, jsonEnd) : out.substring(jsonStart));

            if (exitCode != 0) {
                // Another password prompt was made
                var wrongPw =
                        (outPrefix.contains("Enter password for") || exitCode == CommandControl.EXIT_TIMEOUT_EXIT_CODE)
                                && !hasCompletedRequestInSession;
                if (wrongPw) {
                    SecretManager.clearAll(KEEPER_PASSWORD_ID);
                    ErrorEventFactory.fromMessage("Master password was not accepted by Keeper. Is it correct?")
                            .expected()
                            .handle();
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

            hasCompletedRequestInSession = true;

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
                    ErrorEventFactory.fromMessage(message)
                            .description("Received invalid response")
                            .expected()
                            .handle();
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

    @Override
    public Duration getCacheDuration() {
        var effectiveTwoFactor = twoFactorAuth != null ? twoFactorAuth : new KeeperAuth.None();
        return effectiveTwoFactor.getCacheDuration();
    }
}
