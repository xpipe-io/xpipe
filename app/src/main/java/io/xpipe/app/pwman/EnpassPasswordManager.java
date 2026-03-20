package io.xpipe.app.pwman;

import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.secret.SecretManager;
import io.xpipe.app.secret.SecretPromptStrategy;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.*;
import io.xpipe.core.FilePath;
import io.xpipe.core.JacksonMapper;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TextField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@JsonTypeName("enpass")
@Getter
@Builder
@ToString
@Jacksonized
public class EnpassPasswordManager implements PasswordManager {

    private static final UUID MASTER_PASSWORD_UUID = UUID.randomUUID();
    private static ShellControl SHELL;
    private final FilePath vaultPath;

    @Override
    public boolean supportsKeyConfiguration() {
        return false;
    }

    @Override
    public boolean selectInitial() throws Exception {
        return LocalShell.getShell().view().findProgram("enpass-cli").isPresent();
    }

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.none();
    }

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<EnpassPasswordManager> p) {
        var prop = new SimpleObjectProperty<>(p.getValue().getVaultPath());
        var comp = new ContextualFileReferenceChoiceComp(
                new SimpleObjectProperty<>(DataStorage.get().local().ref()),
                prop,
                null,
                List.of(),
                e -> e.equals(DataStorage.get().local()),
                true);
        comp.apply(struc -> {
            var text = (TextField) struc.getChildren().getFirst();
            text.requestFocus();
            text.setPromptText(AppSystemInfo.ofCurrent()
                    .getUserHome()
                    .resolve("Documents/Enpass/Vaults/primary/vault.json")
                    .toString());

            // Show prompt text, remove focus
            struc.focusedProperty().addListener((observable, oldValue, newValue) -> {
                Platform.runLater(() -> {
                    struc.getParent().requestFocus();
                });
            });
        });
        comp.maxWidth(600);
        return new OptionsBuilder()
                .nameAndDescription("enpassVaultFile")
                .addComp(comp, prop)
                .nameAndDescription("passwordManagerTest")
                .addComp(new PasswordManagerTestComp(true))
                .bind(
                        () -> {
                            return EnpassPasswordManager.builder()
                                    .vaultPath(prop.getValue())
                                    .build();
                        },
                        p);
    }

    @Override
    public synchronized Result query(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("Enpass CLI", "enpass-cli");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .link("https://github.com/hazcod/enpass-cli")
                    .handle();
            return null;
        }

        if (vaultPath == null) {
            throw ErrorEventFactory.expected(new IllegalArgumentException("No vault path has been set"));
        }

        var vaultDir = vaultPath.asLocalPath();
        if (!Files.exists(vaultDir)) {
            throw ErrorEventFactory.expected(
                    new IllegalArgumentException("Vault path " + vaultPath + " does not exist"));
        }
        if (Files.isRegularFile(vaultDir)) {
            vaultDir = vaultDir.getParent();
        }

        var pass = SecretManager.retrieve(
                new SecretPromptStrategy(), "Enter Enpass vault master password", MASTER_PASSWORD_UUID, 0, true);
        if (pass == null) {
            return null;
        }

        try {
            var sc = getOrStartShell();
            try (var command = sc.command(CommandBuilder.of()
                            .add("enpass-cli", "-json", "-vault")
                            .addFile(vaultDir)
                            .add("show")
                            .addQuoted(key))
                    .sensitive()
                    .start()) {
                ThreadHelper.sleep(50);
                sc.writeLine(pass.getSecretValue());
                var out = command.readStdoutIfPossible();
                if (out.isEmpty()) {
                    return null;
                }

                var json = JacksonMapper.getDefault()
                        .readTree(out.get().lines().skip(1).collect(Collectors.joining("\n")));
                if (!json.isArray()) {
                    return null;
                }

                if (json.size() == 0) {
                    throw ErrorEventFactory.expected(
                            new IllegalArgumentException("No items were found matching the title " + key));
                }

                if (json.size() > 1) {
                    var matches = new ArrayList<String>();
                    json.iterator().forEachRemaining(item -> {
                        var title = item.get("title");
                        if (title != null) {
                            matches.add(title.asText());
                        }
                    });
                    throw ErrorEventFactory.expected(new IllegalArgumentException(
                            "Ambiguous item name, multiple password entries match: " + String.join(", ", matches)));
                }

                var login = Optional.ofNullable(json.get(0).get("login"))
                        .map(JsonNode::textValue)
                        .orElse(null);
                var secret = Optional.ofNullable(json.get(0).get("password"))
                        .map(JsonNode::textValue)
                        .orElse(null);
                return Result.of(Credentials.of(login, secret), null);
            }
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return "Item title";
    }

    @Override
    public String getWebsite() {
        return "https://www.enpass.io/";
    }
}
