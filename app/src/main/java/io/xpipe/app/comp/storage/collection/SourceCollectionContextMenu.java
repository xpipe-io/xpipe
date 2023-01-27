package io.xpipe.app.comp.storage.collection;

import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.augment.PopupMenuAugment;
import io.xpipe.extension.util.OsHelper;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

public class SourceCollectionContextMenu<S extends CompStructure<?>> extends PopupMenuAugment<S> {

    private final SourceCollectionWrapper group;
    private final Region renameTextField;

    public SourceCollectionContextMenu(
            boolean showOnPrimaryButton, SourceCollectionWrapper group, Region renameTextField) {
        super(showOnPrimaryButton);
        this.group = group;
        this.renameTextField = renameTextField;
    }

    private void onDelete() {
        if (group.getEntries().size() > 0) {
            AppWindowHelper.showBlockingAlert(alert -> {
                        alert.setTitle(I18n.get("confirmCollectionDeletionTitle"));
                        alert.setHeaderText(I18n.get("confirmCollectionDeletionHeader", group.getName()));
                        alert.setContentText(I18n.get(
                                "confirmCollectionDeletionContent",
                                group.getEntries().size()));
                        alert.setAlertType(Alert.AlertType.CONFIRMATION);
                    })
                    .filter(b -> b.getButtonData().isDefaultButton())
                    .ifPresent(t -> {
                        group.delete();
                    });
        } else {
            group.delete();
        }
    }

    private void onClean() {
        if (group.getEntries().size() > 0) {
            AppWindowHelper.showBlockingAlert(alert -> {
                        alert.setTitle(I18n.get("confirmCollectionDeletionTitle"));
                        alert.setHeaderText(I18n.get("confirmCollectionDeletionHeader", group.getName()));
                        alert.setContentText(I18n.get(
                                "confirmCollectionDeletionContent",
                                group.getEntries().size()));
                        alert.setAlertType(Alert.AlertType.CONFIRMATION);
                    })
                    .filter(b -> b.getButtonData().isDefaultButton())
                    .ifPresent(t -> {
                        group.clean();
                    });
        } else {
            group.clean();
        }
    }

    @Override
    protected ContextMenu createContextMenu() {
        var cm = new ContextMenu();
        var name = new MenuItem(group.getName());
        name.setDisable(true);
        name.getStyleClass().add("header-menu-item");
        cm.getItems().add(name);
        cm.getItems().add(new SeparatorMenuItem());
        {
            var properties = new MenuItem(I18n.get("properties"), new FontIcon("mdi2a-application-cog"));
            properties.setOnAction(e -> {});

            //  cm.getItems().add(properties);
        }
        if (group.isRenameable()) {
            var rename = new MenuItem(I18n.get("rename"), new FontIcon("mdal-edit"));
            rename.setOnAction(e -> {
                renameTextField.requestFocus();
            });
            cm.getItems().add(rename);
        }

        if (AppPrefs.get().developerMode().getValue()) {
            var openDir = new MenuItem(I18n.get("openDir"), new FontIcon("mdal-edit"));
            openDir.setOnAction(e -> {
                OsHelper.browseFileInDirectory(group.getCollection().getDirectory());
            });
            cm.getItems().add(openDir);
        }

        if (group.isDeleteable()) {
            var del = new MenuItem(I18n.get("delete"), new FontIcon("mdal-delete_outline"));
            del.setOnAction(e -> {
                onDelete();
            });
            cm.getItems().add(del);
        } else {
            var del = new MenuItem(I18n.get("clean"), new FontIcon("mdal-delete_outline"));
            del.setOnAction(e -> {
                onClean();
            });
            cm.getItems().add(del);
        }
        return cm;
    }
}
