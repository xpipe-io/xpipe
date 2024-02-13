package test;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.test.LocalExtensionTest;

public class Test extends LocalExtensionTest {

    @org.junit.jupiter.api.Test
    public void test() {
        System.out.println("a");
        System.out.println(DataStorage.get().getStoreEntries());

    }
}
