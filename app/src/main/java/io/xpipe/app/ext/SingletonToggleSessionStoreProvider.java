package io.xpipe.app.ext;

import io.xpipe.app.comp.base.StoreToggleComp;
import io.xpipe.app.comp.base.SystemStateComp;
import io.xpipe.app.comp.store.StoreEntryComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.SingletonToggleSessionStore;
import io.xpipe.core.store.ToggleSessionState;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;

public interface SingletonToggleSessionStoreProvider extends DataStoreProvider {

    @Override
    public default StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        SingletonToggleSessionStore<?> s = sec.getWrapper().getEntry().getStore().asNeeded();

        var enabled = new SimpleBooleanProperty();
        sec.getWrapper().getPersistentState().subscribe((newValue) -> {
            var rdps = (ToggleSessionState) newValue;
            enabled.set(rdps.getEnabled() != null ? rdps.getEnabled() : false);
        });

        var t = new StoreToggleComp(null, sec, enabled, aBoolean -> {
            var state = s.getState();
            if (state.getEnabled() != aBoolean) {
                state.setEnabled(aBoolean);
                s.setState(state);
                sec.getWrapper().getEntry().validate();
            }
        });
        return StoreEntryComp.create(sec.getWrapper(), t, preferLarge);
    }

    public default Comp<?> stateDisplay(StoreEntryWrapper w) {
        SingletonToggleSessionStore<?> st = w.getEntry().getStore().asNeeded();
        return new SystemStateComp(
                Bindings.createObjectBinding(
                        () -> {
                            ToggleSessionState s = (ToggleSessionState) w.getPersistentState().getValue();
                            if (s.getEnabled() == null || !s.getEnabled()) {
                                return SystemStateComp.State.OTHER;
                            }

                            return s.getRunning() != null && s.getRunning()
                                    ? SystemStateComp.State.SUCCESS
                                    : SystemStateComp.State.FAILURE;
                        },
                        w.getPersistentState(),
                        w.getCache()));
    }

    @Override
    public default void storageInit() {
        for (DataStoreEntry e : DataStorage.get().getStoreEntries()) {
            if (getStoreClasses().stream()
                    .anyMatch(aClass ->
                            e.getStore() != null && e.getStore().getClass().equals(aClass))) {
                SingletonToggleSessionStore<?> tunnelStore = e.getStore().asNeeded();
                var state = tunnelStore.getState();
                state.setEnabled(false);
                tunnelStore.setState(state);
            }
        }
    }

}
