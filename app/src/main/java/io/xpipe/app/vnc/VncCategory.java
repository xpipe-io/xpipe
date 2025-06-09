package io.xpipe.app.vnc;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.AppPrefsCategory;
import io.xpipe.app.util.*;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import org.kordamp.ikonli.javafx.FontIcon;

public class VncCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "vnc";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var choiceBuilder = OptionsChoiceBuilder.builder()
                .property(prefs.vncClient)
                .subclasses(ExternalVncClient.getClasses())
                .allowNull(false)
                .transformer(entryComboBox -> {
                    var docsLinkButton = new ButtonComp(
                            AppI18n.observable("docs"), new FontIcon("mdi2h-help-circle-outline"), () -> {
                                Hyperlinks.open(DocumentationLink.VNC_CLIENTS.getLink());
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
        return new OptionsBuilder()
                .addTitle("vncClient")
                .sub(new OptionsBuilder().pref(prefs.vncClient).addComp(choice))
                .buildComp();
    }
}
