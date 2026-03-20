package io.xpipe.app.cred;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.App;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

@JsonTypeName("passwordManagerInPlaceKey")
@Value
@Jacksonized
@Builder
public class PasswordManagerInPlaceKeyStrategy implements SshIdentityAgentStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(
            Property<PasswordManagerInPlaceKeyStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var options = new OptionsBuilder();
        var prefs = AppPrefs.get();
        var keyProperty = options.map(p, PasswordManagerInPlaceKeyStrategy::getKey);
        var field = new TextFieldComp(keyProperty).apply(struc -> struc.promptTextProperty()
                .bind(Bindings.createStringBinding(
                        () -> {
                            return prefs.passwordManager().getValue() != null
                                    ? prefs.passwordManager().getValue().getKeyPlaceholder()
                                    : "?";
                        },
                        prefs.passwordManager())));
        var button = new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
            AppPrefs.get().selectCategory("passwordManager");
            App.getApp().getStage().requestFocus();
        });
        var content = new InputGroupComp(List.of(field, button));
        content.setMainReference(field);
        return options.nameAndDescription("passwordManagerInPlaceKeyKey")
                .addComp(content, keyProperty)
                .nonNull()
                .bind(
                        () -> {
                            return PasswordManagerInPlaceKeyStrategy.builder()
                                    .key(keyProperty.get())
                                    .build();
                        },
                        p);
    }

    String key;

    @Override
    public void prepareParent(ShellControl parent) {}

    @Override
    public void buildCommand(CommandBuilder builder) {}

    @Override
    public List<KeyValue> configOptions(ShellControl sc) {
        return List.of();
    }

    @Override
    public PublicKeyStrategy getPublicKeyStrategy() {
        return null;
    }

    @Override
    public FilePath determinetAgentSocketLocation(ShellControl parent) {
        return null;
    }
}
