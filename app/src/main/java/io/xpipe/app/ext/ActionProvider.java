package io.xpipe.app.ext;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.FailableConsumer;
import io.xpipe.core.util.ModuleLayerLoader;

import javafx.beans.value.ObservableValue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

public interface ActionProvider {

    List<ActionProvider> ALL = new ArrayList<>();
    List<ActionProvider> ALL_STANDALONE = new ArrayList<>();

    static void initProviders() {
        for (ActionProvider actionProvider : ALL) {
            try {
                actionProvider.init();
            } catch (Throwable t) {
                ErrorEvent.fromThrowable(t).handle();
            }
        }
    }

    default void init() throws Exception {}

    default String getId() {
        return null;
    }

    default String getProFeatureId() {
        return null;
    }

    default LauncherCallSite getLauncherCallSite() {
        return null;
    }

    default LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return null;
    }

    default BranchDataStoreCallSite<?> getBranchDataStoreCallSite() {
        return null;
    }

    default BatchDataStoreCallSite<?> getBatchDataStoreCallSite() {
        return null;
    }

    default DefaultDataStoreCallSite<?> getDefaultDataStoreCallSite() {
        return null;
    }

    interface Action {

        void execute() throws Exception;
    }

    interface LauncherCallSite {

        String getId();

        Action createAction(URI uri) throws Exception;
    }

    interface XPipeLauncherCallSite extends LauncherCallSite {

        String getId();

        default Action createAction(URI uri) throws Exception {
            var args = new ArrayList<>(Arrays.asList(uri.getPath().substring(1).split("/")));
            args.addFirst(uri.getHost());
            return createAction(args);
        }

        Action createAction(List<String> args) throws Exception;
    }

    interface DefaultDataStoreCallSite<T extends DataStore> {

        Action createAction(DataStoreEntryRef<T> store);

        Class<T> getApplicableClass();

        default boolean isApplicable(DataStoreEntryRef<T> o) {
            return true;
        }

        default boolean showBusy() {
            return true;
        }
    }

    interface DataStoreCallSite<T extends DataStore> {

        default boolean isSystemAction() {
            return false;
        }

        default boolean isMajor(DataStoreEntryRef<T> o) {
            return false;
        }

        default boolean isApplicable(DataStoreEntryRef<T> o) {
            return true;
        }

        ObservableValue<String> getName(DataStoreEntryRef<T> store);

        String getIcon(DataStoreEntryRef<T> store);

        Class<?> getApplicableClass();

        default boolean showBusy() {
            return true;
        }
    }

    interface BranchDataStoreCallSite<T extends DataStore> extends DataStoreCallSite<T> {

        default boolean isDynamicallyGenerated() {
            return false;
        }

        List<? extends ActionProvider> getChildren(DataStoreEntryRef<T> store);
    }

    interface LeafDataStoreCallSite<T extends DataStore> extends DataStoreCallSite<T> {

        default ActionProvider provider() {
            return new ActionProvider() {
                @Override
                public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
                    return LeafDataStoreCallSite.this;
                }
            };
        }

        static <T extends DataStore> LeafDataStoreCallSite<T> simple(
                boolean major,
                String nameKey,
                String icon,
                Class<T> applicableClass,
                FailableConsumer<DataStoreEntryRef<T>, Exception> action) {
            return new LeafDataStoreCallSite<>() {
                @Override
                public boolean isMajor(DataStoreEntryRef<T> o) {
                    return major;
                }

                @Override
                public Action createAction(DataStoreEntryRef<T> store) {
                    return new Action() {
                        @Override
                        public void execute() throws Exception {
                            action.accept(store);
                        }
                    };
                }

                @Override
                public ObservableValue<String> getName(DataStoreEntryRef<T> store) {
                    return AppI18n.observable(nameKey);
                }

                @Override
                public String getIcon(DataStoreEntryRef<T> store) {
                    return icon;
                }

                @Override
                public Class<?> getApplicableClass() {
                    return applicableClass;
                }
            };
        }

        default boolean canLinkTo() {
            return false;
        }

        Action createAction(DataStoreEntryRef<T> store);

        default boolean requiresValidStore() {
            return true;
        }
    }

    interface BatchDataStoreCallSite<T extends DataStore> {

        ObservableValue<String> getName();

        String getIcon();

        Class<?> getApplicableClass();

        default boolean isApplicable(DataStoreEntryRef<T> o) {
            return true;
        }

        default Action createAction(List<DataStoreEntryRef<T>> stores) {
            var individual = stores.stream().map(ref -> {
                return createAction(ref);
            }).filter(action -> action != null).toList();
            return new Action() {
                @Override
                public void execute() throws Exception {
                    for (Action action : individual) {
                        action.execute();
                    }
                }
            };
        }

        default Action createAction(DataStoreEntryRef<T> store) {
            return null;
        }

        default List<? extends ActionProvider> getChildren(List<DataStoreEntryRef<T>> batch) {
            return List.of();
        }
    }

    class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL.addAll(ServiceLoader.load(layer, ActionProvider.class).stream()
                    .map(actionProviderProvider -> actionProviderProvider.get())
                    .toList());

            for (var p : DataStoreProviders.getAll()) {
                ALL.addAll(p.getActionProviders());
            }

            var menuProviders = ALL.stream()
                    .map(actionProvider -> actionProvider.getBranchDataStoreCallSite() != null
                                    && !actionProvider
                                            .getBranchDataStoreCallSite()
                                            .isDynamicallyGenerated()
                            ? actionProvider.getBranchDataStoreCallSite().getChildren(null)
                            : List.of())
                    .flatMap(List::stream)
                    .toList();
            ALL_STANDALONE.addAll(ALL.stream()
                    .filter(actionProvider -> menuProviders.stream()
                            .noneMatch(menuItem -> menuItem.getClass().equals(actionProvider.getClass())))
                    .toList());
        }
    }
}
