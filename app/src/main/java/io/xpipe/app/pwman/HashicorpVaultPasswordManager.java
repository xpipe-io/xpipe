package io.xpipe.app.pwman;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.xpipe.app.comp.base.SecretFieldComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.*;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.HttpHelper;
import io.xpipe.core.*;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Builder
@ToString
@Jacksonized
@JsonTypeName("hashicorpVault")
public class HashicorpVaultPasswordManager implements PasswordManager {

    @Override
    public boolean supportsKeyConfiguration() {
        return true;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public interface VaultAuth {

        static List<Class<?>> getClasses() {
            var l = new ArrayList<Class<?>>();
            l.add(Existing.class);
            l.add(Token.class);
            l.add(AppRole.class);
            return l;
        }

        String retrieveToken(HashicorpVaultPasswordManager pwman) throws Exception;

        @JsonTypeName("existing")
        @Value
        @Jacksonized
        @Builder
        class Existing implements VaultAuth {

            @SuppressWarnings("unused")
            public static String getOptionsNameKey() {
                return "hashicorpVaultAuthExisting";
            }

            @Override
            public String retrieveToken(HashicorpVaultPasswordManager pwman) throws Exception {
                var sc = getOrStartShell();
                var script = ShellScript.lines(
                        sc.getShellDialect().getSetEnvironmentVariableCommand("VAULT_ADDR", pwman.getVaultAddress()),
                        pwman.getVaultNamespace() != null
                                ? sc.getShellDialect()
                                        .getSetEnvironmentVariableCommand("VAULT_NAMESPACE", pwman.getVaultNamespace())
                                : null,
                        sc.getShellDialect()
                                .getEchoCommand(
                                        "Your current vault login is expired. Please log in again with your currently selected auth method. The proper environment variables for your vault have already been configured in this session. The command syntax for this is:",
                                        false),
                        sc.getShellDialect().getEchoCommand("", false),
                        sc.getShellDialect()
                                .getEchoCommand(
                                        "vault login --method=<auth_method> [optional auth method specific parameters]",
                                        false));
                var scriptFile = ScriptHelper.createExecScript(sc, script.toString());
                TerminalLaunch.builder()
                        .localScript(ShellScript.of(
                                sc.getShellDialect().terminalInitCommand(sc, scriptFile.toString(), false)))
                        .title("Vault login")
                        .pauseOnExit(false)
                        .logIfEnabled(false)
                        .preferTabs(false)
                        .launch();
                return null;
            }
        }

        @JsonTypeName("token")
        @Value
        @Jacksonized
        @Builder
        class Token implements VaultAuth {

            @SuppressWarnings("unused")
            public static String getOptionsNameKey() {
                return "hashicorpVaultAuthToken";
            }

            @SuppressWarnings("unused")
            public static OptionsBuilder createOptions(Property<Token> p) {
                var token = new SimpleObjectProperty<>(p.getValue().getToken());
                return new OptionsBuilder()
                        .nameAndDescription("hashicorpVaultToken")
                        .addComp(new SecretFieldComp(token, true).maxWidth(600), token)
                        .nonNull()
                        .bind(
                                () -> {
                                    return Token.builder().token(token.get()).build();
                                },
                                p);
            }

            InPlaceSecretValue token;

            @Override
            public String retrieveToken(HashicorpVaultPasswordManager pwman) {
                if (token == null) {
                    return null;
                }

                return token.getSecretValue();
            }
        }

        @JsonTypeName("appRole")
        @Value
        @Jacksonized
        @Builder
        class AppRole implements VaultAuth {

            @SuppressWarnings("unused")
            public static String getOptionsNameKey() {
                return "hashicorpVaultAuthAppRole";
            }

            @SuppressWarnings("unused")
            public static OptionsBuilder createOptions(Property<AppRole> p) {
                var roleId = new SimpleStringProperty(p.getValue().getRoleId());
                var secretId = new SimpleObjectProperty<>(p.getValue().getSecretId());
                return new OptionsBuilder()
                        .nameAndDescription("hashicorpVaultRoleId")
                        .addString(roleId)
                        .nonNull()
                        .nameAndDescription("hashicorpVaultSecretId")
                        .addComp(new SecretFieldComp(secretId, true).maxWidth(600), secretId)
                        .nonNull()
                        .bind(
                                () -> {
                                    return AppRole.builder()
                                            .roleId(roleId.get())
                                            .secretId(secretId.get())
                                            .build();
                                },
                                p);
            }

            String roleId;
            InPlaceSecretValue secretId;

            @Override
            public String retrieveToken(HashicorpVaultPasswordManager pwman) throws Exception {
                if (roleId == null || secretId == null) {
                    return null;
                }

                var json = JsonNodeFactory.instance.objectNode();
                json.put("role_id", roleId);
                json.put("secret_id", secretId.getSecretValue());
                var req = HttpRequest.newBuilder().uri(URI.create(pwman.getVaultAddress() + "/v1/auth/approle/login"));
                req.POST(HttpRequest.BodyPublishers.ofString(json.toPrettyString()));
                if (pwman.getVaultNamespace() != null) {
                    req.header("X-Vault-Namespace", pwman.getVaultNamespace());
                }

                var res = HttpHelper.client().send(req.build(), HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() >= 400) {
                    throw new IOException(res.body());
                }

                var resJson = JacksonMapper.getDefault().readTree(res.body());
                if (!resJson.isObject()) {
                    throw new IOException(res.body());
                }

                var auth = resJson.get("auth");
                if (auth == null || auth.get("client_token") == null) {
                    throw new IOException(res.body());
                }

                return auth.required("client_token").textValue();
            }
        }
    }

    private static ShellControl SHELL;

    private final String vaultAddress;
    private final String vaultNamespace;
    private final VaultAuth vaultAuth;

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.of(true, true, false, new PasswordManagerKeyStrategy.Inline(), null);
    }

