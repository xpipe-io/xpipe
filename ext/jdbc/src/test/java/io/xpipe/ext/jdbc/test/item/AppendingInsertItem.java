package io.xpipe.ext.jdbc.test.item;

import io.xpipe.core.data.node.TupleNode;
import io.xpipe.extension.test.TestModule;
import lombok.Value;
import org.junit.jupiter.api.Named;

import java.util.stream.Stream;

@Value
public class AppendingInsertItem {

    public static Stream<Named<AppendingInsertItem>> getAll() {
        return TestModule.getArguments(AppendingInsertItem.class, "io.xpipe.ext.jdbc.test.item.PrivateAppendingInsertItems");
    }

    DatabaseItem.TableItem tableItem;
    TupleNode insert;

    AppendingInsertItem(DatabaseItem.TableItem tableItem, TupleNode insert) {
        this.tableItem = tableItem;
        this.insert = insert;
    }

    @Override
    public String toString() {
        return tableItem.toString();
    }
}
