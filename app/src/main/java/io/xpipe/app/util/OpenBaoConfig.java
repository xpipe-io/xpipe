package io.xpipe.app.util;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.cred.SshIdentityStrategy;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.*;
import io.xpipe.app.pwman.PasswordManager;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.core.FilePath;
import io.xpipe.core.JacksonMapper;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Builder
@ToString
@Jacksonized
@EqualsAndHashCode
public class OpenBaoConfig implements Checkable {

    private static final CacheableConfiguration<OpenBaoConfig> INSTANCE = new CacheableConfiguration<>(
            OpenBaoConfig.class, "openBaoConfig", () -> OpenBaoConfig.builder().build());

    private final String vaultAddress;
    private final String vaultNamespace;
    private final String vaultSignerMount;

    public static CacheableConfiguration<OpenBaoConfig> get() {
        return INSTANCE;
    }

    public static void showDialog() {
        var modal = ModalOverlay.of(
                "openBao", createOptions(INSTANCE.getValue()).buildComp().prefWidth(500));
        modal.addButton(ModalButton.ok());
        modal.show();
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<OpenBaoConfig> p) {
        var vaultAddress = new SimpleStringProperty(p.getValue().getVaultAddress());
        var vaultNamespace = new SimpleStringProperty(p.getValue().getVaultNamespace());
        var vaultSignerMount = new SimpleStringProperty(p.getValue().getVaultSignerMount());

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
                .nameAndDescription("hashicorpVaultSignerMount")
                .addComp(new TextFieldComp(vaultSignerMount).apply(textField -> textField.setPromptText("ssh-client-signer")), vaultSignerMount)
                .documentationLink("https://openbao.org/docs/secrets/ssh/signed-ssh-certificates/#signing-key--role-configuration")
                .nameAndDescription("testConfig")
                .addComp(new TestButtonComp(() -> {
                    p.getValue().checkInstalled();

                    p.getValue().checkConnectivity();

                    p.getValue().checkMountPoint();

                    if (p.getValue().isLoginValid()) {
                        return true;
                    }

                    p.getValue().login();
                    return false;
                }))
                .bind(
                        () -> {
                            return OpenBaoConfig.builder()
                                    .vaultAddress(vaultAddress.get())
                                    .vaultNamespace(vaultNamespace.get())
                                    .vaultSignerMount(vaultSignerMount.get())
                                    .build();
                        },
                        p);
    }

    public void checkComplete() throws ValidationException {
        Validators.nonNull(vaultAddress, "Vault address");
    }

    public void renew(String role, FilePath privateKey, FilePath certificate) throws Exception {
        var sc = LocalShell.get(OpenBaoConfig.class);
        var publicKey = SshIdentityStrategy.getPublicKeyPath(privateKey);
        var mount = vaultSignerMount != null ? vaultSignerMount : "ssh-client-signer";
        var b = CommandBuilder.of()
                .add("bao", "write", mount + "/sign/" + role)
                .addQuotedKeyValue("public_key", "@" + publicKey.toUnix().toString());
        addEnvironment(b);
        sc.command(b).execute();
        var signedB = CommandBuilder.of()
                .add("bao", "write", "-field=signed_key", mount + "/sign/" + role)
                .addQuotedKeyValue("public_key", "@" + publicKey.toUnix().toString());
        addEnvironment(signedB);
        var signedContent = sc.command(signedB).readStdoutOrThrow();
        sc.view().writeRawFile(certificate, signedContent.getBytes(StandardCharsets.UTF_8));
    }

    private void addEnvironment(CommandBuilder b) {
        b.fixedEnvironment("VAULT_ADDR", getVaultAddress());
        if (getVaultNamespace() != null) {
            b.fixedEnvironment("VAULT_NAMESPACE", getVaultNamespace());
        }
    }

    private void checkInstalled() throws Exception {
        try {
            CommandSupport.isInLocalPathOrThrow("OpenBao CLI", "bao");
        } catch (Exception e) {
            ErrorEventFactory.preconfigure(
                    ErrorEventFactory.fromThrowable(e).expected().link("https://openbao.org/docs/install/"));
            throw e;
        }
    }

