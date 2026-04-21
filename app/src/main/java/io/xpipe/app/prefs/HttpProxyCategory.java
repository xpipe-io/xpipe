package io.xpipe.app.prefs;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreCreationDialog;
import io.xpipe.app.hub.comp.StoreCreationModel;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.*;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.HttpProxy;
import io.xpipe.core.OsType;
import javafx.beans.property.SimpleObjectProperty;

public class HttpProxyCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "httpProxy";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2s-server-network-outline");
    }

    @Override
    protected BaseRegionBuilder<?, ?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .title("httpProxyConfiguration")
                .sub(proxy())
                .sub(new OptionsBuilder()
                        .pref(prefs.disableHttpsTlsCheck)
                        .addToggle(prefs.disableHttpsTlsCheck)
                )
                .buildComp();
    }

    private OptionsBuilder proxy() {
        var prefs = AppPrefs.get();
        var initial = prefs.httpProxy.getValue();
        var initialRef = initial != null ? DataStorage.get().getStoreEntryIfPresent(initial)
                                           .map(e -> e.ref())
                                           .filter(e -> HttpProxy.canUseAsProxy(e))
                                           .orElse(null) : null;
        var ref = new SimpleObjectProperty<>(initialRef);
        ref.addListener((observable, oldValue, newValue) -> {
            prefs.httpProxy.setValue(
                    newValue != null
                            ? newValue.get().getUuid()
                            : null);
        });
        var proxyChoice = new DelayedInitComp(
                RegionBuilder.of(() -> {
                    var comp = new StoreChoiceComp<>(
                            null,
                            ref,
                            DataStore.class,
                            r -> HttpProxy.canUseAsProxy(r),
                            StoreViewState.get().getAllConnectionsCategory()) {
                        @Override
                        protected String toName(DataStoreEntry entry) {
                            if (entry == null) {
                                return AppI18n.get("systemDefault");
                            }

                            return super.toName(entry);
                        }

                        @Override
                        protected String toGraphic(DataStoreEntry entry) {
                            if (entry == null) {
                                return "proc:networkProxy_icon.svg";
                            }

                            return super.toGraphic(entry);
                        }
                    };
                    return comp.build();
                }),
                () -> StoreViewState.get() != null && StoreViewState.get().isInitialized());
        proxyChoice.maxWidth(getCompWidth());

        var addButton = new ButtonComp(AppI18n.observable("addProxy"), () -> {
            var selected = DataStoreProviders.byId("networkProxy").orElseThrow();
            StoreCreationDialog.showCreation(
                    null, selected.defaultStore(DataStorage.get().getSelectedCategory()),
                    DataStoreCreationCategory.NETWORK,
                    ignored -> {},
                    false);
        });

        return new OptionsBuilder()
                .nameAndDescription("httpProxy")
                .addComp(proxyChoice, ref)
                .addComp(addButton);
    }
}
