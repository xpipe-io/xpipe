package io.xpipe.app.pwman;

import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.MenuHelper;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStorage;

import io.xpipe.core.JacksonMapper;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.MenuItem;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@JsonTypeName("passwordManagerCommand")
@Value
@Jacksonized
@Builder
public class PasswordManagerCommand implements PasswordManager {

    private static ShellControl SHELL;
    ShellScript script;

    @Override
    public boolean selectInitial() {
        return false;
    }

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.none();
    }

    @Override
    public boolean supportsKeyConfiguration() {
        return false;
    }

    @SuppressWarnings("unused")
    static OptionsBuilder createOptions(Property<PasswordManagerCommand> property) {
        var template = new SimpleObjectProperty<PasswordManagerCommandTemplate>();
        var script = new SimpleObjectProperty<>(
                property.getValue() != null ? property.getValue().getScript() : null);

        var templates = RegionBuilder.of(() -> {
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
        area.maxWidth(600);

        return new OptionsBuilder()
                .nameAndDescription("passwordManagerCommand")
                .addComp(area, script)
                .addComp(templates)
                .nameAndDescription("passwordManagerTest")
                .addComp(new PasswordManagerTestComp(true))
                .bind(() -> new PasswordManagerCommand(script.get()), property);
    }

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
                        .map(s -> s.strip().replaceAll("\\s+$", ""))
                        .orElse("");
            }

            return out;
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable("Unable to retrieve password with command " + cmd, ex)
                    .expected()
                    .handle();
            return null;
        }
    }

    @Override
    public Result query(String key) {
        if (script == null || script.getValue().isBlank()) {
            return null;
        }

        String effectiveKey;
        Map<String, String> fieldMap;
        var keySplit = key.split("\\?", 2);
        if (keySplit.length != 2 || keySplit[0].isEmpty() || keySplit[1].isEmpty()) {
            effectiveKey = key;
            fieldMap = null;
        } else {
            var rawKey = keySplit[0];
            var rawMap = Arrays.stream(keySplit[1].split("&"))
                    .filter(s -> s.split("=").length == 2)
                    .collect(Collectors.toMap(s -> s.split("=", 2)[0], s -> s.split("=", 2)[1]));
            if (rawMap.isEmpty()) {
                throw ErrorEventFactory.expected(new IllegalArgumentException("Invalid secret reference format"));
            }
            effectiveKey = rawKey;
            fieldMap = rawMap;
        }

        var cmd = ExternalApplicationHelper.replaceVariableArgument(script.getValue(), "KEY", effectiveKey);
        var out = retrieveWithCommand(cmd);
        if (out == null) {
            return null;
        }

        if (fieldMap == null && out.startsWith("{")) {
            ErrorEventFactory.fromThrowable(new IllegalArgumentException(
                    "Returned output is json, but no field mapping has been specified")).expected().handle();
            return null;
        }

        if (fieldMap != null && out.startsWith("{")) {
            JsonNode json = null;
            try {
                json = JacksonMapper.getDefault().readTree(out);
            } catch (Exception ignored) {}

            if (json != null && json.isObject()) {
                var username = Optional.ofNullable(json.get(fieldMap.get("user"))).map(JsonNode::textValue).orElse(null);
                var password = Optional.ofNullable(json.get(fieldMap.get("pass"))).map(JsonNode::textValue).orElse(null);
                var publicKey = Optional.ofNullable(json.get(fieldMap.get("public-key"))).map(JsonNode::textValue).orElse(null);
                var privateKey = Optional.ofNullable(json.get(fieldMap.get("private-key"))).map(JsonNode::textValue).orElse(null);
                var creds = Credentials.of(username, password);
                var sshKey = SshKey.of(publicKey, privateKey);
                var r = Result.of(creds, sshKey);
                if (r == null) {
                    if (json.size() == 0) {
                        throw ErrorEventFactory.expected(new IllegalArgumentException("Returned json output is empty"));
                    }

                    var l = new ArrayList<String>();
                    json.fieldNames().forEachRemaining(l::add);
                    throw ErrorEventFactory.expected(
                            new IllegalArgumentException("Found no data for specified fields, but only found the following unmapped fields: " + l));
                }
                return r;
            }
        }

        if (out.contains("\n")) {
            ErrorEventFactory.fromThrowable(new IllegalArgumentException(
                    "Returned secret output contains multiple lines. For raw secrets, the output should only be a single line string")).expected().handle();
            return null;
        }

        return Result.of(Credentials.of(null, out), null);
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
