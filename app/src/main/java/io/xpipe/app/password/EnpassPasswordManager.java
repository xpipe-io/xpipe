package io.xpipe.app.password;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.*;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.JacksonMapper;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TextField;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
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

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    public static OptionsBuilder createOptions(Property<EnpassPasswordManager> p) {
        var prop = new SimpleObjectProperty<>(p.getValue().getVaultPath());
        var comp = new ContextualFileReferenceChoiceComp(
                new SimpleObjectProperty<>(DataStorage.get().local().ref()),
                prop,null, List.of());
        comp.apply(struc -> {
            var text = (TextField) struc.get().getChildren().getFirst();
            text.requestFocus();
            text.setPromptText(System.getProperty("user.home") + "/Documents/Enpass/Vaults/primary/vault.json");

            // Show prompt text, remove focus
            struc.get().focusedProperty().addListener((observable, oldValue, newValue) -> {
                Platform.runLater(() -> {
                    struc.get().getParent().requestFocus();
                });
            });
        });
        comp.maxWidth(600);
        return new OptionsBuilder()
                .nameAndDescription("enpassVaultFile")
                .addComp(comp, prop)
                .bind(
                        () -> {
                            return EnpassPasswordManager.builder().vaultPath(prop.getValue()).build();
                        },
                        p);
    }

    private final FilePath vaultPath;

    @Override
    public synchronized CredentialResult retrieveCredentials(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("Enpass CLI", "enpass-cli");
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e)
                    .link("https://github.com/hazcod/enpass-cli")
                    .handle();
            return null;
        }

        if (vaultPath == null) {
            throw ErrorEvent.expected(new IllegalArgumentException("No vault path has been set"));
        }

        var vaultDir = vaultPath.asLocalPath();
        if (!Files.exists(vaultDir)) {
            throw ErrorEvent.expected(new IllegalArgumentException("Vault path " + vaultPath + " does not exist"));
        }
        if (Files.isRegularFile(vaultDir)) {
            vaultDir = vaultDir.getParent();
        }

        var pass = SecretManager.retrieve(new SecretRetrievalStrategy.Prompt(), "Enter Enpass vault master password", MASTER_PASSWORD_UUID, 0, true);
        if (pass == null) {
            return null;
        }

        try {
            var sc = getOrStartShell();
            try (var command = sc.command(CommandBuilder.of().add("enpass-cli", "-json", "-vault").addFile(vaultDir).add("show").addQuoted(key)).start()) {
                ThreadHelper.sleep(50);
                sc.writeLine(pass.getSecretValue());
                var out = command.readStdoutIfPossible();
                if (out.isEmpty()) {
                    return null;
                }

                var json = JacksonMapper.getDefault().readTree(out.get().lines().skip(1).collect(Collectors.joining("\n")));
                if (!json.isArray()) {
                    return null;
                }

                if (json.size() == 0) {
                    throw ErrorEvent.expected(new IllegalArgumentException("No items were found matching the title " + key));
                }

                if (json.size() > 1) {
                    var matches = new ArrayList<String>();
                    json.iterator().forEachRemaining(item -> {
                        var title = item.get("title");
                        if (title != null) {
                            matches.add(title.asText());
                        }
                    });
                    throw ErrorEvent.expected(new IllegalArgumentException("Ambiguous item name, multiple password entries match: " + String.join(", ", matches)));
                }

                var secret = json.get(0).get("password").asText();
                return null;
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return "Item title";
    }
}
