package io.xpipe.app.core;

import io.xpipe.app.browser.BrowserComp;
import io.xpipe.app.browser.BrowserModel;
import io.xpipe.app.comp.DeveloperTabComp;
import io.xpipe.app.comp.store.StoreLayoutComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.prefs.PrefsComp;
import io.xpipe.app.util.LicenseProvider;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class AppLayoutModel {

    private static AppLayoutModel INSTANCE;
    private final List<Entry> entries;
    private final Property<Entry> selected;

    public AppLayoutModel() {
        this.entries = createEntryList();
        this.selected = new SimpleObjectProperty<>(entries.get(1));
    }

    public static AppLayoutModel get() {
        return INSTANCE;
    }

    public static void init() {
        INSTANCE = new AppLayoutModel();
    }

    public void selectBrowser() {
        selected.setValue(entries.get(0));
    }

    public void selectSettings() {
        selected.setValue(entries.get(2));
    }

    public void selectLicense() {
        selected.setValue(entries.get(3));
    }

    public void selectConnections() {
        selected.setValue(entries.get(1));
    }

    private List<Entry> createEntryList() {
        var l = new ArrayList<>(List.of(new Entry(AppI18n.observable("browser"), "mdi2f-file-cabinet", new BrowserComp(BrowserModel.DEFAULT)),
                new Entry(AppI18n.observable("connections"), "mdi2c-connection", new StoreLayoutComp()),
                new Entry(AppI18n.observable("settings"), "mdsmz-miscellaneous_services", new PrefsComp())));
        // new SideMenuBarComp.Entry(AppI18n.observable("help"), "mdi2b-book-open-variant", new
        // StorageLayoutComp()),
        // new SideMenuBarComp.Entry(AppI18n.observable("account"), "mdi2a-account", new StorageLayoutComp())
        if (AppProperties.get().isDeveloperMode() && !AppProperties.get().isImage()) {
            l.add(new Entry(AppI18n.observable("developer"), "mdi2b-book-open-variant", new DeveloperTabComp()));
        }

        l.add(new Entry(AppI18n.observable("explorePlans"), "mdi2p-professional-hexagon", LicenseProvider.get().overviewPage()));

        return l;
    }

    public record Entry(ObservableValue<String> name, String icon, Comp<?> comp) {}
}
