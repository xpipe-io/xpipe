package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.source.store.GuiDsStoreCreator;
import io.xpipe.app.core.AppFont;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.extension.fxcomps.impl.VerticalComp;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class StoreCreationBarComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var newStreamStore = new ButtonComp(
                        I18n.observable("addStreamStore"), new FontIcon("mdi2c-card-plus-outline"), () -> {
                            GuiDsStoreCreator.showCreation(DataStoreProvider.Category.STREAM);
                        })
                .shortcut(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN))
                .apply(new FancyTooltipAugment<>("addStreamStore"));

        var newShellStore = new ButtonComp(
                        I18n.observable("addShellStore"), new FontIcon("mdi2h-home-plus-outline"), () -> {
                            GuiDsStoreCreator.showCreation(DataStoreProvider.Category.SHELL);
                        })
                .shortcut(new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN))
                .apply(new FancyTooltipAugment<>("addShellStore"));

        var newDbStore = new ButtonComp(
                        I18n.observable("addDatabaseStore"), new FontIcon("mdi2d-database-plus-outline"), () -> {
                            GuiDsStoreCreator.showCreation(DataStoreProvider.Category.DATABASE);
                        })
                .shortcut(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN))
                .apply(new FancyTooltipAugment<>("addDatabaseStore"));

        var box = new VerticalComp(List.of(newShellStore, newDbStore, newStreamStore));
        box.apply(s -> AppFont.medium(s.get()));
        var bar = box.createRegion();
        bar.getStyleClass().add("bar");
        bar.getStyleClass().add("store-creation-bar");
        return bar;
    }
}
