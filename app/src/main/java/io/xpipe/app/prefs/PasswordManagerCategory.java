package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.pwman.PasswordManager;
import io.xpipe.app.util.*;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import org.kordamp.ikonli.javafx.FontIcon;

public class PasswordManagerCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "passwordManager";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdomz-vpn_key");
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var testPasswordManagerValue = new SimpleStringProperty();

        var choiceBuilder = OptionsChoiceBuilder.builder()
                .property(prefs.passwordManager)
                .subclasses(PasswordManager.getClasses())
                .allowNull(true)
                .transformer(entryComboBox -> {
                    var websiteLinkButton =
                            new ButtonComp(AppI18n.observable("website"), new FontIcon("mdi2w-web"), () -> {
                                var l = prefs.passwordManager.getValue().getWebsite();
                                if (l != null) {
                                    Hyperlinks.open(l);
                                }
                            });
                    websiteLinkButton.minWidth(Region.USE_PREF_SIZE);
                    websiteLinkButton.disable(Bindings.createBooleanBinding(
                            () -> {
                                return prefs.passwordManager.getValue() == null || prefs.passwordManager.getValue().getWebsite() == null;
                            },
                            prefs.passwordManager));

                    var hbox = new HBox(entryComboBox, websiteLinkButton.createRegion());
                    HBox.setHgrow(entryComboBox, Priority.ALWAYS);
                    hbox.setSpacing(10);
                    return hbox;
                })
                .build();
        var choice = choiceBuilder.build().buildComp().maxWidth(600);

        var testInput = new PasswordManagerTestComp(testPasswordManagerValue, true);
        testInput.maxWidth(getCompWidth());
        testInput.hgrow();

        return new OptionsBuilder()
                .addTitle("passwordManager")
                .sub(new OptionsBuilder()
                        .pref(prefs.passwordManager)
                        .addComp(choice)
                        .nameAndDescription("passwordManagerCommandTest")
                        .addComp(testInput)
                        .hide(BindingsHelper.map(prefs.passwordManager, p -> p == null)))
                .buildComp();
    }
}
