package io.xpipe.app.action;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.hub.action.BatchStoreAction;
import io.xpipe.app.hub.action.MultiStoreAction;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreListChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.*;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.layout.Region;

public class ActionConfigComp extends SimpleComp {

    private final Property<AbstractAction> action;

    public ActionConfigComp(Property<AbstractAction> action) {
        this.action = action;
    }

    @Override
    protected Region createSimple() {
        var options = new OptionsBuilder();
        options.nameAndDescription("actionStore")
                .addComp(createChooser())
                .nameAndDescription("actionStores")
                .addComp(createMultiChooser());
        options.nameAndDescription("actionConfiguration").addComp(createTextArea());
        return options.build();
    }

    @SuppressWarnings("unchecked")
    private Comp<?> createMultiChooser() {
        var listProp = new SimpleListProperty<DataStoreEntryRef<DataStore>>(FXCollections.observableArrayList());
        if (action.getValue() instanceof BatchStoreAction<?> ba) {
            listProp.setAll(((BatchStoreAction<DataStore>) ba).getRefs());
        } else if (action.getValue() instanceof MultiStoreAction<?> ma) {
            listProp.setAll(((MultiStoreAction<DataStore>) ma).getRefs());
        } else {
            listProp.clear();
        }

        listProp.addListener((obs, o, n) -> {
            if (action.getValue() instanceof BatchStoreAction<?> ba) {
                action.setValue(((BatchStoreAction<DataStore>) ba).withRefs(n));
            } else if (action.getValue() instanceof MultiStoreAction<?> ma) {
                action.setValue(((MultiStoreAction<DataStore>) ma).withRefs(n));
            }
        });

        var choice = new StoreListChoiceComp<>(
                listProp, DataStore.class, null, StoreViewState.get().getAllConnectionsCategory());
        choice.hide(listProp.emptyProperty());
        choice.maxHeight(450);
        return choice;
    }

    @SuppressWarnings("unchecked")
    private Comp<?> createChooser() {
        var singleProp = new SimpleObjectProperty<DataStoreEntryRef<DataStore>>();
        var s = action.getValue() instanceof StoreAction<?> sa ? sa.getRef() : null;
        singleProp.set((DataStoreEntryRef<DataStore>) s);

        singleProp.addListener((obs, o, n) -> {
            if (action.getValue() instanceof StoreAction<?> sa) {
                action.setValue(sa.withRef(n.asNeeded()));
            }
        });

        var choice = new StoreChoiceComp<>(
                StoreChoiceComp.Mode.OTHER,
                null,
                singleProp,
                DataStore.class,
                ref -> true,
                StoreViewState.get().getAllConnectionsCategory());
        choice.hide(singleProp.isNull());
        return choice;
    }

    private Comp<?> createTextArea() {
        var config = new SimpleStringProperty();
        var s = action.getValue() instanceof SerializableAction sa ? sa.toConfigNode() : null;
        config.set(s != null && s.size() > 0 ? s.toPrettyString() : null);

        config.addListener((obs, o, n) -> {
            if (action.getValue() instanceof SerializableAction aa && n != null) {
                var with = aa.withConfigString(n);
                if (with.isPresent()) {
                    action.setValue(with.get());
                }
            }
        });

        var area = new IntegratedTextAreaComp(config, false, "action", new SimpleStringProperty("json"));
        area.hide(config.isNull());
        return area;
    }
}
