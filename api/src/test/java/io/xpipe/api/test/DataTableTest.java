package io.xpipe.api.test;

import io.xpipe.api.DataTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({XPipeConfig.class})
public class DataTableTest {

    @Test
    public void testGet() {
        var table = DataTable.get("new folder:username");
        var r = table.read(2);
        var a = 0;
    }
}
