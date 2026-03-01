package io.xpipe.app.pwman;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.xpipe.app.comp.base.SecretFieldComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.secret.SecretQueryState;
import io.xpipe.app.util.AskpassAlert;
import io.xpipe.app.util.HttpHelper;
import io.xpipe.core.*;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Getter
@Builder
@ToString
@Jacksonized
@JsonTypeName("hashicorpVault")
public class HashicorpVaultPasswordManager implements PasswordManager {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public interface VaultAuth {

        static List<Class<?>> getClasses() {
            var l = new ArrayList<Class<?>>();
            l.add(Token.class);
            l.add(AppRole.class);
            return l;
        }

        String retrieveToken(HashicorpVaultPasswordManager pwman) throws Exception;

        @JsonTypeName("token")
        @Value
        @Jacksonized
        @Builder
        class Token implements VaultAuth {

            @SuppressWarnings("unused")
            public static OptionsBuilder createOptions(Property<Token> p) {
                var token = new SimpleObjectProperty<>(p.getValue().getToken());
                return new OptionsBuilder().name("keeperTotpDuration")
                        .nameAndDescription("hashicorpVaultSecretId")
                        .addComp(new SecretFieldComp(token, false).maxWidth(600), token)
                        .nonNull()
                        .bind(() -> {
                            return Token.builder().token(token.get()).build();
                        }, p);
            }

            InPlaceSecretValue token;

            @Override
            public String retrieveToken(HashicorpVaultPasswordManager pwman) throws Exception {
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
            public static OptionsBuilder createOptions(Property<AppRole> p) {
                var roleId = new SimpleStringProperty(p.getValue().getRoleId());
                var secretId = new SimpleObjectProperty<>(p.getValue().getSecretId());
                return new OptionsBuilder()
                        .nameAndDescription("hashicorpVaultRoleId")
                        .addString(roleId)
                        .nonNull()
                        .nameAndDescription("hashicorpVaultSecretId")
                        .addComp(new SecretFieldComp(secretId, false).maxWidth(600), secretId)
                        .nonNull()
                        .bind(
                                () -> {
                                    return AppRole.builder().roleId(roleId.get()).secretId(secretId.get()).build();
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
        return PasswordManagerKeyConfiguration.none();
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<HashicorpVaultPasswordManager> p) {
        var vaultAddress = new SimpleStringProperty(p.getValue().getVaultAddress());
        var vaultNamespace = new SimpleStringProperty(p.getValue().getVaultNamespace());
        var vaultAuth = new SimpleObjectProperty<>(p.getValue().getVaultAuth());

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
                .sub(OptionsChoiceBuilder.builder().available(VaultAuth.getClasses()).property(vaultAuth).build().build(), vaultAuth)
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

    private boolean login() throws Exception {
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
            login();

            var b = CommandBuilder.of().add("vault", "read", "--format=json", "-non-interactive");
            if (vaultNamespace != null) {
                b.fixedEnvironment("VAULT_NAMESPACE", vaultNamespace);
            }
            b.addLiteral(key);
            b.fixedEnvironment("VAULT_ADDR", vaultAddress);

            var out = getOrStartShell().command(b).sensitive().readStdoutOrThrow();
            var json = JacksonMapper.getDefault().readTree(out);
            var data = json.get("data");
            if (data == null) {
                return null;
            }

            var username = Optional.ofNullable(data.get("username")).map(JsonNode::textValue).orElse(null);
            var password = Optional.ofNullable(data.get("password")).map(JsonNode::textValue).orElse(null);
            var creds = Credentials.of(username, password);
            return Result.of(creds, null);
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
