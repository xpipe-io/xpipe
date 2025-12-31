package io.xpipe.app.pwman;

import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.comp.base.SecretFieldComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.FilePath;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.OsType;

import javafx.beans.property.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Getter
@Builder
@ToString
@Jacksonized
@JsonTypeName("passbolt")
public class PassboltPasswordManager implements PasswordManager {

    private static ShellControl SHELL;
    private final String serverUrl;
    private final InPlaceSecretValue passphrase;
    private final Path privateKey;

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<PassboltPasswordManager> p) {
        var serverUrl = new SimpleStringProperty(p.getValue().getServerUrl());
        var passphrase = new SimpleObjectProperty<>(p.getValue().getPassphrase());
        var privateKey = new SimpleObjectProperty<>(FilePath.of(p.getValue().getPrivateKey()));

        ContextualFileReferenceChoiceComp chooser = new ContextualFileReferenceChoiceComp(
                new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()),
                privateKey,
                null,
                List.of(),
                e -> e.equals(DataStorage.get().local()),
                false);
        chooser.setPrompt(new ReadOnlyObjectWrapper<>(FilePath.of("passbolt_private.asc")));

        return new OptionsBuilder()
                .nameAndDescription("passboltServerUrl")
                .addComp(
                        new TextFieldComp(serverUrl)
                                .apply(struc -> {
                                    struc.get().setPromptText("https://cloud.passbolt.com/myorg");
                                })
                                .maxWidth(600),
                        serverUrl)
                .nonNull()
                .nameAndDescription("passboltPassphrase")
                .addComp(new SecretFieldComp(passphrase, false).maxWidth(600), passphrase)
                .nonNull()
                .nameAndDescription("passboltPrivateKey")
                .addComp(chooser, privateKey)
                .nonNull()
                .bind(
                        () -> {
                            return PassboltPasswordManager.builder()
                                    .passphrase(passphrase.get())
                                    .privateKey(
                                            privateKey.get() != null
                                                    ? privateKey.get().asLocalPath()
                                                    : null)
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

    private Optional<String> parseConfig() throws IOException {
        var dir =
                switch (OsType.ofLocal()) {
                    case OsType.Windows ignored -> AppSystemInfo.ofWindows().getRoamingAppData();
                    case OsType.Linux ignored ->
                        AppSystemInfo.ofLinux().getUserHome().resolve(".config");
                    case OsType.MacOs ignored ->
                        AppSystemInfo.ofMacOs().getUserHome().resolve("Library", "Application Support");
                };
        var path = dir.resolve("go-passbolt-cli", "go-passbolt-cli.toml");
        if (!Files.exists(path)) {
            return Optional.empty();
        }

        var s = Files.readString(path);
        return Optional.of(s);
    }

    @JsonIgnore
    private Boolean validConfig;

    @JsonIgnore
    private boolean mfaTotpInteractiveConfigured;

    @Override
    public synchronized CredentialResult retrieveCredentials(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("Passbolt CLI", "passbolt");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .expected()
                    .link("https://github.com/passbolt/go-passbolt-cli")
                    .handle();
            return null;
        }

        if (validConfig == null) {
            try {
                var config = parseConfig();
                if (config.isPresent()) {
                    mfaTotpInteractiveConfigured =
                            config.get().contains("totptoken") && !config.get().contains("totptoken = ''");
                    var cmd = getOrStartShell().command(CommandBuilder.of().add("passbolt", "verify"));
                    var r = cmd.executeAndCheck();
                    validConfig = r;
                } else {
                    validConfig = false;
                }
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
                validConfig = false;
            }
        }

        var b = CommandBuilder.of().add("passbolt");
        if (!validConfig) {
            if (serverUrl == null || passphrase == null || privateKey == null) {
                return null;
            }

            b.addIf(AppPrefs.get().disableApiHttpsTlsCheck().getValue(), "--tlsSkipVerify")
                    .add("--serverAddress")
                    .addLiteral(serverUrl)
                    .add("--userPassword")
                    .addLiteral(passphrase.getSecretValue())
                    .add("--userPrivateKeyFile")
                    .addFile(privateKey);
        }
        b.add("--mfaMode", mfaTotpInteractiveConfigured ? "noninteractive-totp" : "none");
        b.add("get", "resource").add("--id").addLiteral(key).add("--json");

        try {
            var cmd = getOrStartShell().command(b).sensitive();
            var r = JacksonMapper.getDefault().readTree(cmd.readStdoutOrThrow());
            var username = r.required("username").asText();
            var password = r.required("password").asText();
            return new CredentialResult(
                    username.isEmpty() ? null : username, password.isEmpty() ? null : InPlaceSecretValue.of(password));
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("passboltPlaceholder");
    }

    @Override
    public String getWebsite() {
        return "https://www.passbolt.com";
    }
}
