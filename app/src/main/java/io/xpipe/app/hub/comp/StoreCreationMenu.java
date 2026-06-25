package io.xpipe.app.hub.comp;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.browser.BrowserFileChooserSessionComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.*;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.MenuHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ScanDialog;

import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;
import java.util.Comparator;

public class StoreCreationMenu {

    public static void addButtons(ObservableList<MenuItem> items, boolean allowSearch) {
        if (allowSearch) {
            var automatically = new MenuItem();
            automatically.setGraphic(new FontIcon("mdi2e-eye-plus-outline"));
            automatically.textProperty().bind(AppI18n.observable("addAutomatically"));
            automatically.setOnAction(event -> {
                ScanDialog.showSingleAsync(null);
                event.consume();
            });
            items.add(automatically);
            items.add(networkScanMenu());
            items.add(new SeparatorMenuItem());

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

        items.add(categoryMenu("addHost", "mdi2h-home-plus", DataStoreCreationCategory.HOST));

        items.add(categoryMenu("addDesktop", "mdi2c-camera-plus", DataStoreCreationCategory.DESKTOP));

        items.add(cloudMenu());

        items.add(new SeparatorMenuItem());

        items.add(categoryMenu("addIdentity", "mdi2a-account-multiple-plus", DataStoreCreationCategory.IDENTITY));

        items.add(new SeparatorMenuItem());

        items.add(categoryMenu("addService", "mdi2l-link-plus", DataStoreCreationCategory.SERVICE));

        items.add(categoryMenu("addTunnel", "mdi2v-vector-polyline-plus", DataStoreCreationCategory.TUNNEL));

        items.add(new SeparatorMenuItem());

        items.add(categoryMenu("addCommand", "mdi2c-code-greater-than", DataStoreCreationCategory.COMMAND));

        items.add(categoryMenu(
                "addScript",
                "mdi2s-script-text-outline",
                DataStoreCreationCategory.SCRIPT,
                DataStoreCreationCategory.SCRIPT_SOURCE));

        items.add(new SeparatorMenuItem());

        var actionMenu = categoryMenu("addMacro", "mdmz-miscellaneous_services", DataStoreCreationCategory.MACRO);
        var item = new MenuItem();
        item.setGraphic(PrettyImageHelper.ofFixedSize("action.png", 16, 16).build());
        item.textProperty().bind(AppI18n.observable("actionShortcut"));
        item.setOnAction(event -> {
            Platform.runLater(() -> {
                AbstractAction.expectPick();
            });

            // Fix weird JavaFX NPE
            actionMenu.getParentPopup().hide();
        });
        actionMenu.getItems().addFirst(item);

        items.add(categoryMenu(
                "addOther",
                "mdi2f-folder-plus-outline",
                DataStoreCreationCategory.NETWORK,
                DataStoreCreationCategory.CLUSTER,
                DataStoreCreationCategory.FILE_SYSTEM,
                DataStoreCreationCategory.SERIAL));

        items.add(new SeparatorMenuItem());

        items.add(actionMenu);
    }

    private static Menu categoryMenu(String name, String graphic, DataStoreCreationCategory... categories) {
        var providers = DataStoreProviders.getAll().stream()
                .filter(dataStoreProvider ->
                        Arrays.asList(categories).contains(dataStoreProvider.getCreationCategory()))
                .sorted(Comparator.<DataStoreProvider>comparingInt(
                                p -> Arrays.asList(categories).indexOf(p.getCreationCategory()))
                        .thenComparingInt(dataStoreProvider -> dataStoreProvider.getOrderPriority()))
                .toList();

        var menu = new Menu();
        menu.setGraphic(new FontIcon(graphic));
        menu.textProperty().bind(AppI18n.observable(name));

        if (providers.isEmpty()) {
            return menu;
        }

        menu.setOnAction(event -> {
            if (event.getTarget() != menu) {
                return;
            }

            Platform.runLater(() -> {
                if (categories.length == 1 && categories[0].getDefaultProvider() != null) {
                    providers.stream()
                            .filter(dataStoreProvider ->
                                    dataStoreProvider.getId().equals(categories[0].getDefaultProvider()))
                            .findFirst()
                            .ifPresent(dataStoreProvider -> {
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

        int lastOrder = providers.getFirst().getOrderPriority();
        DataStoreCreationCategory lastCategory = providers.getFirst().getCreationCategory();
        for (var dataStoreProvider : providers) {
            if (dataStoreProvider.getOrderPriority() != lastOrder
                    || dataStoreProvider.getCreationCategory() != lastCategory) {
                menu.getItems().add(new SeparatorMenuItem());
                lastOrder = dataStoreProvider.getOrderPriority();
            }

            var item = new MenuItem();
            item.textProperty().bind(dataStoreProvider.displayName());
            item.setGraphic(PrettyImageHelper.ofFixedSizeSquare(dataStoreProvider.getDisplayIconFileName(null), 16)
                    .build());
            item.setOnAction(event -> {
                StoreCreationDialog.showCreation(dataStoreProvider, dataStoreProvider.getCreationCategory());
                event.consume();
            });
            item.setDisable(!dataStoreProvider.allowCreation());
            menu.getItems().add(item);
        }

        if (categories[0].equals(DataStoreCreationCategory.DESKTOP)) {
            var rdpFile = MenuHelper.createMenuItem(new LabelGraphic.ImageGraphic("rdpFile_icon.svg", 16), "rdpFile");
            rdpFile.setOnAction(event -> {
                BrowserFileChooserSessionComp.open(() -> DataStorage.get().local().ref(), () -> null, fileReference -> {
                    var file = fileReference.getPath().asLocalPath();
                    ThreadHelper.runFailableAsync(() -> {
                        ProcessControlProvider.get().importRdpFile(file);
                    });
                }, false, false, entry -> entry.equals(DataStorage.get().local()), null);
                event.consume();
            });
            menu.getItems().add(menu.getItems().size() - 2, rdpFile);
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
