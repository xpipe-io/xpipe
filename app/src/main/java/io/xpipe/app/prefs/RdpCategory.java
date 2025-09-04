package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.rdp.ExternalRdpClient;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

public class RdpCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "rdp";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2r-remote-desktop");
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();

        var visit = new ButtonComp(AppI18n.observable("website"), new FontIcon("mdi2w-web"), () -> {
            var t = prefs.rdpClientType().getValue();
            if (t == null || t.getWebsite() == null) {
                return;
            }

            Hyperlinks.open(t.getWebsite());
        });


        var choiceBuilder = OptionsChoiceBuilder.builder()
                .property(prefs.rdpClientType)
                .available(ExternalRdpClient.getClasses())
                .allowNull(false)
                .transformer(entryComboBox -> {
                    var websiteLinkButton =
                            new ButtonComp(AppI18n.observable("website"), new FontIcon("mdi2w-web"), () -> {
                                var c = prefs.rdpClientType.getValue();
                                if (c != null && c.getWebsite() != null) {
                                    Hyperlinks.open(c.getWebsite());
                                }
                            });
                    websiteLinkButton.minWidth(Region.USE_PREF_SIZE);

                    var hbox = new HBox(entryComboBox, websiteLinkButton.createRegion());
                    hbox.setMaxWidth(600);
                    HBox.setHgrow(entryComboBox, Priority.ALWAYS);
                    hbox.setSpacing(10);
                    return hbox;
                })
                .build();

        return new OptionsBuilder()
                .addTitle("rdpConfiguration")
                .sub(new OptionsBuilder()
                        .nameAndDescription("rdpClient")
                        .longDescription(DocumentationLink.RDP)
                        .sub(choiceBuilder.build(), prefs.rdpClientType))
                .buildComp();
    }
}
