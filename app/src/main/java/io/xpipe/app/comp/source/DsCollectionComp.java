package io.xpipe.app.comp.source;

import io.xpipe.core.source.CollectionReadConnection;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.ArrayList;

public class DsCollectionComp extends Comp<CompStructure<TreeView<String>>> {

    private final ObservableValue<CollectionReadConnection> con;

    private final ObservableValue<String> value;

    public DsCollectionComp(ObservableValue<CollectionReadConnection> con) {
        this.con = con;
        this.value = new SimpleObjectProperty<>("/");
    }

    private TreeItem<String> createTree() {
        var c = new ArrayList<TreeItem<String>>();

        if (con.getValue() != null) {
            try {
                con.getValue().listEntries().forEach(e -> {
                    var item = new TreeItem<String>(e.getFileName());
                    c.add(item);
                });
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).handle();
            }
        }

        var ar = new TreeItem<String>(value.getValue());
        ar.getChildren().setAll(c);
        return ar;
    }

    private void setupListener(TreeView<String> tv) {
        ChangeListener<CollectionReadConnection> listener = (c, o, n) -> {
            var nt = createTree();
            tv.setRoot(nt);
        };
        con.addListener(listener);
        listener.changed(con, null, con.getValue());
    }

    @Override
    public CompStructure<TreeView<String>> createBase() {
        var table = new TreeView<String>();
        setupListener(table);
        return new SimpleCompStructure<>(table);
    }
}
