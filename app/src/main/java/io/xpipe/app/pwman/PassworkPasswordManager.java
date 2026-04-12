package io.xpipe.app.pwman;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.app.comp.base.SecretFieldComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.JacksonMapper;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.Optional;

@Getter
@Builder
@ToString
@Jacksonized
@JsonTypeName("passwork")
public class PassworkPasswordManager implements PasswordManager {

    private static ShellControl SHELL;

    private final String serverUrl;
    private final InPlaceSecretValue token;
    private final InPlaceSecretValue masterKey;

    @Override
    public boolean supportsKeyConfiguration() {
        return false;
    }

    @Override
    public boolean selectInitial() throws Exception {
        return LocalShell.getShell().view().findProgram("passwork-cli").isPresent();
    }

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.none();
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<PassworkPasswordManager> p) {
        var serverUrl = new SimpleStringProperty(p.getValue().getServerUrl());
        var token = new SimpleObjectProperty<>(p.getValue().getToken());
        var masterKey = new SimpleObjectProperty<>(p.getValue().getMasterKey());
        return new OptionsBuilder()
                .nameAndDescription("passworkServerUrl")
                .addComp(
                        new TextFieldComp(serverUrl)
                                .apply(struc -> {
                                    struc.setPromptText("https://myorg.passwork.io");
                                })
                                .maxWidth(600),
                        serverUrl)
                .nonNull()
                .nameAndDescription("passworkAccessToken")
                .addComp(new SecretFieldComp(token, false).maxWidth(600), token)
                .nonNull()
                .nameAndDescription("passworkMasterKey")
                .addComp(new SecretFieldComp(masterKey, false).maxWidth(600), masterKey)
                .nonNull()
                .nameAndDescription("passwordManagerTest")
                .addComp(new PasswordManagerTestComp(true))
                .bind(
                        () -> {
                            return PassworkPasswordManager.builder()
                                    .token(token.get())
                                    .masterKey(masterKey.get())
                                    .serverUrl(serverUrl.get())
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

    @Override
    public synchronized Result query(String key) {
        if (serverUrl == null || token == null || masterKey == null) {
            return null;
        }

        try {
            CommandSupport.isInLocalPathOrThrow("Passwork CLI", "passwork-cli");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .expected()
                    .link("https://passwork.pro/user-guides/api-and-integrations/passwork-cli/")
                    .handle();
            return null;
        }

        try {
            var fixedServerUrl = serverUrl.startsWith("http") ? serverUrl : "https://" + serverUrl;
            getOrStartShell().view().setSensitiveEnvironmentVariable("PASSWORK_HOST", fixedServerUrl);
            getOrStartShell().view().setSensitiveEnvironmentVariable("PASSWORK_TOKEN", token.getSecretValue());
            getOrStartShell()
                    .view()
                    .setSensitiveEnvironmentVariable("PASSWORK_MASTER_KEY", masterKey.getSecretValue());
            var user = getOrStartShell()
                    .command(CommandBuilder.of()
                            .add("passwork-cli")
                            .add("get")
                            .add("--password-id")
                            .addQuoted(key)
                            .add("--field", "login"))
                    .sensitive()
                    .readStdoutOrThrow();
            if ("None".equals(user)) {
                user = null;
            }

            var pass = getOrStartShell()
                    .command(CommandBuilder.of()
                            .add("passwork-cli")
                            .add("get")
                            .add("--password-id")
                            .addQuoted(key)
                            .add("--field", "password"))
                    .sensitive()
                    .readStdoutOrThrow();
            if ("None".equals(pass)) {
                pass = null;
            }

            return Result.of(Credentials.of(user, pass), null);
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("passworkPlaceholder");
    }

    @Override
    public String getWebsite() {
        return "https://passwork.pro/";
    }
}
