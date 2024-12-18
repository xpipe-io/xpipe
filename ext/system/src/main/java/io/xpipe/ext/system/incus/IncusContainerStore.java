package io.xpipe.ext.system.incus;

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
import lombok.Getter;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.OptionalInt;

@JsonTypeName("incusContainer")
@SuperBuilder
@Jacksonized
@Getter
@AllArgsConstructor
@Value
public class IncusContainerStore implements ShellStore, FixedChildStore, StatefulDataStore<ContainerStoreState> {

    private final DataStoreEntryRef<IncusInstallStore> install;
    private final String containerName;

    @Override
    public Class<ContainerStoreState> getStateClass() {
        return ContainerStoreState.class;
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(install);
        Validators.isType(install, IncusInstallStore.class);
        install.checkComplete();
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
                return getInstall().getStore().getHost().getStore();
            }

            @Override
            public ShellControl control(ShellControl parent) {
                var base = new IncusCommandView(parent).exec(containerName);
                return base.withSourceStore(IncusContainerStore.this)
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
