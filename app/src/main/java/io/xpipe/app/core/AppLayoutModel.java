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
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@Getter
public class AppLayoutModel {

    @Data
    @Builder
    @Jacksonized
    public static class SavedState {

        double sidebarWidth;
        double browserConnectionsWidth;
    }

    private static AppLayoutModel INSTANCE;

    public static AppLayoutModel get() {
        return INSTANCE;
    }

    public static void init() {
        var state = AppCache.get("layoutState", SavedState.class, () -> new SavedState(250, 300));
        INSTANCE = new AppLayoutModel(state);
    }

    public static void reset() {
        AppCache.update("layoutState", INSTANCE.savedState);
        INSTANCE = null;
    }

    @Getter
    private final SavedState savedState;
    private final List<Entry> entries;
    private final Property<Entry> selected;

    public AppLayoutModel(SavedState savedState) {
        this.savedState = savedState;
        this.entries = createEntryList();
        this.selected = new SimpleObjectProperty<>(entries.get(1));
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
        var l = new ArrayList<>(List.of(
                new Entry(
                        AppI18n.observable("browser"), "mdi2f-file-cabinet", new BrowserComp(BrowserModel.DEFAULT)),
                new Entry(AppI18n.observable("connections"), "mdi2c-connection", new StoreLayoutComp()),
                new Entry(
                        AppI18n.observable("settings"), "mdsmz-miscellaneous_services", new PrefsComp(this))));
        // new SideMenuBarComp.Entry(AppI18n.observable("help"), "mdi2b-book-open-variant", new
        // StorageLayoutComp()),
        // new SideMenuBarComp.Entry(AppI18n.observable("account"), "mdi2a-account", new StorageLayoutComp())
        if (AppProperties.get().isDeveloperMode() && !AppProperties.get().isImage()) {
            l.add(new Entry(
                    AppI18n.observable("developer"), "mdi2b-book-open-variant", new DeveloperTabComp()));
        }

        l.add(new Entry(
                AppI18n.observable("explorePlans"),
                "mdi2p-professional-hexagon",
                LicenseProvider.get().overviewPage()));

        return l;
    }

    public record Entry(ObservableValue<String> name, String icon, Comp<?> comp) {}
}
