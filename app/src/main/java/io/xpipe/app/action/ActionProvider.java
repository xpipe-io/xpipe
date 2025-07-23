package io.xpipe.app.action;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.ModuleLayerLoader;

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

    default void init() {}

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
                    .sorted(Comparator.comparing(p -> p.type().getModule().getName()))
                    .map(p -> p.get())
                    .toList());
        }
    }
}
