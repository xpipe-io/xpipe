package io.xpipe.app.comp.source;

import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.type.DataTypeVisitors;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import io.xpipe.extension.fxcomps.util.SimpleChangeListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.Stack;

public class DsTableComp extends Comp<CompStructure<TableView<DsTableComp.RowWrapper>>> {

    private final ObservableValue<ArrayNode> value;

    public DsTableComp(ObservableValue<ArrayNode> value) {
        this.value = value;
    }

    private TupleType determineDataType(ArrayNode table) {
        if (table == null || table.size() == 0) {
            return TupleType.empty();
        }

        var first = table.at(0);
        return (TupleType) first.determineDataType();
    }

    private void setupListener(TableView<RowWrapper> table) {
        SimpleChangeListener.apply(PlatformThread.sync(value), n -> {
            table.getItems().clear();
            table.getColumns().clear();

            var t = determineDataType(n);
            var stack = new Stack<ObservableList<TableColumn<RowWrapper, ?>>>();
            stack.push(table.getColumns());

            t.visit(DataTypeVisitors.table(
                    tupleName -> {
                        var current = stack.peek();
                        stack.push(current.get(current.size() - 1).getColumns());
                    },
                    stack::pop,
                    (name, pointer) -> {
                        TableColumn<RowWrapper, String> col = new TableColumn<>(name);
                        col.setCellValueFactory(cellData -> {
                            var node = pointer.get(n.at(cellData.getValue().rowIndex()));
                            return new SimpleStringProperty(nodeToString(node));
                        });
                        var current = stack.peek();
                        current.add(col);
                    }));

            var list = new ArrayList<RowWrapper>(n.size());
            for (int i = 0; i < n.size(); i++) {
                list.add(new RowWrapper(n, i));
            }
            table.setItems(FXCollections.observableList(list));
        });
    }

    private String nodeToString(DataStructureNode node) {
        if (node.isValue()) {
            return node.asString();
        }
        if (node.isArray()) {
            return "[...]";
        }
        if (node.isTuple()) {
            return "{...}";
        }
        return null;
    }

    @Override
    public CompStructure<TableView<RowWrapper>> createBase() {
        var table = new TableView<RowWrapper>();
        setupListener(table);
        return new SimpleCompStructure<>(table);
    }

    public record RowWrapper(ArrayNode table, int rowIndex) {}
}