    private void checkConnectivity() throws Exception {
        var sc = LocalShell.get(OpenBaoConfig.class);
        var b = CommandBuilder.of().add("bao", "status");
        addEnvironment(b);
        try {
            sc.command(b).sensitive().execute();
        } catch (ProcessOutputException pex) {
            throw ErrorEventFactory.expected(pex);
        }
    }

    private void checkMountPoint() throws Exception {
        var sc = LocalShell.get(OpenBaoConfig.class);
        var mount = vaultSignerMount != null ? vaultSignerMount : "ssh-client-signer";
        var b = CommandBuilder.of().add("bao", "read", "-field=public_key " + mount + "/config/ca");
        addEnvironment(b);
        try {
            sc.command(b).sensitive().execute();
        } catch (ProcessOutputException pex) {
            throw ErrorEventFactory.expected(pex);
        }
    }

    private boolean isLoginValid() throws Exception {
        var sc = LocalShell.get(OpenBaoConfig.class);
        var b = CommandBuilder.of().add("bao", "token", "lookup", "-non-interactive", "-format=json");
        addEnvironment(b);
        var valid = sc.command(b).sensitive().executeAndCheck();
        return valid;
    }

    private void login() throws Exception {
        var sc = LocalShell.get(OpenBaoConfig.class);
        var script = ShellScript.lines(
                sc.getShellDialect().getSetEnvironmentVariableCommand("VAULT_ADDR", getVaultAddress()),
                getVaultNamespace() != null
                        ? sc.getShellDialect().getSetEnvironmentVariableCommand("VAULT_NAMESPACE", getVaultNamespace())
                        : null,
                sc.getShellDialect()
                        .getEchoCommand(
                                "Your current OpenBao login is expired. Please log in again with your currently selected auth method. The proper environment variables for your vault like VAULT_ADDR have already been configured in this session. The command syntax for this is:",
                                false),
                sc.getShellDialect().getEchoCommand("", false),
                sc.getShellDialect()
                        .getEchoCommand(
                                "bao login [-method=<auth_method>] [optional auth method specific parameters]", false),
                sc.getShellDialect().getEchoCommand("For a list of available auth methods, run bao auth list", false),
                sc.getShellDialect().getEchoCommand("", false));
        var scriptFile = ScriptHelper.createExecScript(sc, script.toString());
        TerminalLaunch.builder()
                .localScript(ShellScript.of(sc.getShellDialect().terminalInitCommand(sc, scriptFile.toString(), false)))
                .title("OpenBao login")
                .pauseOnExit(false)
                .logIfEnabled(false)
                .preferTabs(false)
                .launch();
    }

    public PasswordManager.Result querySecret(String key) {
        if (vaultAddress == null) {
            return null;
        }

        try {
            CommandSupport.isInLocalPathOrThrow("OpenBao CLI", "bao");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .expected()
                    .link("https://openbao.org/docs/install/")
                    .handle();
            return null;
        }

        try {
            checkConnectivity();

            if (!isLoginValid()) {
                login();
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

            var b = CommandBuilder.of().add("bao", "read", "-format=json", "-non-interactive");
            if (vaultNamespace != null) {
                b.fixedEnvironment("VAULT_NAMESPACE", vaultNamespace);
            }
            b.addLiteral(secretPath);
            b.fixedEnvironment("VAULT_ADDR", vaultAddress);

            var sc = LocalShell.get(OpenBaoConfig.class);
            var out = sc.command(b).sensitive().readStdoutOrThrow();
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
            var creds = PasswordManager.Credentials.of(username, password);
            var sshKey = PasswordManager.SshKey.of(publicKey, privateKey);
            var r = PasswordManager.Result.of(creds, sshKey);
            if (r == null) {
                if (subData.size() == 0) {
                    throw ErrorEventFactory.expected(new IllegalArgumentException("Found data at path but no fields"));
                }

                var l = new ArrayList<String>();
                subData.fieldNames().forEachRemaining(l::add);
                throw ErrorEventFactory.expected(new IllegalArgumentException(
                        "Found no data for specified fields, but only found the following unmapped fields: " + l));
            }
            return r;
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return null;
        }
    }
}
