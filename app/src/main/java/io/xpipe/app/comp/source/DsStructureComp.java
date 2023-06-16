package io.xpipe.app.comp.source;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.core.data.node.DataStructureNode;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class DsStructureComp extends Comp<CompStructure<TreeView<String>>> {

    private final ObservableValue<DataStructureNode> value;

    public DsStructureComp(ObservableValue<DataStructureNode> value) {
        this.value = value;
    }

    private TreeItem<String> createTree(DataStructureNode n, AtomicInteger counter, int max) {
        if (n.isArray()) {
            var c = new ArrayList<TreeItem<String>>();
            for (int i = 0; i < Math.min(n.size(), max - counter.get()); i++) {
                var item = createTree(n.at(i), counter, max);
                item.setValue("[" + i + "] = " + item.getValue());
                c.add(item);
            }
            var ar = new TreeItem<>("[" + n.size() + "... ]");
            ar.getChildren().setAll(c);
            return ar;
        } else if (n.isTuple()) {
            var c = new ArrayList<TreeItem<String>>();
            for (int i = 0; i < Math.min(n.size(), max - counter.get()); i++) {
                var item = createTree(n.at(i), counter, max);
                var key = n.asTuple().getKeyNames().get(i);
                item.setValue((key != null ? key : "" + i) + " = " + item.getValue());
                c.add(item);
            }
            var ar = new TreeItem<>("( " + n.size() + "... )");
            ar.getChildren().setAll(c);
            return ar;
        } else {
            var ar = new TreeItem<>(n.asValue().asString());
            return ar;
        }
    }

    private void setupListener(TreeView<String> tv) {
        ChangeListener<DataStructureNode> listener = (c, o, n) -> {
            var nt = createTree(n, new AtomicInteger(0), 100);
            tv.setRoot(nt);
        };
        value.addListener(listener);
        listener.changed(value, null, value.getValue());
    }

    @Override
    public CompStructure<TreeView<String>> createBase() {
        var table = new TreeView<String>();
        setupListener(table);
        return new SimpleCompStructure<>(table);
    }
}
