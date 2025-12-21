package io.xpipe.app.hub.comp;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.*;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.ScanDialog;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Comparator;

public class StoreCreationMenu {

    public static void addButtons(MenuButton menu, boolean allowSearch) {
        if (allowSearch) {
            var automatically = new MenuItem();
            automatically.setGraphic(new FontIcon("mdi2e-eye-plus-outline"));
            automatically.textProperty().bind(AppI18n.observable("addAutomatically"));
            automatically.setOnAction(event -> {
                ScanDialog.showSingleAsync(null);
                event.consume();
            });
            menu.getItems().add(automatically);
            menu.getItems().add(networkScanMenu());
            menu.getItems().add(new SeparatorMenuItem());

            var disableSearch = Bindings.createBooleanBinding(
                    () -> {
                        var allCat = StoreViewState.get().getAllConnectionsCategory();
                        var connections = StoreViewState.get().getAllEntries().getList().stream()
                                .filter(wrapper -> allCat.equals(
                                        wrapper.getCategory().getValue().getRoot()))
                                .toList();
                        return 1 == connections.size()
                                && StoreViewState.get()
                                        .getActiveCategory()
                                        .getValue()
                                        .getRoot()
                                        .equals(allCat);
                    },
                    StoreViewState.get().getAllEntries().getList());
            automatically.disableProperty().bind(disableSearch);
        }

        menu.getItems().add(categoryMenu("addHost", "mdi2h-home-plus", DataStoreCreationCategory.HOST, "ssh"));

        menu.getItems().add(categoryMenu("addDesktop", "mdi2c-camera-plus", DataStoreCreationCategory.DESKTOP, null));

        menu.getItems()
                .add(categoryMenu(
                        "addIdentity",
                        "mdi2a-account-multiple-plus",
                        DataStoreCreationCategory.IDENTITY,
                        "localIdentity"));

        menu.getItems().add(cloudMenu());

        menu.getItems().add(new SeparatorMenuItem());

        menu.getItems()
                .add(categoryMenu("addService", "mdi2l-link-plus", DataStoreCreationCategory.SERVICE, "customService"));

        menu.getItems()
                .add(categoryMenu(
                        "addTunnel", "mdi2v-vector-polyline-plus", DataStoreCreationCategory.TUNNEL, "sshLocalTunnel"));

        menu.getItems()
                .add(categoryMenu(
                        "addFileSystem",
                        "mdi2f-folder-plus-outline",
                        DataStoreCreationCategory.FILE_SYSTEM,
                        "genericS3Bucket"));

        menu.getItems().add(new SeparatorMenuItem());

        menu.getItems()
                .add(categoryMenu("addCommand", "mdi2c-code-greater-than", DataStoreCreationCategory.COMMAND, null));

        menu.getItems()
                .add(categoryMenu(
                        "addScript", "mdi2s-script-text-outline", DataStoreCreationCategory.SCRIPT, "script"));

        menu.getItems().add(new SeparatorMenuItem());

        var actionMenu = categoryMenu("addMacro", "mdmz-miscellaneous_services", DataStoreCreationCategory.MACRO, null);
        var item = new MenuItem();
        item.setGraphic(PrettyImageHelper.ofFixedSize("app:shortcut/actionShortcut_icon.svg", 16, 16)
                .createRegion());
        item.textProperty().bind(AppI18n.observable("actionShortcut"));
        item.setOnAction(event -> {
            Platform.runLater(() -> {
                AbstractAction.expectPick();
            });

            // Fix weird JavaFX NPE
            actionMenu.getParentPopup().hide();
        });
        actionMenu.getItems().addFirst(item);

        menu.getItems().add(categoryMenu("addSerial", "mdi2s-serial-port", DataStoreCreationCategory.SERIAL, "serial"));

        menu.getItems().add(new SeparatorMenuItem());

        menu.getItems().add(actionMenu);
    }

    private static Menu categoryMenu(
            String name, String graphic, DataStoreCreationCategory category, String defaultProvider) {
        var providers = DataStoreProviders.getAll().stream()
                .filter(dataStoreProvider -> category.equals(dataStoreProvider.getCreationCategory()))
                .sorted(Comparator.comparingInt(dataStoreProvider -> dataStoreProvider.getOrderPriority()))
                .toList();

        var menu = new Menu();
        menu.setGraphic(new FontIcon(graphic));
        menu.textProperty().bind(AppI18n.observable(name));

        if (providers.isEmpty()) {
            return menu;
        }

        if (AppDistributionType.get() != AppDistributionType.ANDROID_LINUX_TERMINAL) {
            menu.setOnAction(event -> {
                if (event.getTarget() != menu) {
                    return;
                }

                Platform.runLater(() -> {
                    if (defaultProvider != null) {
                        providers.stream().filter(dataStoreProvider -> dataStoreProvider.getId().equals(defaultProvider)).findFirst().ifPresent(
                                dataStoreProvider -> {
                                    var index = providers.indexOf(dataStoreProvider);
                                    menu.getItems().get(index).fire();
                                });
                        return;
                    }

                    var onlyItem = menu.getItems().getFirst();
                    onlyItem.fire();
                });

                // Fix weird JavaFX NPE
                menu.getParentPopup().hide();
            });
        }

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
                StoreCreationDialog.showCreation(dataStoreProvider, category);
                event.consume();
            });
            menu.getItems().add(item);
        }
        return menu;
    }

    private static Menu cloudMenu() {
        var menu = new Menu();
        menu.setGraphic(new FontIcon("mdi2t-toy-brick-plus-outline"));
        menu.textProperty().bind(AppI18n.observable("addCloud"));

        for (var p : CloudSetupProvider.ALL) {
            var item = new MenuItem();
            item.textProperty().bind(AppI18n.observable(p.getNameKey()));
            item.setGraphic(p.getGraphic().createGraphicNode());
            item.setOnAction(event -> {
                var action =
                        SetupToolActionProvider.Action.builder().type(p.getId()).build();
                action.executeAsync();
                event.consume();
            });
            menu.getItems().add(item);
        }
        return menu;
    }

    private static MenuItem networkScanMenu() {
        var menu = new MenuItem();
        menu.setGraphic(new FontIcon("mdi2a-access-point-plus"));
        menu.textProperty().bind(AppI18n.observable("addNetwork"));
        menu.setOnAction(event -> {
            ProcessControlProvider.get().createNetworkScanModal().show();
            event.consume();
        });
        return menu;
    }
}
