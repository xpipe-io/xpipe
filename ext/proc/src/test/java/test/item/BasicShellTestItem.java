package test.item;

import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.ext.proc.ShellCommandStore;
import io.xpipe.extension.test.TestModule;

import java.util.Map;
import java.util.function.Supplier;

public class BasicShellTestItem extends TestModule<ShellProcessControl> {

    @Override
    protected void init(Map<String, Supplier<ShellProcessControl>> list) {
        list.put("local", () -> new LocalStore().create());

        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            list.put("local pwsh", () -> ShellCommandStore.shell(new LocalStore(), ShellTypes.POWERSHELL)
                    .create());
            list.put("local pwsh cmd", () -> ShellCommandStore.shell(
                            ShellCommandStore.shell(
                                    ShellCommandStore.shell(new LocalStore(), ShellTypes.POWERSHELL), ShellTypes.CMD),
                            ShellTypes.CMD)
                    .create());
        }
    }

    @Override
    protected Class<ShellProcessControl> getValueClass() {
        return ShellProcessControl.class;
    }
}
