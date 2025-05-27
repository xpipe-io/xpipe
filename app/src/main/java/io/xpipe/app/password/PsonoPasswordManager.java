package io.xpipe.app.password;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.SecretFieldComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.*;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.SecretValue;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Getter
@Builder
@ToString
@Jacksonized
@JsonTypeName("psono")
public class PsonoPasswordManager implements PasswordManager {

    private final InPlaceSecretValue apiKey;
    private final InPlaceSecretValue apiSecretKey;
    private final String serverUrl;

    public static OptionsBuilder createOptions(Property<PsonoPasswordManager> p) {
        var apiKey = new SimpleObjectProperty<>(p.getValue().getApiKey());
        var apiSecretKey = new SimpleObjectProperty<>(p.getValue().getApiSecretKey());
        var serverUrl = new SimpleStringProperty(p.getValue().getServerUrl());
        return new OptionsBuilder()
                .nameAndDescription("psonoServerUrl")
                .addComp(new TextFieldComp(serverUrl).apply(struc -> {
                    struc.get().setPromptText("https://www.psono.pw/server");
                }).maxWidth(600), serverUrl)
                .nameAndDescription("psonoApiKey")
                .addComp(new SecretFieldComp(apiKey, false).maxWidth(600), apiKey)
                .nameAndDescription("psonoApiSecretKey")
                .addComp(new SecretFieldComp(apiSecretKey, false).maxWidth(600), apiSecretKey)
                .bind(
                        () -> {
                            return PsonoPasswordManager.builder().apiKey(apiKey.get()).apiSecretKey(apiSecretKey.get()).serverUrl(serverUrl.get()).build();
                        },
                        p);
    }

    private static ShellControl SHELL;

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    @Override
    public String retrievePassword(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("Psono CLI", "psonoci");
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e)
                    .expected()
                    .link("https://doc.psono.com/user/psonoci/install.html")
                    .handle();
            return null;
        }

        try {
            var cmd = getOrStartShell()
                    .command(CommandBuilder.of()
                            .add("psonoci", "--api-key-id")
                            .addLiteral(apiKey.getSecretValue())
                            .add("--api-secret-key-hex")
                            .addLiteral(apiSecretKey.getSecretValue())
                            .add("--server-url")
                            .addLiteral(serverUrl)
                            .add("secret", "get")
                            .addLiteral(key)
                            .add("password"));
            cmd.setSensitive();;
            var r = cmd.readStdoutOrThrow();
            return r;
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("psonoPlaceholder");
    }
}
