package test.item;

import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.extension.test.TestModule;
import org.junit.jupiter.api.Named;

import java.util.stream.Stream;

public class ShellTestItem {

    public static Stream<Named<ShellProcessControl>> getAll() {
        return TestModule.getArguments(ShellProcessControl.class, "test.item.BasicShellTestItem", "test.item.PrivateShellTestItem");
    }
}
