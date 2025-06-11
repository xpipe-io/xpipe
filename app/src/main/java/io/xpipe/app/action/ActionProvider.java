package io.xpipe.app.action;

import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.util.ModuleLayerLoader;

import java.util.*;

public interface ActionProvider {

    List<ActionProvider> ALL = new ArrayList<>();

    static void initProviders() {
        TrackEvent.trace("Starting action provider initialization");
        for (ActionProvider actionProvider : ALL) {
            try {
                actionProvider.init();
            } catch (Throwable t) {
                ErrorEventFactory.fromThrowable(t).handle();
            }
        }
        TrackEvent.trace("Finished action provider initialization");
    }

    default void init() throws Exception {}

    default String getLicensedFeatureId() {
        return null;
    }

    default String getId() {
        return null;
    }

    default boolean isMutation() {
        return false;
    }

    @SuppressWarnings("unchecked")
    default Optional<Class<? extends AbstractAction>> getActionClass() {
        var child = Arrays.stream(getClass().getDeclaredClasses())
                .filter(aClass -> aClass.getSimpleName().equals("Action"))
                .findFirst()
                .map(aClass -> (Class<? extends AbstractAction>) aClass);
        return Optional.of(child.get());
    }

    class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL.addAll(ServiceLoader.load(layer, ActionProvider.class).stream()
                    .map(p -> p.get())
                    .toList());
            for (var p : DataStoreProviders.getAll()) {
                ALL.addAll(p.getActionProviders());
            }
        }
    }
}
