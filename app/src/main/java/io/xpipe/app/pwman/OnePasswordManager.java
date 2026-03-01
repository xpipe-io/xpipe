package io.xpipe.app.pwman;

import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.ProcessOutputException;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.core.JacksonMapper;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@JsonTypeName("onePassword")
@Builder
@Jacksonized
@Getter
public class OnePasswordManager implements PasswordManager {

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.of(true, false, true, keyStrategy);
    }

    private static ShellControl SHELL;
    private static final ListProperty<String> availableAccounts = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final String account;
    private final PasswordManagerKeyStrategy keyStrategy;

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
                .hide(account.isNull().and(availableAccounts.emptyProperty()))
                .nameAndDescription("passwordManagerKeyStrategy")
                .sub(keyStrategyChoice.build(), keyStrategy)
                .bind(() -> {
                    return OnePasswordManager.builder().keyStrategy(keyStrategy.getValue()).account(account.get()).build();
                }, p);
    }

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    private List<String> listAccounts() throws Exception {
        var out = getOrStartShell().command(CommandBuilder.of().add("op", "account", "list", "--format", "json")).readStdoutOrThrow();
        var json = JacksonMapper.getDefault().readTree(out);
        if (!json.isArray()) {
            return List.of();
        }

        var emails = new ArrayList<String>();
        for (JsonNode jsonNode : json) {
            emails.add(jsonNode.required("email").textValue());
        }
        return emails;
    }

    private String getActiveAccount() throws Exception {
        if (!availableAccounts.isEmpty()) {
            if (account != null) {
                if (!availableAccounts.contains(account)) {
                    throw ErrorEventFactory.expected(new IllegalArgumentException("Account " + account + " is not registered to the 1password CLI"));
                }
                return account;
            }
            return availableAccounts.getFirst();
        }

        var accounts = listAccounts();
        availableAccounts.setAll(accounts);
        if (availableAccounts.isEmpty()) {
            throw ErrorEventFactory.expected(new IllegalStateException("No accounts are registered to the 1password CLI"));
        }
        return availableAccounts.getFirst();
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
            var account = getActiveAccount();
            var b = CommandBuilder.of()
                    .add("op", "item", "get")
                    .addLiteral(name)
                    .add("--account").addLiteral(account)
                    .add("--format", "json");
            if (vault != null) {
                b.add("--vault").addLiteral(vault);
            }

            var r = getOrStartShell().command(b).sensitive().readStdoutOrThrow();
            var tree = JacksonMapper.getDefault().readTree(r);


            var username = getValue(tree, "username");
            var password = getValue(tree, "password");
            var creds = Credentials.of(username, password);

            var fingerprint = getValue(tree, "fingerprint");
            var publicKey = getValue(tree, "public_key");
            var privateKey = getValue(tree, "private_key");
            var sshKey = SshKey.of(fingerprint, publicKey, privateKey);

            return new Result(creds, sshKey);
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
