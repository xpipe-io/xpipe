package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.ChoiceComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.util.OptionsBuilder;

public class RdpCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "rdp";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("rdpConfiguration")
                .sub(new OptionsBuilder()
                        .nameAndDescription("rdpClient")
                        .addComp(ChoiceComp.ofTranslatable(
                                prefs.rdpClientType, PrefsChoiceValue.getSupported(ExternalRdpClientType.class), false))
                        .nameAndDescription("customRdpClientCommand")
                        .addComp(new TextFieldComp(prefs.customRdpClientCommand, true)
                                .apply(struc -> struc.get().setPromptText("myrdpclient -c $FILE"))
                                .hide(prefs.rdpClientType.isNotEqualTo(ExternalRdpClientType.CUSTOM)))
                )
                .buildComp();
    }
}
