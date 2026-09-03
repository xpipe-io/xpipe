package io.xpipe.app.store;

import io.xpipe.app.hub.action.impl.ToggleActionProvider;
import io.xpipe.app.hub.entry.StoreEntryComp;
import io.xpipe.app.hub.entry.StoreEntryWrapper;
import io.xpipe.app.hub.entry.StoreToggleComp;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.platform.LabelGraphic;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

public interface SingletonSessionStoreProvider extends DataStoreProvider {

    @Override
    default ObservableBooleanValue busy(StoreEntryWrapper wrapper) {
        return Bindings.createBooleanBinding(
                () -> {
                    // This can be called while reloading the storage
                    // where an entry is made invalid
                    if (wrapper.getEntry().getStore() == null) {
                        return false;
                    }

                    SingletonSessionStore<?> s = wrapper.getEntry().getStore().asNeeded();
                    return s.isSessionEnabled() != s.isSessionRunning();
                },
                wrapper.getCache());
    }

    @Override
    default StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        var t = createToggleComp(sec);
        return StoreEntryComp.create(sec, t, preferLarge);
    }

    default StoreToggleComp createToggleComp(StoreSection sec) {
        var enabled = new SimpleBooleanProperty();
        sec.getWrapper().getCache().subscribe((ignored) -> {
            var entry = sec.getWrapper().getEntry();
            if (entry.getStore() == null) {
                return;
            }

            SingletonSessionStore<?> s = entry.getStore().asNeeded();
            enabled.set(s.isSessionEnabled());
        });

        ObservableValue<LabelGraphic> g = enabled.map(aBoolean -> aBoolean
                ? new LabelGraphic.IconGraphic("mdi2c-circle-slice-8")
                : new LabelGraphic.IconGraphic("mdi2p-power"));
        var t = new StoreToggleComp(null, g, sec, enabled, newState -> {
            var entry = sec.getWrapper().getEntry();
            if (entry.getStore() == null) {
                return;
            }

            SingletonSessionStore<?> s = entry.getStore().asNeeded();
            if (s.isSessionEnabled() != newState) {
                var action = ToggleActionProvider.Action.builder()
                        .ref(sec.getWrapper().getEntry().ref())
                        .enabled(newState)
                        .build();
                action.executeAsync();
            }
        });

        t.setCustomVisibility(Bindings.createBooleanBinding(
                () -> {
                    SingletonSessionStore<?> s =
                            sec.getWrapper().getEntry().getStore().asNeeded();
                    return supportsSession(s) && (showToggleWhenInactive(s) || s.isSessionEnabled());
                },
                sec.getWrapper().getCache(),
                enabled));
        return t;
    }

    default boolean showToggleWhenInactive(SingletonSessionStore<?> store) {
        return true;
    }

    default boolean supportsSession(SingletonSessionStore<?> store) {
        return true;
    }
}
