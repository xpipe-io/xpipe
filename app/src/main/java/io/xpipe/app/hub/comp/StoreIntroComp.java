package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.IntroComp;
import io.xpipe.app.comp.base.IntroListComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.ScanDialog;

import javafx.scene.layout.Region;

import java.util.List;

public class StoreIntroComp extends SimpleRegionBuilder {

    @Override
    public Region createSimple() {
        var hub = new IntroComp("storeIntro", new LabelGraphic.NodeGraphic(() -> PrettyImageHelper.ofSpecificFixedSize(
                        "welcome/wave.svg", 80, 144)
                .build()));
        hub.setButtonAction(() -> {
            ScanDialog.showSingleAsync(DataStorage.get().local());
        });
        hub.setButtonGraphic(new LabelGraphic.IconGraphic("mdi2i-import"));
        hub.setButtonDefault(true);

        var docs = new IntroComp("storeIntroDocs", new LabelGraphic.IconGraphic("mdi2b-book-open-variant"));
        docs.setButtonGraphic(new LabelGraphic.IconGraphic("mdi2b-book-open-variant"));
        docs.setButtonAction(() -> {
            DocumentationLink.INTRO.open();
        });

        var list = new IntroListComp(List.of(hub, docs));
        return list.build();
    }
}
