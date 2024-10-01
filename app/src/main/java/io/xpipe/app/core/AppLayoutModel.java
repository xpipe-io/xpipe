package io.xpipe.app.core;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.browser.session.BrowserSessionComp;
import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.comp.base.TerminalViewDockComp;
import io.xpipe.app.comp.store.StoreLayoutComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.util.LabelGraphic;
import io.xpipe.app.prefs.AppPrefsComp;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.LicenseProvider;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.*;
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
        this.selected = new SimpleObjectProperty<>(entries.get(0));
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
        selected.setValue(entries.get(1));
    }

    public void selectTerminal() {
        selected.setValue(entries.get(2));
    }

    public void selectSettings() {
        selected.setValue(entries.get(3));
    }

    public void selectLicense() {
        selected.setValue(entries.get(4));
    }

    public void selectConnections() {
        selected.setValue(entries.get(0));
    }

    private List<Entry> createEntryList() {
        var l = new ArrayList<>(List.of(
                new Entry(
                        AppI18n.observable("connections"),
                        new LabelGraphic.IconGraphic("mdi2c-connection"),
                        new StoreLayoutComp(),
                        null,
                        new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN)),
                new Entry(
                        AppI18n.observable("browser"),
                        new LabelGraphic.IconGraphic("mdi2f-file-cabinet"),
                        new BrowserSessionComp(BrowserSessionModel.DEFAULT),
                        null,
                        new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.SHORTCUT_DOWN)),
                new Entry(
                        AppI18n.observable("terminal"),
                        new LabelGraphic.IconGraphic("mdi2m-monitor-screenshot"),
                        new TerminalViewDockComp(),
                        null,
                        new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.SHORTCUT_DOWN)),
                new Entry(
                        AppI18n.observable("settings"),
                        new LabelGraphic.IconGraphic("mdsmz-miscellaneous_services"),
                        new AppPrefsComp(),
                        null,
                        new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.SHORTCUT_DOWN)),
                new Entry(
                        AppI18n.observable("explorePlans"),
                        new LabelGraphic.IconGraphic("mdi2p-professional-hexagon"),
                        LicenseProvider.get().overviewPage(),
                        null,
                        null),
                new Entry(
                        AppI18n.observable("visitGithubRepository"),
                        new LabelGraphic.IconGraphic("mdi2g-github"),
                        null,
                        () -> Hyperlinks.open(Hyperlinks.GITHUB),
                        null),
                new Entry(
                        AppI18n.observable("discord"),
                        new LabelGraphic.IconGraphic("mdi2d-discord"),
                        null,
                        () -> Hyperlinks.open(Hyperlinks.DISCORD),
                        null),
                new Entry(
                        AppI18n.observable("api"),
                        new LabelGraphic.IconGraphic("mdi2c-code-json"),
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

        var now = Instant.now();
        var zone = ZoneId.of(ZoneId.SHORT_IDS.get("PST"));
        var phStart = ZonedDateTime.of(2024, 10, 22, 0, 1, 0, 0, zone).toInstant();
        var clicked = AppCache.get("phClicked",Boolean.class,() -> false);
        var phShow = now.isAfter(phStart) && !clicked;
        if (phShow) {
            l.add(new Entry(
                    new SimpleStringProperty("Product Hunt"),
                    new LabelGraphic.ImageGraphic("app:producthunt-color.png", 24),
                    null,
                    () -> {
                        AppCache.update("phClicked", true);
                        Hyperlinks.open(Hyperlinks.PRODUCT_HUNT);
                    },
                    null));
        }
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
            ObservableValue<String> name,
            LabelGraphic icon,
            Comp<?> comp,
            Runnable action,
            KeyCombination combination) {}
}
