package io.xpipe.app.action;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.comp.store.StoreChoiceComp;
import io.xpipe.app.comp.store.StoreListChoiceComp;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.*;
import io.xpipe.core.store.DataStore;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class ActionConfigComp extends SimpleComp {

    private final Property<AbstractAction> action;

    public ActionConfigComp(Property<AbstractAction> action) {this.action = action;}

    @Override
    protected Region createSimple() {
        var options = new OptionsBuilder();
        options.nameAndDescription("actionStore")
                .addComp(createChooser())
                .nameAndDescription("actionStores")
                .addComp(createMultiChooser());
        options.nameAndDescription("actionConfiguration")
                .addComp(createTextArea());
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

        var choice = new StoreListChoiceComp<>(listProp, DataStore.class, null, StoreViewState.get().getAllConnectionsCategory());
        choice.hide(listProp.emptyProperty());
        return choice;
    }

    @SuppressWarnings("unchecked")
    private Comp<?> createChooser() {
        var singleProp = new SimpleObjectProperty<DataStoreEntryRef<DataStore>>();
        var s = action instanceof StoreAction<?> sa ? sa.getRef() : null;
        singleProp.set((DataStoreEntryRef<DataStore>) s);

        var choice = new StoreChoiceComp<>(StoreChoiceComp.Mode.OTHER, null, singleProp, singleProp.get() != null ?
                (Class<DataStore>) singleProp.getValue().getStore().getClass() : DataStore.class, ref -> true,
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