    @Override
    public boolean selectInitial() throws Exception {
        return LocalShell.getShell().view().findProgram("vault").isPresent();
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<HashicorpVaultPasswordManager> p) {
        var vaultAddress = new SimpleStringProperty(p.getValue().getVaultAddress());
        var vaultNamespace = new SimpleStringProperty(p.getValue().getVaultNamespace());
        var vaultAuth = new SimpleObjectProperty<>(
                p.getValue().getVaultAuth() != null ? p.getValue().getVaultAuth() : new VaultAuth.Existing());

        return new OptionsBuilder()
                .nameAndDescription("hashicorpVaultAddress")
                .addComp(
                        new TextFieldComp(vaultAddress)
                                .apply(struc -> {
                                    struc.setPromptText("https://my.vault.example.com:8200");
                                })
                                .maxWidth(600),
                        vaultAddress)
                .nonNull()
                .nameAndDescription("hashicorpVaultNamespace")
                .addString(vaultNamespace)
                .nameAndDescription("hashicorpVaultAuthType")
                .sub(
                        OptionsChoiceBuilder.builder()
                                .available(VaultAuth.getClasses())
                                .property(vaultAuth)
                                .build()
                                .build(),
                        vaultAuth)
                .nonNull()
                .nameAndDescription("passwordManagerTest")
                .addComp(new PasswordManagerTestComp(true))
                .bind(
                        () -> {
                            return HashicorpVaultPasswordManager.builder()
                                    .vaultAddress(vaultAddress.get())
                                    .vaultNamespace(vaultNamespace.get())
                                    .vaultAuth(vaultAuth.get())
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

    private boolean isLoginValid() throws Exception {
        var sc = getOrStartShell();
        var b = CommandBuilder.of().add("vault", "token", "lookup", "-non-interactive", "--format=json");
        if (getVaultNamespace() != null) {
            b.fixedEnvironment("VAULT_NAMESPACE", getVaultNamespace());
        }
        b.fixedEnvironment("VAULT_ADDR", getVaultAddress());
        var valid = sc.command(b).sensitive().executeAndCheck();
        return valid;
    }

    private boolean login() throws Exception {
        if (isLoginValid()) {
            return true;
        }

        var token = vaultAuth.retrieveToken(HashicorpVaultPasswordManager.this);
        if (token == null) {
            return false;
        }

        var b = CommandBuilder.of().add("vault", "login", "-non-interactive");
        if (vaultNamespace != null) {
            b.fixedEnvironment("VAULT_NAMESPACE", vaultNamespace);
        }
        b.addLiteral(token);
        b.fixedEnvironment("VAULT_ADDR", vaultAddress);
        getOrStartShell().command(b).sensitive().execute();
        return true;
    }

    @Override
    public synchronized Result query(String key) {
        if (vaultAddress == null || vaultAuth == null) {
            return null;
        }

        try {
            CommandSupport.isInLocalPathOrThrow("Hashicorp Vault CLI", "vault");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .expected()
                    .link("https://developer.hashicorp.com/vault/docs/commands")
                    .handle();
            return null;
        }

        try {
            if (!login()) {
                return null;
            }

            var keySplit = key.split("\\?", 2);
            if (keySplit.length != 2 || keySplit[0].isEmpty() || keySplit[1].isEmpty()) {
                throw ErrorEventFactory.expected(new IllegalArgumentException("Invalid secret reference format"));
            }

            var secretPath = keySplit[0];
            var keys = Arrays.stream(keySplit[1].split("&"))
                    .filter(s -> s.split("=").length == 2)
                    .collect(Collectors.toMap(s -> s.split("=", 2)[0], s -> s.split("=", 2)[1]));
            if (keys.isEmpty()) {
                throw ErrorEventFactory.expected(new IllegalArgumentException("Invalid secret reference format"));
            }

            var b = CommandBuilder.of().add("vault", "read", "--format=json", "-non-interactive");
            if (vaultNamespace != null) {
                b.fixedEnvironment("VAULT_NAMESPACE", vaultNamespace);
            }
            b.addLiteral(secretPath);
            b.fixedEnvironment("VAULT_ADDR", vaultAddress);

            var out = getOrStartShell().command(b).sensitive().readStdoutOrThrow();
            var json = JacksonMapper.getDefault().readTree(out);
            var data = json.get("data");
            if (data == null) {
                return null;
            }

            var subData = data.get("data");
            if (subData == null) {
                return null;
            }

            var username = Optional.ofNullable(subData.get(keys.get("user")))
                    .map(JsonNode::textValue)
                    .orElse(null);
            var password = Optional.ofNullable(subData.get(keys.get("pass")))
                    .map(JsonNode::textValue)
                    .orElse(null);
            var publicKey = Optional.ofNullable(subData.get(keys.get("public-key")))
                    .map(JsonNode::textValue)
                    .orElse(null);
            var privateKey = Optional.ofNullable(subData.get(keys.get("private-key")))
                    .map(JsonNode::textValue)
                    .orElse(null);
            var creds = Credentials.of(username, password);
            var sshKey = SshKey.of(publicKey, privateKey);
            var r = Result.of(creds, sshKey);
            if (r == null) {
                if (subData.size() == 0) {
                    throw ErrorEventFactory.expected(new IllegalArgumentException("Found data at path but no fields"));
                }

                var l = new ArrayList<String>();
                subData.fieldNames().forEachRemaining(l::add);
                throw ErrorEventFactory.expected(new IllegalArgumentException("Found data for specified key mapping, but only found the following unmapped keys: " + l));
            }
            return r;
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("hashicorpVaultPlaceholder");
    }

    @Override
    public String getWebsite() {
        return "https://www.hashicorp.com/en/products/vault";
    }
}
