package io.xpipe.app.cred;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.Validator;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
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
                        new TextFieldComp(pwmanProp)
                                .apply(struc -> struc.setEditable(false))
                                .hgrow(),
                        new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
                                    AppPrefs.get().selectCategory("passwordManager");
                                })
                                .padding(new Insets(7))))
                .spacing(9);

        return new OptionsBuilder()
                .nameAndDescription("passwordManagerSshKeyConfig")
                .addComp(pwmanDisplay)
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

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        var pwman = AppPrefs.get().passwordManager().getValue();
        var strat = pwman.getKeyStrategy().getSshIdentityStrategy();
        strat.prepareParent(parent);
    }

    @Override
    public void buildCommand(CommandBuilder builder) {
        var pwman = AppPrefs.get().passwordManager().getValue();
        var strat = pwman.getKeyStrategy().getSshIdentityStrategy();
        strat.buildCommand(builder);
    }

    @Override
    public List<KeyValue> configOptions(ShellControl sc) throws Exception {
        var pwman = AppPrefs.get().passwordManager().getValue();
        var strat = pwman.getKeyStrategy().getSshIdentityStrategy();
        return strat.configOptions(sc);
    }
}
