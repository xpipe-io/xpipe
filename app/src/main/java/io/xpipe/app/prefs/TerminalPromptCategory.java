package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.terminal.TerminalPrompt;
import io.xpipe.app.util.*;

import javafx.beans.binding.Bindings;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import org.kordamp.ikonli.javafx.FontIcon;

public class TerminalPromptCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "terminalPrompt";
    }

    @Override
    protected Comp<?> create() {
        return new OptionsBuilder()
                .addTitle("terminalPromptConfiguration")
                .sub(terminalPrompt())
                .buildComp();
    }

    private OptionsBuilder terminalPrompt() {
        var prefs = AppPrefs.get();
        var choiceBuilder = OptionsChoiceBuilder.builder()
                .property(prefs.terminalPrompt)
                .allowNull(true)
                .subclasses(TerminalPrompt.getClasses())
                .transformer(entryComboBox -> {
                    var websiteLinkButton =
                            new ButtonComp(AppI18n.observable("website"), new FontIcon("mdi2w-web"), () -> {
                                var l = prefs.terminalPrompt().getValue().getDocsLink();
                                if (l != null) {
                                    Hyperlinks.open(l);
                                }
                            });
                    websiteLinkButton.minWidth(Region.USE_PREF_SIZE);
                    websiteLinkButton.disable(Bindings.createBooleanBinding(
                            () -> {
                                return prefs.terminalPrompt.getValue() == null
                                        || prefs.terminalPrompt.getValue().getDocsLink() == null;
                            },
                            prefs.terminalPrompt));

                    var hbox = new HBox(entryComboBox, websiteLinkButton.createRegion());
                    HBox.setHgrow(entryComboBox, Priority.ALWAYS);
                    hbox.setSpacing(10);
                    return hbox;
                })
                .build();
        var choice = choiceBuilder.build().buildComp();
        choice.maxWidth(getCompWidth());
        return new OptionsBuilder().nameAndDescription("terminalPrompt").addComp(choice, prefs.terminalPrompt);
    }
}
