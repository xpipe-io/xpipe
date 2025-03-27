package io.xpipe.app.password;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.*;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.util.ValidationException;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("passwordManagerCommand")
@Value
@Jacksonized
@Builder
public class PasswordManagerCommand implements PasswordManager {

    static OptionsBuilder createOptions(Property<PasswordManagerCommand> property) {
        var template = new SimpleObjectProperty<PasswordManagerCommandTemplate>();
        var script = new SimpleObjectProperty<>(property.getValue() != null ? property.getValue().getScript() : null);

        var templates = Comp.of(() -> {
            var cb = new MenuButton();
            AppFontSizes.base(cb);
            cb.textProperty().bind(BindingsHelper.flatMap(template, t -> {
                return t != null
                        ? AppI18n.observable(t.getId())
                        : AppI18n.observable("chooseTemplate");
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

        var area = IntegratedTextAreaComp.script(new SimpleObjectProperty<>(DataStorage.get().local().ref()), script);
        area.minHeight(200);
        area.maxWidth(600);

        return new OptionsBuilder().nameAndDescription("passwordManagerCommand").addComp(area, script).addComp(templates).bind(() -> new PasswordManagerCommand(script.get()), property);
    }

    @Override
    public void checkComplete() throws ValidationException {
        Validators.nonNull(script);
        Validators.notEmpty(script.getValue());
    }

    private static ShellControl SHELL;

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    public static String retrieveWithCommand(String cmd) {
        try (var cc = getOrStartShell().command(cmd).start()) {
            var out = cc.readStdoutOrThrow();

            // Dashlane fixes
            if (cmd.contains("dcli")) {
                out = out.lines()
                        .findFirst()
                        .map(s -> s.trim().replaceAll("\\s+$", ""))
                        .orElse(null);
            }

            return out;
        } catch (Exception ex) {
            ErrorEvent.fromThrowable("Unable to retrieve password with command " + cmd, ex)
                    .expected()
                    .handle();
            return null;
        }
    }

    ShellScript script;

    @Override
    public String getDocsLink() {
        return null;
    }

    @Override
    public String retrievePassword(String key) {
        var cmd = ExternalApplicationHelper.replaceFileArgument(script.getValue(), "KEY", key);
        return retrieveWithCommand(cmd);
    }

    @Override
    public String getKeyPlaceholder() {
        return "$KEY";
    }
}
