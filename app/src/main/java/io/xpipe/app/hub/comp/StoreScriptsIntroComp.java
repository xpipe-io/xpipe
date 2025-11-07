package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.IntroComp;
import io.xpipe.app.comp.base.IntroListComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.platform.LabelGraphic;

import javafx.beans.property.BooleanProperty;
import javafx.scene.layout.Region;

import java.util.List;

public class StoreScriptsIntroComp extends SimpleComp {

    private final BooleanProperty show;

    public StoreScriptsIntroComp(BooleanProperty show) {
        this.show = show;
    }

    @Override
    public Region createSimple() {
        var top = new IntroComp("scriptsIntro", new LabelGraphic.IconGraphic("mdi2s-script-text"));

        var bottom = new IntroComp("scriptsIntroBottom", new LabelGraphic.IconGraphic("mdi2t-tooltip-edit"));
        bottom.setButtonGraphic(new LabelGraphic.IconGraphic("mdi2p-play-circle"));
        bottom.setButtonDefault(true);
        bottom.setButtonAction(() -> {
            AppCache.update("scriptsIntroCompleted", true);
            show.set(false);
        });

        var list = new IntroListComp(List.of(top, bottom));
        return list.createRegion();
    }
}
