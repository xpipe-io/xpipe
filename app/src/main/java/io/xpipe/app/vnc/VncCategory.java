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
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdral-desktop_windows");
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var choiceBuilder = OptionsChoiceBuilder.builder()
                .property(prefs.vncClient)
                .available(ExternalVncClient.getClasses())
                .allowNull(false)
                .transformer(entryComboBox -> {
                    var websiteLinkButton =
                            new ButtonComp(AppI18n.observable("website"), new FontIcon("mdi2w-web"), () -> {
                                var c = prefs.vncClient.getValue();
                                if (c != null && c.getWebsite() != null) {
                                    Hyperlinks.open(c.getWebsite());
                                }
                            });
                    websiteLinkButton.minWidth(Region.USE_PREF_SIZE);

                    var hbox = new HBox(entryComboBox, websiteLinkButton.createRegion());
                    HBox.setHgrow(entryComboBox, Priority.ALWAYS);
                    hbox.setSpacing(10);
                    return hbox;
                })
                .build();
        var choice = choiceBuilder.build().buildComp().maxWidth(600);
        return new OptionsBuilder()
                .addTitle("vncClient")
                .sub(new OptionsBuilder().pref(prefs.vncClient).addComp(choice))
                .buildComp();
    }
}
