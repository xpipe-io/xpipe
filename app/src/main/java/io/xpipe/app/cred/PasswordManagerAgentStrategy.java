package io.xpipe.app.cred;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.pwman.PasswordManagerKeyConfiguration;
import io.xpipe.core.KeyValue;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

@JsonTypeName("passwordManager")
@Value
@Jacksonized
@Builder
public class PasswordManagerAgentStrategy implements SshIdentityStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(
            Property<PasswordManagerAgentStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var forward =
                new SimpleBooleanProperty(p.getValue() != null && p.getValue().isForwardAgent());
        var publicKey =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);

        var pwmanBinding = Bindings.createObjectBinding(() -> {
            var pwman = AppPrefs.get().passwordManager().getValue();
            if (pwman == null) {
                return AppI18n.get("passwordManagerEmpty");
            }

            if (!pwman.getKeyStrategy().supportsAgent()) {
                return AppI18n.get("passwordManagerNoAgentSupport");
            }

            return null;
        }, AppPrefs.get().passwordManager(), AppI18n.activeLanguage());
        var pwmanProp = new SimpleStringProperty();
        pwmanProp.bind(pwmanBinding);
        var pwmanDisplay = new HorizontalComp(List.of(
                        new LabelComp(pwmanProp)
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
                .hide(pwmanProp.isNull())
                .nameAndDescription("publicKey")
                .addComp(new SshAgentKeyListComp(config.getFileSystem(), p, publicKey), publicKey)
                .nameAndDescription("forwardAgent")
                .addToggle(forward)
                .nonNull()
                .hide(!config.isAllowAgentForward())
                .bind(
                        () -> {
                            return new PasswordManagerAgentStrategy(forward.get(), publicKey.get());
                        },
                        p);
    }

    boolean forwardAgent;
    String publicKey;

    private PasswordManagerKeyConfiguration getConfig() {
        var pwman = AppPrefs.get().passwordManager().getValue();
        return pwman != null && pwman.getKeyStrategy() != null && pwman.getKeyStrategy().supportsAgent() ? pwman.getKeyStrategy() : null;
    }

    @Override
    public void checkComplete() throws ValidationException {
        var config = getConfig();
        if (config == null) {
            throw new ValidationException(AppI18n.get("passwordManagerSshKeysNotSupported"));
        }
    }

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        var config = getConfig();
        if (config != null) {
            var strat = config.getSshIdentityStrategy(publicKey, forwardAgent);
            strat.prepareParent(parent);
        }
    }

    @Override
    public void buildCommand(CommandBuilder builder) {
        var config = getConfig();
        if (config != null) {
            var strat = config.getSshIdentityStrategy(publicKey, forwardAgent);
            strat.buildCommand(builder);
        }
    }

    @Override
    public List<KeyValue> configOptions(ShellControl sc) throws Exception {
        var config = getConfig();
        if (config != null) {
            var strat = config.getSshIdentityStrategy(publicKey, forwardAgent);
            return strat.configOptions(sc);
        } else {
            return List.of();
        }
    }
}
