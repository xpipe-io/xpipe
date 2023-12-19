package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellStoreState;
import io.xpipe.core.util.JacksonizedValue;

@JsonTypeName("local")
public class LocalStore extends JacksonizedValue implements ShellStore, StatefulDataStore<ShellStoreState> {

    @Override
    public Class<ShellStoreState> getStateClass() {
        return ShellStoreState.class;
    }

    @Override
    public ShellControl control() {
        var pc = ProcessControlProvider.get().createLocalProcessControl(true);
        pc.withShellStateInit(this);
        pc.withShellStateFail(this);
        return pc;
    }
}
