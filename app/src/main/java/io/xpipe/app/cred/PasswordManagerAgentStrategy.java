package io.xpipe.app.cred;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.pwman.PasswordManagerKeyConfiguration;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

@JsonTypeName("passwordManagerAgent")
@Value
@Jacksonized
@Builder
public class PasswordManagerAgentStrategy implements SshIdentityAgentStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(
            Property<PasswordManagerAgentStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var identifier =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getIdentifier() : null);

        var pwmanError = Bindings.createObjectBinding(
                () -> {
                    var pwman = AppPrefs.get().passwordManager().getValue();
                    if (pwman == null) {
                        return AppI18n.get("passwordManagerEmpty");
                    }

                    if (!pwman.supportsKeyConfiguration()) {
                        return AppI18n.get("passwordManagerNoAgentSupport");
                    }

                    if (!pwman.getKeyConfiguration().useAgent()) {
                        return AppI18n.get("passwordManagerNoAgentConfigured");
                    }

                    return null;
                },
                AppPrefs.get().passwordManager(),
                AppI18n.activeLanguage());
        var pwmanErrorProp = new SimpleStringProperty();
        pwmanErrorProp.bind(pwmanError);
        var pwmanDisplay = new HorizontalComp(List.of(
                        new LabelComp(pwmanErrorProp)
                                .maxWidth(10000)
                                .apply(label -> label.setAlignment(Pos.CENTER_LEFT))
                                .hgrow(),
                        new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
                                    AppPrefs.get().selectCategory("passwordManager");
                                })
                                .padding(new Insets(7))))
                .spacing(9);

        return new OptionsBuilder()
                .nameAndDescription("passwordManagerSshKeyConfig")
                .addComp(pwmanDisplay)
                .hide(pwmanErrorProp.isNull())
                .nameAndDescription(useKeyName() ? "agentKeyName" : "publicKey")
                .addComp(new SshAgentKeyListComp(config.getFileSystem(), p, identifier, useKeyName()), identifier)
                .disable(pwmanErrorProp.isNotNull())
                .nonNull()
                .hide(!config.isAllowAgentForward())
                .bind(
                        () -> {
                            return new PasswordManagerAgentStrategy(identifier.get());
                        },
                        p);
    }

    String identifier;

    private static PasswordManagerKeyConfiguration getConfig() {
        var pwman = AppPrefs.get().passwordManager().getValue();
        return pwman != null
                        && pwman.getKeyConfiguration() != null
                        && pwman.getKeyConfiguration().useAgent()
                ? pwman.getKeyConfiguration()
                : null;
    }

    private static boolean useKeyName() {
        var config = getConfig();
        return config != null && config.supportsAgentKeyNames();
    }

    @Override
    public void checkComplete() throws ValidationException {
        Validators.nonNull(identifier);
        var config = getConfig();
        if (config == null) {
            throw new ValidationException(AppI18n.get("passwordManagerSshKeysNotSupported"));
        }
    }

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        var config = getConfig();
        if (config != null) {
            var strat = config.getSshIdentityStrategy(null, false);
            strat.prepareParent(parent);
        }
    }

    @Override
    public FilePath determinetAgentSocketLocation(ShellControl parent) {
        var config = getConfig();
        return config != null ? FilePath.of(config.getDefaultSocketLocation()) : null;
    }

    @Override
    public void buildCommand(CommandBuilder builder) {
        var config = getConfig();
        if (config != null) {
            var strat = config.getSshIdentityStrategy(null, false);
            strat.buildCommand(builder);
        }
    }

    @Override
    public List<KeyValue> configOptions(ShellControl sc) throws Exception {
        var config = getConfig();
        if (config != null) {
            var strat = config.getSshIdentityStrategy(getPublicKeyStrategy().retrievePublicKey(), false);
            return strat.configOptions(sc);
        } else {
            return List.of();
        }
    }

    @Override
    public PublicKeyStrategy getPublicKeyStrategy() {
        if (identifier == null) {
            return null;
        }

        if (!useKeyName()) {
            return PublicKeyStrategy.Fixed.of(identifier);
        }

        return new PublicKeyStrategy.Dynamic(() -> {
            return SshAgentKeyList.findAgentIdentity(DataStorage.get().local().ref(), this, identifier)
                    .toString();
        });
    }
}
