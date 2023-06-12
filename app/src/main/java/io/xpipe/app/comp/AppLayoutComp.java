package io.xpipe.app.comp;

import io.xpipe.app.browser.BrowserComp;
import io.xpipe.app.browser.BrowserModel;
import io.xpipe.app.comp.about.AboutTabComp;
import io.xpipe.app.comp.base.SideMenuBarComp;
import io.xpipe.app.comp.storage.store.StoreLayoutComp;
import io.xpipe.app.core.*;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.prefs.AppPrefs;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppLayoutComp extends Comp<CompStructure<BorderPane>> {

    private final List<SideMenuBarComp.Entry> entries;
    private final Property<SideMenuBarComp.Entry> selected;

    public AppLayoutComp() {
        entries = createEntryList();
        selected = new SimpleObjectProperty<>(AppState.get().isInitialLaunch() ? entries.get(1) : entries.get(0));

        shortcut(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN), structure -> {
            AppActionLinkDetector.detectOnPaste();
        });
    }

    @SneakyThrows
    private List<SideMenuBarComp.Entry> createEntryList() {
        var l = new ArrayList<>(List.of(
                new SideMenuBarComp.Entry(
                        AppI18n.observable("browser"),
                        "mdi2f-file-cabinet",
                        new BrowserComp(BrowserModel.DEFAULT)),
                new SideMenuBarComp.Entry(AppI18n.observable("connections"), "mdi2c-connection", new StoreLayoutComp()),
                // new SideMenuBarComp.Entry(AppI18n.observable("data"), "mdsal-dvr", new SourceCollectionLayoutComp()),
                new SideMenuBarComp.Entry(
                        AppI18n.observable("settings"), "mdsmz-miscellaneous_services", new PrefsComp(this)),
                // new SideMenuBarComp.Entry(AppI18n.observable("help"), "mdi2b-book-open-variant", new
                // StorageLayoutComp()),
                // new SideMenuBarComp.Entry(AppI18n.observable("account"), "mdi2a-account", new StorageLayoutComp()),
                new SideMenuBarComp.Entry(AppI18n.observable("about"), "mdi2p-package-variant", new AboutTabComp())));
        if (AppProperties.get().isDeveloperMode()) {
            l.add(new SideMenuBarComp.Entry(
                    AppI18n.observable("developer"), "mdi2b-book-open-variant", new DeveloperTabComp()));
        }

        //        l.add(new SideMenuBarComp.Entry(AppI18n.observable("abc"), "mdi2b-book-open-variant", Comp.of(() -> {
        //            var fi = new FontIcon("mdsal-dvr");
        //            fi.setIconSize(30);
        //            fi.setIconColor(Color.valueOf("#111C"));
        //            JfxHelper.addEffect(fi);
        //            return new StackPane(fi);
        //        })));

        return l;
    }

    @Override
    public CompStructure<BorderPane> createBase() {
        var map = new HashMap<SideMenuBarComp.Entry, Region>();
        getRegion(entries.get(0), map);
        getRegion(entries.get(1), map);

        var pane = new BorderPane();
        var sidebar = new SideMenuBarComp(selected, entries);
        pane.setCenter(getRegion(selected.getValue(), map));
        pane.setRight(sidebar.createRegion());
        selected.addListener((c, o, n) -> {
            if (o != null && o.equals(entries.get(2))) {
                AppPrefs.get().save();
            }

            pane.setCenter(getRegion(n, map));
        });
        AppFont.normal(pane);
        return new SimpleCompStructure<>(pane);
    }

    private Region getRegion(SideMenuBarComp.Entry entry, Map<SideMenuBarComp.Entry, Region> map) {
        if (map.containsKey(entry)) {
            return map.get(entry);
        }

        Region r = entry.comp().createRegion();
        map.put(entry, r);
        return r;
    }

    public List<SideMenuBarComp.Entry> getEntries() {
        return entries;
    }

    public SideMenuBarComp.Entry getSelected() {
        return selected.getValue();
    }

    public Property<SideMenuBarComp.Entry> selectedProperty() {
        return selected;
    }
}
