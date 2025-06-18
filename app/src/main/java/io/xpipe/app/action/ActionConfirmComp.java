package io.xpipe.app.action;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.hub.action.BatchStoreAction;
import io.xpipe.app.hub.action.MultiStoreAction;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.comp.StoreListChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.store.DataStore;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;

import java.util.List;
import java.util.Map;

public class ActionConfirmComp extends SimpleComp {

    private final AbstractAction action;

    public ActionConfirmComp(AbstractAction action) {
        this.action = action;
    }

    @Override
    protected Region createSimple() {
        var options = new OptionsBuilder();
        var plural = action instanceof BatchStoreAction<?> || action instanceof MultiStoreAction<?>;
        options.nameAndDescription(plural ? "actionConnections" : "actionConnection")
                .addComp(createList());
        options.nameAndDescription("actionConfiguration").addComp(createTable());
        return options.build();
    }

    @SuppressWarnings("unchecked")
    private Comp<?> createList() {
        var listProp = new SimpleListProperty<DataStoreEntryRef<DataStore>>(FXCollections.observableArrayList());
        if (action instanceof BatchStoreAction<?> ba) {
            listProp.setAll(((BatchStoreAction<DataStore>) ba).getRefs());
        } else if (action instanceof MultiStoreAction<?> ma) {
            listProp.setAll(((MultiStoreAction<DataStore>) ma).getRefs());
        } else if (action instanceof StoreAction<?> sa) {
            listProp.setAll(List.of(sa.getRef().asNeeded()));
        }

        var choice = new StoreListChoiceComp<>(
                listProp, DataStore.class, null, StoreViewState.get().getAllConnectionsCategory());
        choice.setEditable(false);
        choice.hide(listProp.emptyProperty());
        return choice;
    }

    private Comp<?> createTable() {
        var map = action.toDisplayMap();
        return Comp.of(() -> {
            var grid = new GridPane();
            grid.setHgap(11);
            grid.setVgap(2);
            var row = 0;
            for (Map.Entry<String, String> e : map.entrySet()) {
                var name = new Label(e.getKey());
                var value = new Label(e.getValue());
                value.getStyleClass().add(Styles.TEXT_BOLD);
                grid.add(name, 0, row);
                grid.add(value, 1, row);
                row++;
            }
            return grid;
        });
    }
}
