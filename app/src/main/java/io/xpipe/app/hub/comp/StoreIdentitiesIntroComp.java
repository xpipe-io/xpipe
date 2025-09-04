package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.IntroComp;
import io.xpipe.app.comp.base.IntroListComp;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;

import javafx.scene.layout.Region;

import java.util.List;

public class StoreIdentitiesIntroComp extends SimpleComp {

    @Override
    public Region createSimple() {
        var top = new IntroComp(
                "identitiesIntro",
                new LabelGraphic.IconGraphic("mdi2a-account-group"));
        top.setButtonDefault(true);
        top.setButtonGraphic(new LabelGraphic.IconGraphic("mdi2p-play-circle"));
        top.setButtonAction(() -> {
            var canSync = DataStorage.get().supportsSync();
            var prov = canSync
                    ? DataStoreProviders.byId("syncedIdentity").orElseThrow()
                    : DataStoreProviders.byId("localIdentity").orElseThrow();
            StoreCreationDialog.showCreation(prov, DataStoreCreationCategory.IDENTITY);
        });

        var bottom = new IntroComp(
                "identitiesIntroBottom",
                new LabelGraphic.IconGraphic("mdi2g-git"));
        bottom.setButtonGraphic(new LabelGraphic.IconGraphic("mdi2p-play-circle"));
        bottom.setButtonAction(() -> {
            AppPrefs.get().selectCategory("vaultSync");
        });

        var list = new IntroListComp(List.of(top, bottom));
        return list.createRegion();
    }
}
