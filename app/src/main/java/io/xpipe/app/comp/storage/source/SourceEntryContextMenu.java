package io.xpipe.app.comp.storage.source;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataSourceEntry;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DesktopHelper;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

public class SourceEntryContextMenu<S extends CompStructure<?>> extends ContextMenuAugment<S> {


    public SourceEntryContextMenu(boolean showOnPrimaryButton, SourceEntryWrapper entry, Region renameTextField) {
        super(() -> createContextMenu(entry, renameTextField));
    }

    protected static ContextMenu createContextMenu(SourceEntryWrapper entry, Region renameTextField) {
        var cm = new ContextMenu();
        AppFont.normal(cm.getStyleableNode());

        for (var actionProvider : entry.getActionProviders()) {
            var c = actionProvider.getDataSourceCallSite();
            var name = c.getName(entry.getEntry().getSource().asNeeded());
            var icon = c.getIcon(entry.getEntry().getSource().asNeeded());
            var item = new MenuItem(null, new FontIcon(icon));
            item.setOnAction(event -> {
                event.consume();
                try {
                    var action = c.createAction(entry.getEntry().getSource().asNeeded());
                    action.execute();
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).handle();
                }
            });
            item.textProperty().bind(name);
            // item.setDisable(!entry.getState().getValue().isUsable());
            cm.getItems().add(item);

            // actionProvider.applyToRegion(entry.getEntry().getStore().asNeeded(), region);
        }

        if (entry.getActionProviders().size() > 0) {
            cm.getItems().add(new SeparatorMenuItem());
        }

        var properties = new MenuItem(AppI18n.get("properties"), new FontIcon("mdi2a-application-cog"));
        properties.setOnAction(e -> {});
        //  cm.getItems().add(properties);

        var rename = new MenuItem(AppI18n.get("rename"), new FontIcon("mdi2r-rename-box"));
        rename.setOnAction(e -> {
            renameTextField.requestFocus();
        });
        cm.getItems().add(rename);

        var validate = new MenuItem(AppI18n.get("refresh"), new FontIcon("mdal-360"));
        validate.setOnAction(event -> {
            DataStorage.get().refreshAsync(entry.getEntry(), true);
        });
        cm.getItems().add(validate);

        var edit = new MenuItem(AppI18n.get("edit"), new FontIcon("mdal-edit"));
        edit.setOnAction(event -> entry.editDialog());
        edit.disableProperty().bind(Bindings.equal(DataSourceEntry.State.LOAD_FAILED, entry.getState()));
        cm.getItems().add(edit);

        var del = new MenuItem(AppI18n.get("delete"), new FontIcon("mdal-delete_outline"));
        del.setOnAction(e -> {
            entry.delete();
        });
        cm.getItems().add(del);

        if (AppPrefs.get().developerMode().getValue()) {
            cm.getItems().add(new SeparatorMenuItem());

            var openDir = new MenuItem(AppI18n.get("browseInternal"), new FontIcon("mdi2f-folder-open-outline"));
            openDir.setOnAction(e -> {
                DesktopHelper.browsePath(entry.getEntry().getDirectory());
            });
            cm.getItems().add(openDir);
        }

        return cm;
    }
}
