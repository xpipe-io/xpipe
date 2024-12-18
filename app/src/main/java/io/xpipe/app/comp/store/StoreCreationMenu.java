package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.util.ScanAlert;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Comparator;

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

        menu.getItems().add(category("addHost", "mdi2h-home-plus", DataStoreCreationCategory.HOST, "ssh"));

        menu.getItems().add(category("addDesktop", "mdi2c-camera-plus", DataStoreCreationCategory.DESKTOP, null));

        menu.getItems()
                .add(category("addScript", "mdi2s-script-text-outline", DataStoreCreationCategory.SCRIPT, "script"));

        menu.getItems().add(category("addCommand", "mdi2c-code-greater-than", DataStoreCreationCategory.COMMAND, null));

        menu.getItems()
                .add(category("addService", "mdi2l-link-plus", DataStoreCreationCategory.SERVICE, "customService"));

        menu.getItems()
                .add(category(
                        "addTunnel", "mdi2v-vector-polyline-plus", DataStoreCreationCategory.TUNNEL, "sshLocalTunnel"));

        menu.getItems().add(category("addSerial", "mdi2s-serial-port", DataStoreCreationCategory.SERIAL, "serial"));

        menu.getItems()
                .add(category(
                        "addIdentity",
                        "mdi2a-account-multiple-plus",
                        DataStoreCreationCategory.IDENTITY,
                        "localIdentity"));

        // menu.getItems().add(category("addDatabase", "mdi2d-database-plus", DataStoreCreationCategory.DATABASE,
        // null));
    }

    private static MenuItem category(
            String name, String graphic, DataStoreCreationCategory category, String defaultProvider) {
        var sub = DataStoreProviders.getAll().stream()
                .filter(dataStoreProvider -> category.equals(dataStoreProvider.getCreationCategory()))
                .toList();
        if (sub.size() < 2) {
            var item = new MenuItem();
            item.setGraphic(new FontIcon(graphic));
            item.textProperty().bind(AppI18n.observable(name));
            item.setOnAction(event -> {
                StoreCreationComp.showCreation(
                        defaultProvider != null
                                ? DataStoreProviders.byName(defaultProvider).orElseThrow()
                                : null,
                        category);
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

            StoreCreationComp.showCreation(
                    defaultProvider != null
                            ? DataStoreProviders.byName(defaultProvider).orElseThrow()
                            : null,
                    category);
            event.consume();
        });

        var providers = sub.stream()
                .sorted(Comparator.comparingInt(dataStoreProvider -> dataStoreProvider.getOrderPriority()))
                .toList();
        int lastOrder = providers.getFirst().getOrderPriority();
        for (io.xpipe.app.ext.DataStoreProvider dataStoreProvider : providers) {
            if (dataStoreProvider.getOrderPriority() != lastOrder) {
                menu.getItems().add(new SeparatorMenuItem());
                lastOrder = dataStoreProvider.getOrderPriority();
            }

            var item = new MenuItem();
            item.textProperty().bind(dataStoreProvider.displayName());
            item.setGraphic(PrettyImageHelper.ofFixedSizeSquare(dataStoreProvider.getDisplayIconFileName(null), 16)
                    .createRegion());
            item.setOnAction(event -> {
                StoreCreationComp.showCreation(dataStoreProvider, category);
                event.consume();
            });
            menu.getItems().add(item);
        }
        return menu;
    }
}
