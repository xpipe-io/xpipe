package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ChoiceComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.rdp.ExternalRdpClient;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.OptionsBuilder;

import javafx.geometry.Pos;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

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

        var choice = ChoiceComp.ofTranslatable(
                prefs.rdpClientType, PrefsChoiceValue.getSupported(ExternalRdpClient.class), false);

        var visit = new ButtonComp(AppI18n.observable("website"), new FontIcon("mdi2w-web"), () -> {
            var t = prefs.rdpClientType().getValue();
            if (t == null || t.getWebsite() == null) {
                return;
            }

            Hyperlinks.open(t.getWebsite());
        });

        var h = new HorizontalComp(List.of(choice.hgrow(), visit)).apply(struc -> {
            struc.get().setAlignment(Pos.CENTER_LEFT);
            struc.get().setSpacing(10);
        });
        h.maxWidth(600);

        return new OptionsBuilder()
                .addTitle("rdpConfiguration")
                .sub(new OptionsBuilder()
                        .nameAndDescription("rdpClient")
                        .longDescription(DocumentationLink.RDP)
                        .addComp(h, prefs.rdpClientType)
                        .nameAndDescription("customRdpClientCommand")
                        .addComp(
                                new TextFieldComp(prefs.customRdpClientCommand, true)
                                        .apply(struc -> struc.get().setPromptText("myrdpclient -c $FILE"))
                                        .hide(prefs.rdpClientType.isNotEqualTo(ExternalRdpClient.CUSTOM))
                                        .prefWidth(600),
                                prefs.customRdpClientCommand))
                .buildComp();
    }
}
