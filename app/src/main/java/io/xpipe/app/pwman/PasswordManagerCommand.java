package io.xpipe.app.pwman;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.MenuHelper;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.SecretValue;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("passwordManagerCommand")
@Value
@Jacksonized
@Builder
public class PasswordManagerCommand implements PasswordManager {

    private static ShellControl SHELL;
    ShellScript script;

    @SuppressWarnings("unused")
    static OptionsBuilder createOptions(Property<PasswordManagerCommand> property) {
        var template = new SimpleObjectProperty<PasswordManagerCommandTemplate>();
        var script = new SimpleObjectProperty<>(
                property.getValue() != null ? property.getValue().getScript() : null);

        var templates = Comp.of(() -> {
            var cb = MenuHelper.createMenuButton();
            AppFontSizes.base(cb);
            cb.textProperty().bind(BindingsHelper.flatMap(template, t -> {
                return t != null ? AppI18n.observable(t.getId()) : AppI18n.observable("chooseTemplate");
            }));
            PasswordManagerCommandTemplate.ALL.forEach(e -> {
                var m = new MenuItem();
                m.textProperty().bind(AppI18n.observable(e.getId()));
                m.setOnAction(event -> {
                    script.set(new ShellScript(e.getTemplate()));
                    event.consume();
                });
                cb.getItems().add(m);
            });
            return cb;
        });

        var area = IntegratedTextAreaComp.script(
                new SimpleObjectProperty<>(DataStorage.get().local().ref()), script);
        area.minHeight(200);
        area.maxWidth(600);

        return new OptionsBuilder()
                .nameAndDescription("passwordManagerCommand")
                .addComp(area, script)
                .addComp(templates)
                .bind(() -> new PasswordManagerCommand(script.get()), property);
    }

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    public static SecretValue retrieveWithCommand(String cmd) {
        try (var cc = getOrStartShell().command(cmd).start()) {
            var out = cc.readStdoutOrThrow();

            // Dashlane fixes
            if (cmd.contains("dcli")) {
                out = out.lines()
                        .findFirst()
                        .map(s -> s.strip().replaceAll("\\s+$", ""))
                        .orElse("");
            }

            return InPlaceSecretValue.of(out);
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable("Unable to retrieve password with command " + cmd, ex)
                    .expected()
                    .handle();
            return null;
        }
    }

    @Override
    public CredentialResult retrieveCredentials(String key) {
        if (script == null || script.getValue().isBlank()) {
            return null;
        }

        var cmd = ExternalApplicationHelper.replaceVariableArgument(script.getValue(), "KEY", key);
        var secret = retrieveWithCommand(cmd);
        return new CredentialResult(null, secret);
    }

    @Override
    public String getKeyPlaceholder() {
        return "$KEY";
    }

    @Override
    public String getWebsite() {
        return null;
    }
}
