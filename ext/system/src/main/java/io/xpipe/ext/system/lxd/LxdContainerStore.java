package io.xpipe.ext.system.lxd;

import io.xpipe.app.ext.ContainerStoreState;
import io.xpipe.app.ext.ShellControlFunction;
import io.xpipe.app.ext.ShellControlParentStoreFunction;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LicenseRequiredException;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FixedChildStore;
import io.xpipe.core.store.StatefulDataStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.OptionalInt;

@JsonTypeName("lxd")
@SuperBuilder
@Jacksonized
@Value
@AllArgsConstructor
public class LxdContainerStore implements ShellStore, FixedChildStore, StatefulDataStore<ContainerStoreState> {

    private final DataStoreEntryRef<LxdCmdStore> cmd;
    private final String containerName;

    @Override
    public Class<ContainerStoreState> getStateClass() {
        return ContainerStoreState.class;
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(cmd);
        Validators.isType(cmd, LxdCmdStore.class);
        cmd.checkComplete();
        Validators.nonNull(containerName);
    }

    @Override
    public OptionalInt getFixedId() {
        return OptionalInt.of(Objects.hash(containerName));
    }

    @Override
    public ShellControlFunction shellFunction() {
        return new ShellControlParentStoreFunction() {

            @Override
            public ShellStore getParentStore() {
                return getCmd().getStore().getHost().getStore();
            }

            @Override
            public ShellControl control(ShellControl parent) {
                var base = new LxdCommandView(parent).exec(containerName);
                return base.withSourceStore(LxdContainerStore.this)
                        .onInit(shellControl -> {
                            var s = getState().toBuilder()
                                    .osType(shellControl.getOsType())
                                    .shellDialect(shellControl.getShellDialect())
                                    .ttyState(shellControl.getTtyState())
                                    .running(true)
                                    .osName(shellControl.getOsName())
                                    .build();
                            setState(s);
                        })
                        .onStartupFail(throwable -> {
                            if (throwable instanceof LicenseRequiredException) {
                                return;
                            }

                            var s = getState().toBuilder()
                                    .running(false)
                                    .containerState("Connection failed")
                                    .build();
                            setState(s);
                        });
            }
        };
    }
}
