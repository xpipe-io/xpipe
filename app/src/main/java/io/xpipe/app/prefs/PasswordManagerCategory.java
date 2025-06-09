package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.pwman.PasswordManager;
import io.xpipe.app.util.*;

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
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var testPasswordManagerValue = new SimpleStringProperty();

        var choiceBuilder = OptionsChoiceBuilder.builder()
                .property(prefs.passwordManager)
                .subclasses(PasswordManager.getClasses())
                .allowNull(true)
                .transformer(entryComboBox -> {
                    var docsLinkButton = new ButtonComp(
                            AppI18n.observable("docs"), new FontIcon("mdi2h-help-circle-outline"), () -> {
                                Hyperlinks.open(DocumentationLink.PASSWORD_MANAGER.getLink());
                            });
                    docsLinkButton.minWidth(Region.USE_PREF_SIZE);

                    var hbox = new HBox(entryComboBox, docsLinkButton.createRegion());
                    HBox.setHgrow(entryComboBox, Priority.ALWAYS);
                    hbox.setSpacing(10);
                    hbox.setMaxWidth(getCompWidth());
                    return hbox;
                })
                .build();
        var choice = choiceBuilder.build().buildComp();

        var testInput = new PasswordManagerTestComp(testPasswordManagerValue);
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
