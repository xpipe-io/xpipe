package io.xpipe.app.pwman;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.*;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.OsType;

import javafx.beans.property.*;
import javafx.collections.FXCollections;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

@JsonTypeName("onePassword")
@Builder
@Jacksonized
@Getter
public class OnePasswordManager implements PasswordManager {

    @Override
    public boolean supportsKeyConfiguration() {
        return true;
    }

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.of(true, false, true, keyStrategy, getSocketLocation());
    }

    @Override
    public boolean selectInitial() throws Exception {
        return LocalShell.getShell().view().findProgram("op").isPresent();
    }

    private static ShellControl SHELL;
    private static final MapProperty<String, String> availableAccounts =
            new SimpleMapProperty<>(FXCollections.observableMap(new LinkedHashMap<>()));

    private final String account;
    private final PasswordManagerKeyStrategy keyStrategy;

    private static Path getSocketLocation() {
        var socket =
                switch (OsType.ofLocal()) {
                    case OsType.Linux ignored ->
                        AppSystemInfo.ofLinux().getUserHome().resolve(".1password", "agent.sock");
                    case OsType.MacOs ignored ->
                        AppSystemInfo.ofMacOs()
                                .getUserHome()
                                .resolve("Library", "Group Containers", "2BUA8C4S2C.com.1password", "t", "agent.sock");
                    case OsType.Windows ignored -> null;
                };
        return socket;
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<OnePasswordManager> p) {
        var account = new SimpleStringProperty(p.getValue().getAccount());
        var keyStrategy = new SimpleObjectProperty<>(p.getValue().getKeyStrategy());

        var keyStrategyChoice = OptionsChoiceBuilder.builder()
                .allowNull(true)
                .available(List.of(PasswordManagerKeyStrategy.Agent.class))
                .property(keyStrategy)
                .build();

        return new OptionsBuilder()
                .nameAndDescription("onePasswordManagerAccount")
                .addString(account)
                .hide(account.isNull().and(availableAccounts.sizeProperty().lessThan(2)))
                .nameAndDescription("passwordManagerTest")
                .addComp(new PasswordManagerTestComp(true))
                .nameAndDescription("passwordManagerKeyStrategy")
                .sub(keyStrategyChoice.build(), keyStrategy)
                .bind(
                        () -> {
                            return OnePasswordManager.builder()
                                    .keyStrategy(keyStrategy.getValue())
                                    .account(account.get())
                                    .build();
                        },
                        p);
    }

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    private SequencedMap<String, String> listAccounts() throws Exception {
        var out = getOrStartShell()
                .command(CommandBuilder.of().add("op", "account", "list", "--format", "json"))
                .sensitive()
                .readStdoutOrThrow();
        var json = JacksonMapper.getDefault().readTree(out);
        if (!json.isArray()) {
            return new LinkedHashMap<>();
        }

        var emails = new LinkedHashMap<String, String>();
        for (JsonNode jsonNode : json) {
            emails.put(
                    jsonNode.required("email").textValue(),
                    jsonNode.required("user_uuid").textValue());
        }
        return emails;
    }

    private String getActiveAccount() throws Exception {
        if (availableAccounts.isEmpty()) {
            var accounts = listAccounts();
            // Running commands instantly after each other breaks 1password
            ThreadHelper.sleep(1500);
            availableAccounts.clear();
            availableAccounts.putAll(accounts);
        }

        if (availableAccounts.isEmpty()) {
            return null;
        }

        if (account != null) {
            if (availableAccounts.get(account) == null) {
                throw ErrorEventFactory.expected(
                        new IllegalArgumentException("Account " + account + " is not registered to the 1password CLI"));
            }
            return availableAccounts.get(account);
        }

        var first = availableAccounts.entrySet().iterator().next().getValue();
        return first;
    }

    private String getValue(JsonNode node, String name) {
        var fields = node.get("fields");
        if (fields == null || !fields.isArray()) {
            return null;
        }

        for (JsonNode field : fields) {
            var id = field.get("id");
            if (id != null && id.textValue().equals(name)) {
                var value = field.get("value");
                return value != null ? value.textValue() : null;
            }
        }

        return null;
    }

    @Override
    public synchronized Result query(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("1Password CLI", "op");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .expected()
                    .link("https://developer.1password.com/docs/cli/get-started/")
                    .handle();
            return null;
        }

        String vault = null;
        String name = key;

        if (key.startsWith("op://")) {
            var match = Pattern.compile("op://([^/]+)/([^/]+)").matcher(key);
            if (match.find()) {
                vault = match.group(1);
                name = match.group(2);
            }
        }

        try {
            var b = CommandBuilder.of().add("op", "item", "get").addLiteral(name);
            var account = getActiveAccount();
            if (account != null) {
                b.add("--account").addLiteral(account);
            }
            b.add("--format", "json");
            if (vault != null) {
                b.add("--vault").addLiteral(vault);
            }

            var r = getOrStartShell().command(b).sensitive().readStdoutOrThrow();
            var tree = JacksonMapper.getDefault().readTree(r);

            var username = getValue(tree, "username");
            var password = getValue(tree, "password");
            var creds = Credentials.of(username, password);

            var publicKey = getValue(tree, "public_key");
            var privateKey = getValue(tree, "private_key");
            var sshKey = SshKey.of(publicKey, privateKey);

            return Result.of(creds, sshKey);
        } catch (Exception e) {
            var event = ErrorEventFactory.fromThrowable(e);
            if (!key.startsWith("op://")
                    && e instanceof ProcessOutputException pex
                    && pex.getOutput().contains("Specify the item")) {
                event.documentationLink(DocumentationLink.ONE_PASSWORD_KEYS).expected();
            }
            event.handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("onePasswordPlaceholder");
    }

    @Override
    public String getWebsite() {
        return "https://1password.com/";
    }
}
