package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.IntroComp;
import io.xpipe.app.comp.base.IntroListComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.platform.LabelGraphic;
import javafx.beans.property.BooleanProperty;
import javafx.scene.layout.Region;

import java.util.List;

public class StoreScriptSourcesIntroComp extends SimpleComp {


    @Override
    public Region createSimple() {
        var intro = new IntroComp("scriptSourcesIntro", new LabelGraphic.IconGraphic("mdi2d-download"));
        intro.setButtonGraphic(new LabelGraphic.IconGraphic("mdi2p-play-circle"));
        intro.setButtonDefault(true);
        intro.setButtonAction(() -> {
            StoreCreationDialog.showCreation(DataStoreProviders.byId("scriptSource").orElseThrow(), DataStoreCreationCategory.SCRIPT);
        });
        var list = new IntroListComp(List.of(intro));
        return list.createRegion();
    }
}
