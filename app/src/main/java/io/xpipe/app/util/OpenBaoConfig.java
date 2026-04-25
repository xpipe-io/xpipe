package io.xpipe.app.util;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.cred.SshIdentityStrategy;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.*;
import io.xpipe.app.pwman.OpenBaoPasswordManager;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.core.FilePath;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.charset.StandardCharsets;


@Getter
@Builder
@ToString
@Jacksonized
@EqualsAndHashCode
public class OpenBaoConfig implements Checkable {

    private static final CacheableConfiguration<OpenBaoConfig> INSTANCE = new CacheableConfiguration<>(OpenBaoConfig.class, "openBaoConfig", () -> OpenBaoConfig.builder().build());

    private final String vaultAddress;
    private final String vaultNamespace;

    public static CacheableConfiguration<OpenBaoConfig> get() {
        return INSTANCE;
    }

    public static void showDialog() {
        var modal = ModalOverlay.of("openBao", createOptions(INSTANCE.getValue()).buildComp().prefWidth(500));
        modal.addButton(ModalButton.ok());
        modal.show();
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<OpenBaoConfig> p) {
        var vaultAddress = new SimpleStringProperty(p.getValue().getVaultAddress());
        var vaultNamespace = new SimpleStringProperty(p.getValue().getVaultNamespace());

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
                .nameAndDescription("testConfig")
                .addComp(new TestButtonComp(() -> {
                    p.getValue().checkInstalled();

                    p.getValue().checkConnectivity();

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
        var b = CommandBuilder.of().add("bao", "write", "ssh-client-signer/sign/" + role).addQuotedKeyValue("public_key",
                "@" + publicKey.toUnix().toString());
        addEnvironment(b);
        sc.command(b).execute();
        var signedB = CommandBuilder.of().add("bao", "write", "-field=signed_key", "ssh-client-signer/sign/" + role).addQuotedKeyValue(
                "public_key", "@" + publicKey.toUnix().toString());
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
            ErrorEventFactory.preconfigure(ErrorEventFactory.fromThrowable(e).expected().link("https://openbao.org/docs/install/"));
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
                        ? sc.getShellDialect()
                          .getSetEnvironmentVariableCommand("VAULT_NAMESPACE", getVaultNamespace())
                        : null,
                sc.getShellDialect()
                        .getEchoCommand(
                                "Your current OpenBao login is expired. Please log in again with your currently selected auth method. The proper environment variables for your vault like VAULT_ADDR have already been configured in this session. The command syntax for this is:",
                                false),
                sc.getShellDialect().getEchoCommand("", false),
                sc.getShellDialect()
                        .getEchoCommand(
                                "bao login -method=<auth_method> [optional auth method specific parameters]",
                                false),
                sc.getShellDialect()
                        .getEchoCommand(
                                "For a list of available auth methods, run bao auth list",
                                false),
                sc.getShellDialect().getEchoCommand("", false)
        );
        var scriptFile = ScriptHelper.createExecScript(sc, script.toString());
        TerminalLaunch.builder()
                .localScript(ShellScript.of(
                        sc.getShellDialect().terminalInitCommand(sc, scriptFile.toString(), false)))
                .title("OpenBao login")
                .pauseOnExit(false)
                .logIfEnabled(false)
                .preferTabs(false)
                .launch();
    }
}
