package io.xpipe.ext.jdbc.test.item;

import io.xpipe.core.data.node.TupleNode;
import io.xpipe.extension.test.TestModule;
import lombok.Value;
import org.junit.jupiter.api.Named;

import java.util.stream.Stream;

@Value
public class InsertItem {

    public static Stream<Named<InsertItem>> getAll() {
        return TestModule.getArguments(InsertItem.class, "io.xpipe.ext.jdbc.test.item.PrivateInsertItems");
    }

    public DatabaseItem.TableItem tableItem;
    public TupleNode insert;

    InsertItem(DatabaseItem.TableItem tableItem, TupleNode insert) {
        this.tableItem = tableItem;
        this.insert = insert;
    }
}
