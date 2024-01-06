package io.xpipe.app.comp.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.util.ScanAlert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.kordamp.ikonli.javafx.FontIcon;

public class StoreCreationMenu {

    public static void addButtons(MenuButton menu) {
        var automatically = new MenuItem();
        automatically.setGraphic(new FontIcon("mdi2e-eye-plus-outline"));
        automatically.textProperty().bind(AppI18n.observable("addAutomatically"));
        automatically.setOnAction(event -> {
            ScanAlert.showAsync(null);
            event.consume();
        });
        menu.getItems().add(automatically);
        menu.getItems().add(new SeparatorMenuItem());

        menu.getItems().add(category("addHost", "mdi2h-home-plus",
                DataStoreProvider.CreationCategory.HOST, "ssh"));

        menu.getItems().add(category("addShell", "mdi2t-text-box-multiple",
                DataStoreProvider.CreationCategory.SHELL, null));

        menu.getItems().add(category("addScript", "mdi2s-script-text-outline",
                DataStoreProvider.CreationCategory.SCRIPT, "script"));

        menu.getItems().add(category("addCommand", "mdi2c-code-greater-than",
                DataStoreProvider.CreationCategory.COMMAND, "cmd"));

        menu.getItems().add(category("addTunnel", "mdi2v-vector-polyline-plus",
                DataStoreProvider.CreationCategory.TUNNEL, null));

        menu.getItems().add(category("addDatabase", "mdi2d-database-plus",
                DataStoreProvider.CreationCategory.DATABASE, null));
    }

    private static MenuItem category(String name, String graphic, DataStoreProvider.CreationCategory category, String defaultProvider) {
        var sub = DataStoreProviders.getAll().stream().filter(dataStoreProvider -> category.equals(dataStoreProvider.getCreationCategory())).toList();
        if (sub.size() < 2) {
            var item = new MenuItem();
            item.setGraphic(new FontIcon(graphic));
            item.textProperty().bind(AppI18n.observable(name));
            item.setOnAction(event -> {
                GuiDsStoreCreator.showCreation(defaultProvider != null ? DataStoreProviders.byName(defaultProvider).orElseThrow() : null,
                        v -> category.equals(v.getCreationCategory()));
                event.consume();
            });
            return item;
        }

        var menu = new Menu();
        menu.setGraphic(new FontIcon(graphic));
        menu.textProperty().bind(AppI18n.observable(name));
        menu.setOnAction(event -> {
            if (event.getTarget() != menu) {
                return;
            }

            GuiDsStoreCreator.showCreation(defaultProvider != null ? DataStoreProviders.byName(defaultProvider).orElseThrow() : null,
                    v -> category.equals(v.getCreationCategory()));
            event.consume();
        });
        sub.forEach(dataStoreProvider -> {
            var item = new MenuItem(dataStoreProvider.getDisplayName());
            item.setGraphic(PrettyImageHelper.ofFixedSmallSquare(dataStoreProvider.getDisplayIconFileName(null)).createRegion());
            item.setOnAction(event -> {
                GuiDsStoreCreator.showCreation(dataStoreProvider,
                        v -> category.equals(v.getCreationCategory()));
                event.consume();
            });
            menu.getItems().add(item);
        });
        return menu;
    }
}
