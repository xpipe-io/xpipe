package io.xpipe.app.action;

import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.hub.action.BatchHubProvider;
import io.xpipe.app.hub.action.HubBranchProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.HubMenuItemProvider;
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

                // For debugging
//                if (actionProvider instanceof HubLeafProvider<?>) {
//                    actionProvider.getActionClass().orElseThrow();
//                }
//                if (actionProvider instanceof HubBranchProvider<?> b) {
//                    for (HubMenuItemProvider<?> child : b.getChildren(null)) {
//                        if (ALL.stream().noneMatch(a -> a.getClass().equals(child.getClass()))) {
//                            System.out.println(child.getClass());
//                        }
//                    }
//                }

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

    @SuppressWarnings("unchecked")
    default Optional<Class<? extends AbstractAction>> getActionClass() {
        var child = Arrays.stream(getClass().getDeclaredClasses())
                .filter(aClass -> aClass.getSimpleName().equals("Action"))
                .findFirst()
                .map(aClass -> (Class<? extends AbstractAction>) aClass);
        return child.isPresent() ? Optional.of(child.get()) : Optional.empty();
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
