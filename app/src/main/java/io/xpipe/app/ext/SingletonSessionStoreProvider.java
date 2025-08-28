package io.xpipe.app.ext;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.hub.action.impl.ToggleActionProvider;
import io.xpipe.app.hub.comp.*;
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

    default Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(Bindings.createObjectBinding(
                () -> {
                    SingletonSessionStore<?> s = w.getEntry().getStore().asNeeded();
                    if (!s.isSessionEnabled() || (s.isSessionEnabled() && !s.isSessionRunning())) {
                        return SystemStateComp.State.OTHER;
                    }

                    return s.isSessionRunning() ? SystemStateComp.State.SUCCESS : SystemStateComp.State.FAILURE;
                },
                w.getCache()));
    }

    default StoreToggleComp createToggleComp(StoreSection sec) {
        var enabled = new SimpleBooleanProperty();
        sec.getWrapper().getCache().subscribe((newValue) -> {
            SingletonSessionStore<?> s = sec.getWrapper().getEntry().getStore().asNeeded();
            enabled.set(s.isSessionEnabled());
        });

        ObservableValue<LabelGraphic> g = enabled.map(aBoolean -> aBoolean
                ? new LabelGraphic.IconGraphic("mdi2c-circle-slice-8")
                : new LabelGraphic.IconGraphic("mdi2p-power"));
        var t = new StoreToggleComp(null, g, sec, enabled, newState -> {
            SingletonSessionStore<?> s = sec.getWrapper().getEntry().getStore().asNeeded();
            if (s.isSessionEnabled() != newState) {
                var action = ToggleActionProvider.Action.builder()
                        .ref(sec.getWrapper().getEntry().ref())
                        .enabled(newState)
                        .build();
                action.executeAsync();
            }
        });
        t.tooltipKey("enabled");
        return t;
    }
}
