package io.xpipe.app.core;

import io.xpipe.app.browser.BrowserFullSessionComp;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.store.StoreLayoutComp;
import io.xpipe.app.prefs.AppPrefsComp;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@Getter
public class AppLayoutModel {

    private static AppLayoutModel INSTANCE;

    private final SavedState savedState;

    private final List<Entry> entries;

    private final Property<Entry> selected;

    private final ObservableList<QueueEntry> queueEntries;

    private final BooleanProperty ptbAvailable = new SimpleBooleanProperty();

    public AppLayoutModel(SavedState savedState) {
        this.savedState = savedState;
        this.entries = createEntryList();
        this.selected = new SimpleObjectProperty<>(entries.getFirst());
        this.queueEntries = FXCollections.observableArrayList();
    }

    public static AppLayoutModel get() {
        return INSTANCE;
    }

    public static void init() {
        var state = AppCache.getNonNull("layoutState", SavedState.class, () -> new SavedState(270, 300));
        INSTANCE = new AppLayoutModel(state);
    }

    public static void reset() {
        if (INSTANCE == null) {
            return;
        }

        AppCache.update("layoutState", INSTANCE.savedState);
        INSTANCE = null;
    }

    public void selectBrowser() {
        PlatformThread.runLaterIfNeeded(() -> {
            selected.setValue(entries.get(1));
        });
    }

    public void selectSettings() {
        PlatformThread.runLaterIfNeeded(() -> {
            selected.setValue(entries.get(2));
        });
    }

    public void selectLicense() {
        PlatformThread.runLaterIfNeeded(() -> {
            selected.setValue(entries.get(3));
        });
    }

    public void selectConnections() {
        PlatformThread.runLaterIfNeeded(() -> {
            selected.setValue(entries.getFirst());
        });
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
                        new BrowserFullSessionComp(BrowserFullSessionModel.DEFAULT),
                        null,
                        new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.SHORTCUT_DOWN)),
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
                        AppI18n.observable("docs"),
                        new LabelGraphic.IconGraphic("mdi2b-book-open-variant"),
                        null,
                        () -> Hyperlinks.open(Hyperlinks.DOCS),
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
                        null)));
                //                new Entry(
                //                        AppI18n.observable("api"),
                //                        new LabelGraphic.IconGraphic("mdi2c-code-json"),
                //                        null,
                //                        () -> Hyperlinks.open(
                //                                "http://localhost:" + AppBeaconServer.get().getPort()),
                //                        null),);
        if (AppDistributionType.get() != AppDistributionType.WEBTOP) {
            l.add(new Entry(
                    AppI18n.observable("webtop"),
                    new LabelGraphic.IconGraphic("mdi2d-desktop-mac"),
                    null,
                    () -> Hyperlinks.open(Hyperlinks.GITHUB_WEBTOP),
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

    @Value
    public static class QueueEntry {

        ObservableValue<String> name;
        LabelGraphic icon;
        Runnable action;
    }
}
