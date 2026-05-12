package io.xpipe.app.prefs;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppCertStore;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreCreationDialog;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.HttpHelper;
import io.xpipe.app.util.HttpProxy;

import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

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
        var testComp = new TestButtonComp(() -> {
           var addr = new SimpleStringProperty();
           var addrField = new TextFieldComp(addr).apply(textField -> textField.setPromptText("https://example.com"));
           var modal = ModalOverlay.of("proxyTestAddressDialogTitle", new OptionsBuilder()
                   .nameAndDescription("proxyTestAddress").addComp(addrField, addr).buildComp().prefWidth(450));
           modal.addButton(ModalButton.cancel());
           modal.addButton(ModalButton.ok());
           modal.showAndWait();
           if (addr.get() == null) {
               return false;
           }

            var effectiveAddr = addr.get();
           if (!effectiveAddr.startsWith("http")) {
               effectiveAddr = "https://" + effectiveAddr;
           }

            var res = HttpHelper.client().send(HttpRequest.newBuilder().GET().uri(URI.create(effectiveAddr)).build(), HttpResponse.BodyHandlers.ofString());
           HttpHelper.checkOrThrow(res);
           return true;
        });
        return new OptionsBuilder()
                .title("httpProxyConfiguration")
                .sub(proxy())
                .sub(new OptionsBuilder()
                        .addComp(testComp)
                        .pref(prefs.disableHttpsTlsCheck)
                        .addToggle(prefs.disableHttpsTlsCheck)
                        .nameAndDescription("browseCertificateStore")
                        .addComp(new ButtonComp(AppI18n.observable("browseCertificateStoreButton"), () -> {
                            ThreadHelper.runFailableAsync(() -> {
                                Files.createDirectories(AppCertStore.getDir());
                                DesktopHelper.browseFile(AppCertStore.getDir());
                            });
                        }))
                )
                .buildComp();
    }

    private OptionsBuilder proxy() {
        var prefs = AppPrefs.get();
        var ref = new SimpleObjectProperty<DataStoreEntryRef<DataStore>>();
        var proxyChoice = new DelayedInitComp(
                RegionBuilder.of(() -> {
                    var initial = prefs.httpProxy.getValue();
                    var initialRef = initial != null
                            ? DataStorage.get().getStoreEntries().stream()
                                    .filter(e -> {
                                        return initial.equals(ProcessControlProvider.get()
                                                .getHttpProxy(e.ref().asNeeded())
                                                .orElse(null));
                                    })
                                    .map(DataStoreEntry::ref)
                                    .findFirst()
                                    .orElse(null)
                            : null;
                    ref.set(initialRef);
                    ref.addListener((observable, oldValue, newValue) -> {
                        prefs.httpProxy.setValue(
                                newValue != null
                                        ? ProcessControlProvider.get()
                                                .getHttpProxy(newValue)
                                                .orElse(null)
                                        : null);
                    });

                    var comp =
                            new StoreChoiceComp<>(
                                    null,
                                    ref,
                                    DataStore.class,
                                    r -> HttpProxy.canUseAsProxy(r.asNeeded()),
                                    StoreViewState.get().getAllConnectionsCategory(),
                                    null) {
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
                    null,
                    selected.defaultStore(DataStorage.get().getSelectedCategory()),
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
