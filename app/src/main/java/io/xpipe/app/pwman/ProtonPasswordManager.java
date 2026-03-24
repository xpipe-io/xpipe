package io.xpipe.app.pwman;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.*;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.OsType;

import javafx.beans.property.*;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@JsonTypeName("protonPass")
@Builder
@Jacksonized
@Getter
public class ProtonPasswordManager implements PasswordManager {

    @Override
    public boolean supportsKeyConfiguration() {
        return true;
    }

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.of(true, true, true, keyStrategy, getSocketLocation());
    }

    @Override
    public boolean selectInitial() throws Exception {
        return LocalShell.getShell().view().findProgram("pass-cli").isPresent();
    }

    private static ShellControl SHELL;

    private final PasswordManagerKeyStrategy keyStrategy;

    private static Path getSocketLocation() {
        var socket =
                switch (OsType.ofLocal()) {
                    case OsType.Linux ignored ->
                        AppSystemInfo.ofLinux().getUserHome().resolve(".ssh", "proton-pass-agent.sock");
                    case OsType.MacOs ignored ->
                        AppSystemInfo.ofMacOs().getUserHome().resolve(".ssh", "proton-pass-agent.sock");
                    case OsType.Windows ignored -> null;
                };
        return socket;
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<ProtonPasswordManager> p) {
        var keyStrategy = new SimpleObjectProperty<>(p.getValue().getKeyStrategy());

        var keyStrategyChoice = OptionsChoiceBuilder.builder()
                .allowNull(true)
                .available(List.of(
                        PasswordManagerKeyStrategy.Inline.class, PasswordManagerKeyStrategy.ProtonPassAgent.class))
                .property(keyStrategy)
                .build();

        return new OptionsBuilder()
                .nameAndDescription("passwordManagerTest")
                .addComp(new PasswordManagerTestComp(true))
                .nameAndDescription("passwordManagerKeyStrategy")
                .sub(keyStrategyChoice.build(), keyStrategy)
                .bind(
                        () -> {
                            return ProtonPasswordManager.builder()
                                    .keyStrategy(keyStrategy.getValue())
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

    private String runCommand(CommandBuilder b) throws Exception {
        var r = getOrStartShell().command(b).sensitive().readStdoutAndStderr();
        // pass-cli does not set exit codes on failure
        if (r[0].isEmpty() && !r[1].isEmpty()) {
            throw ErrorEventFactory.expected(ProcessOutputException.of(1, r[1]));
        }
        return r[0];
    }

    @Override
    public synchronized Result query(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("ProtonPass CLI", "pass-cli");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .expected()
                    .link("https://proton.me/pass")
                    .handle();
            return null;
        }

        try {
            var sc = getOrStartShell();
            var loggedIn =
                    sc.command(CommandBuilder.of().add("pass-cli", "info")).executeAndCheck();
            if (!loggedIn) {
                var script = ShellScript.lines("pass-cli login");
                TerminalLaunch.builder()
                        .title("Proton Pass login")
                        .localScript(script)
                        .logIfEnabled(false)
                        .preferTabs(false)
                        .pauseOnExit(true)
                        .launch();
                return null;
            }

            var split = key.split("/", 2);
            var vault = split.length > 1 ? split[0] : null;
            var itemName = split.length > 1 ? split[1] : key;

            if (vault == null) {
                var out = runCommand(CommandBuilder.of().add("pass-cli", "vault", "list", "--output", "json"));
                var json = JacksonMapper.getDefault().readTree(out);
                var vaultsNode = json.required("vaults");
                if (vaultsNode == null || vaultsNode.size() == 0) {
                    runCommand(CommandBuilder.of().add("pass-cli", "vault", "list"));
                    throw ErrorEventFactory.expected(new IllegalStateException("No Proton Pass vaults are available"));
                }

                vault = vaultsNode.get(0).required("name").textValue();

                if (vaultsNode.size() > 1) {
                    var all = new ArrayList<String>();
                    for (JsonNode vaultNode : vaultsNode) {
                        all.add(vaultNode.required("name").textValue());
                    }
                    throw ErrorEventFactory.expected(new IllegalStateException(
                            "No vault was specified but multiple are available: " + String.join(", ", all)
                                    + ". Specify the vault with <Vault Name>/<Item name>, e.g. " + vault + "/"
                                    + itemName));
                }
            }

            var b = CommandBuilder.of().add("pass-cli", "item", "view");
            if (vault != null) {
                b.add("--vault-name").addQuoted(vault);
            }
            b.add("--item-title").addQuoted(itemName);
            var out = runCommand(CommandBuilder.of().add(b).add("--output", "json"));
            var json = JacksonMapper.getDefault().readTree(out);

            var itemNode = json.get("item");
            if (itemNode == null) {
                runCommand(b);
                return null;
            }

            var contentNode = itemNode.get("content");
            if (contentNode == null) {
                return null;
            }

            var subContentNode = contentNode.get("content");
            if (subContentNode == null) {
                return null;
            }

            var login = subContentNode.get("Login");
            if (login != null) {
                var username = login.required("username").textValue();
                var password = login.required("password").textValue();
                return Result.of(Credentials.of(username, password), null);
            }

            var sshKey = subContentNode.get("SshKey");
            if (sshKey == null) {
                return null;
            }

            var privateKey = sshKey.get("private_key").textValue();
            var publicKey = sshKey.get("public_key").textValue();

            var extraFields = contentNode.get("extra_fields");
            String username = null;
            String password = null;
            if (extraFields != null) {
                for (JsonNode extraField : extraFields) {
                    var name = extraField.required("name").textValue();
                    if (name.equalsIgnoreCase("Username")) {
                        username =
                                extraField.required("content").required("Text").textValue();
                    } else if (name.equalsIgnoreCase("Password")) {
                        password =
                                extraField.required("content").required("Text").textValue();
                    }
                }
            }

            return Result.of(Credentials.of(username, password), SshKey.of(publicKey, privateKey));
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("protonPassPasswordPlaceholder");
    }

    @Override
    public String getWebsite() {
        return "https://proton.me/pass";
    }
}
