package io.xpipe.app.core;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.browser.session.BrowserSessionComp;
import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.comp.store.StoreLayoutComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.prefs.AppPrefsComp;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.LicenseProvider;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@Getter
public class AppLayoutModel {

    private static AppLayoutModel INSTANCE;

    private final SavedState savedState;

    private final List<Entry> entries;

    private final Property<Entry> selected;

    public AppLayoutModel(SavedState savedState) {
        this.savedState = savedState;
        this.entries = createEntryList();
        this.selected = new SimpleObjectProperty<>(entries.get(1));
    }

    public static AppLayoutModel get() {
        return INSTANCE;
    }

    public static void init() {
        var state = AppCache.get("layoutState", SavedState.class, () -> new SavedState(260, 300));
        INSTANCE = new AppLayoutModel(state);
    }

    public static void reset() {
        AppCache.update("layoutState", INSTANCE.savedState);
        INSTANCE = null;
    }

    public void selectBrowser() {
        selected.setValue(entries.getFirst());
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
                        AppI18n.observable("browser"),
                        "mdi2f-file-cabinet",
                        new BrowserSessionComp(BrowserSessionModel.DEFAULT),
                        null,
                        new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN)),
                new Entry(
                        AppI18n.observable("connections"),
                        "mdi2c-connection",
                        new StoreLayoutComp(),
                        null,
                        new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.SHORTCUT_DOWN)),
                new Entry(
                        AppI18n.observable("settings"),
                        "mdsmz-miscellaneous_services",
                        new AppPrefsComp(),
                        null,
                        new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.SHORTCUT_DOWN)),
                new Entry(
                        AppI18n.observable("explorePlans"),
                        "mdi2p-professional-hexagon",
                        LicenseProvider.get().overviewPage(),
                        null,
                        null),
                new Entry(
                        AppI18n.observable("visitGithubRepository"),
                        "mdi2g-github",
                        null,
                        () -> Hyperlinks.open(Hyperlinks.GITHUB),
                        null),
                new Entry(
                        AppI18n.observable("discord"),
                        "mdi2d-discord",
                        null,
                        () -> Hyperlinks.open(Hyperlinks.DISCORD),
                        null),
                new Entry(
                        AppI18n.observable("api"),
                        "mdi2c-code-json",
                        null,
                        () -> Hyperlinks.open(
                                "http://localhost:" + AppBeaconServer.get().getPort()),
                        null)
//                new Entry(
//                        AppI18n.observable("webtop"),
//                        "mdi2d-desktop-mac",
//                        null,
//                        () -> Hyperlinks.open(Hyperlinks.GITHUB_WEBTOP),
//                        null)
        ));
        return l;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SavedState {

        double sidebarWidth;
        double browserConnectionsWidth;
    }

    public record Entry(
            ObservableValue<String> name, String icon, Comp<?> comp, Runnable action, KeyCombination combination) {}
}
