package io.xpipe.app.pwman;

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

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.Optional;

@Getter
@Builder
@ToString
@Jacksonized
@JsonTypeName("psono")
public class PsonoPasswordManager implements PasswordManager {

    private static ShellControl SHELL;
    private final InPlaceSecretValue apiKey;
    private final InPlaceSecretValue apiSecretKey;
    private final String serverUrl;

    @Override
    public boolean supportsKeyConfiguration() {
        return false;
    }

    @Override
    public boolean selectInitial() throws Exception {
        return LocalShell.getShell().view().findProgram("psonoci").isPresent();
    }

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.none();
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<PsonoPasswordManager> p) {
        var apiKey = new SimpleObjectProperty<>(p.getValue().getApiKey());
        var apiSecretKey = new SimpleObjectProperty<>(p.getValue().getApiSecretKey());
        var serverUrl = new SimpleStringProperty(p.getValue().getServerUrl());
        return new OptionsBuilder()
                .nameAndDescription("psonoServerUrl")
                .addComp(
                        new TextFieldComp(serverUrl)
                                .apply(struc -> {
                                    struc.setPromptText("https://www.psono.pw/server");
                                })
                                .maxWidth(600),
                        serverUrl)
                .nameAndDescription("psonoApiKey")
                .addComp(new SecretFieldComp(apiKey, false).maxWidth(600), apiKey)
                .nameAndDescription("psonoApiSecretKey")
                .addComp(new SecretFieldComp(apiSecretKey, false).maxWidth(600), apiSecretKey)
                .nameAndDescription("passwordManagerTest")
                .addComp(new PasswordManagerTestComp(true))
                .bind(
                        () -> {
                            return PsonoPasswordManager.builder()
                                    .apiKey(apiKey.get())
                                    .apiSecretKey(apiSecretKey.get())
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
        if (serverUrl == null || apiKey == null || apiSecretKey == null) {
            return null;
        }

        try {
            CommandSupport.isInLocalPathOrThrow("Psono CLI", "psonoci");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .expected()
                    .link("https://doc.psono.com/user/psonoci/install.html")
                    .handle();
            return null;
        }

        try {
            getOrStartShell().view().setSensitiveEnvironmentVariable("PSONO_CI_API_KEY_ID", apiKey.getSecretValue());
            getOrStartShell()
                    .view()
                    .setSensitiveEnvironmentVariable("PSONO_CI_API_SECRET_KEY_HEX", apiSecretKey.getSecretValue());
            var cmd = getOrStartShell()
                    .command(CommandBuilder.of()
                            .add("psonoci")
                            .add("--server-url")
                            .addLiteral(serverUrl)
                            .add("secret", "get")
                            .addLiteral(key)
                            .add("json"))
                    .sensitive();
            var r = JacksonMapper.getDefault().readTree(cmd.readStdoutOrThrow());
            var username = Optional.of(r.required("username"))
                    .filter(n -> !n.isNull())
                    .map(JsonNode::textValue)
                    .orElse(null);
            var password = Optional.of(r.required("password"))
                    .filter(n -> !n.isNull())
                    .map(JsonNode::textValue)
                    .orElse(null);
            return Result.of(Credentials.of(username, password), null);
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("psonoPlaceholder");
    }

    @Override
    public String getWebsite() {
        return "https://psono.com/";
    }
}
