package io.xpipe.ext.base.action;

import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.CommandDialog;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.process.CommandControl;
import io.xpipe.core.process.ShellTtyState;
import io.xpipe.core.process.SystemState;
import io.xpipe.ext.base.script.ScriptHierarchy;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import lombok.Value;

import java.util.LinkedHashMap;
import java.util.List;

public class RunScriptActionMenu implements ActionProvider {

    @Value
    private static class TerminalRunActionProvider implements ActionProvider {

        ScriptHierarchy hierarchy;

        @Value
        private class Action implements ActionProvider.Action {

            DataStoreEntryRef<ShellStore> shellStore;

            @Override
            public void execute() throws Exception {
                var sc = shellStore.getStore().getOrStartSession();
                var script = hierarchy.getLeafBase().getStore().assembleScriptChain(sc);
                TerminalLauncher.open(
                        shellStore.get(),
                        hierarchy.getLeafBase().get().getName() + " - "
                                + shellStore.get().getName(),
                        null,
                        sc.command(script));
            }
        }

        @Override
        public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
            return new LeafDataStoreCallSite<ShellStore>() {
                @Override
                public Action createAction(DataStoreEntryRef<ShellStore> store) {
                    return new Action(store);
                }

                @Override
                public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
                    var t = AppPrefs.get().terminalType().getValue();
                    return AppI18n.observable(
                            "executeInTerminal",
                            t != null ? t.toTranslatedString().getValue() : "?");
                }

                @Override
                public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
                    return new LabelGraphic.IconGraphic("mdi2c-code-greater-than");
                }

                @Override
                public Class<?> getApplicableClass() {
                    return ShellStore.class;
                }
            };
        }

        @Override
        public BatchDataStoreCallSite<ShellStore> getBatchDataStoreCallSite() {
            return new BatchDataStoreCallSite<>() {

                @Override
                public ObservableValue<String> getName() {
                    var t = AppPrefs.get().terminalType().getValue();
                    return AppI18n.observable(
                            "executeInTerminal",
                            t != null ? t.toTranslatedString().getValue() : "?");
                }

                @Override
                public LabelGraphic getIcon() {
                    return new LabelGraphic.IconGraphic("mdi2c-code-greater-than");
                }

                @Override
                public Class<?> getApplicableClass() {
                    return ShellStore.class;
                }

                @Override
                public ActionProvider.Action createAction(DataStoreEntryRef<ShellStore> store) {
                    return new Action(store);
                }
            };
        }
    }

    @Value
    private static class HubRunActionProvider implements ActionProvider {

        ScriptHierarchy hierarchy;

        @Override
        public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
            return new LeafDataStoreCallSite<ShellStore>() {
                @Override
                public Action createAction(DataStoreEntryRef<ShellStore> store) {
                    return () -> {
                        var sc = store.getStore().getOrStartSession();
                        var script = hierarchy.getLeafBase().getStore().assembleScriptChain(sc);
                        var cmd = sc.command(script);
                        CommandDialog.runAsyncAndShow(cmd);
                    };
                }

                @Override
                public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
                    return AppI18n.observable("runInConnectionHub");
                }

                @Override
                public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
                    return new LabelGraphic.IconGraphic("mdi2d-desktop-mac");
                }

                @Override
                public Class<?> getApplicableClass() {
                    return ShellStore.class;
                }
            };
        }

        @Override
        public BatchDataStoreCallSite<ShellStore> getBatchDataStoreCallSite() {
            return new BatchDataStoreCallSite<>() {

                @Override
                public ObservableValue<String> getName() {
                    return AppI18n.observable("runInConnectionHub");
                }

                @Override
                public LabelGraphic getIcon() {
                    return new LabelGraphic.IconGraphic("mdi2d-desktop-mac");
                }

                @Override
                public Class<?> getApplicableClass() {
                    return ShellStore.class;
                }

                @Override
                public Action createAction(List<DataStoreEntryRef<ShellStore>> stores) {
                    return () -> {
                        var map = new LinkedHashMap<String, CommandControl>();
                        for (DataStoreEntryRef<ShellStore> ref : stores) {
                            var sc = ref.getStore().getOrStartSession();
                            var script = hierarchy.getLeafBase().getStore().assembleScriptChain(sc);
                            var cmd = sc.command(script);
                            map.put(ref.get().getName(), cmd);
                        }
                        CommandDialog.runAsyncAndShow(map);
                    };
                }
            };
        }
    }

    @Value
    private static class BackgroundRunActionProvider implements ActionProvider {

        ScriptHierarchy hierarchy;

        @Value
        private class Action implements ActionProvider.Action {

            DataStoreEntryRef<ShellStore> shellStore;

            @Override
            public void execute() throws Exception {
                var sc = shellStore.getStore().getOrStartSession();
                sc.checkLicenseOrThrow();
                var script = hierarchy.getLeafBase().getStore().assembleScriptChain(sc);
                sc.command(script).execute();
            }
        }

        @Override
        public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
            return new LeafDataStoreCallSite<ShellStore>() {
                @Override
                public Action createAction(DataStoreEntryRef<ShellStore> store) {
                    return new Action(store);
                }

                @Override
                public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
                    return AppI18n.observable("executeInBackground");
                }

                @Override
                public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
                    return new LabelGraphic.IconGraphic("mdi2f-flip-to-back");
                }

                @Override
                public Class<?> getApplicableClass() {
                    return ShellStore.class;
                }
            };
        }

        @Override
        public BatchDataStoreCallSite<?> getBatchDataStoreCallSite() {
            return new BatchDataStoreCallSite<ShellStore>() {

                @Override
                public Class<ShellStore> getApplicableClass() {
                    return ShellStore.class;
                }

                @Override
                public ObservableValue<String> getName() {
                    return AppI18n.observable("executeInBackground");
                }

                @Override
                public LabelGraphic getIcon() {
                    return new LabelGraphic.IconGraphic("mdi2f-flip-to-back");
                }

                @Override
                public ActionProvider.Action createAction(DataStoreEntryRef<ShellStore> store) {
                    return new Action(store);
                }

                @Override
                public List<ActionProvider> getChildren(List<DataStoreEntryRef<ShellStore>> batch) {
                    return List.of();
                }
            };
        }
    }

    @Value
    private static class ScriptActionProvider implements ActionProvider {

        ScriptHierarchy hierarchy;

        private BranchDataStoreCallSite<?> getLeafSite() {
            return new BranchDataStoreCallSite<ShellStore>() {

                @Override
                public Class<ShellStore> getApplicableClass() {
                    return ShellStore.class;
                }

                @Override
                public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
                    return new SimpleStringProperty(hierarchy.getBase().get().getName());
                }

                @Override
                public boolean isDynamicallyGenerated() {
                    return true;
                }

                @Override
                public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
                    return new LabelGraphic.ImageGraphic(
                            hierarchy.getBase().get().getEffectiveIconFile(), 16);
                }

                @Override
                public List<? extends ActionProvider> getChildren(DataStoreEntryRef<ShellStore> store) {
                    return List.of(
                            new TerminalRunActionProvider(hierarchy),
                            new HubRunActionProvider(hierarchy),
                            new BackgroundRunActionProvider(hierarchy));
                }
            };
        }

        public BranchDataStoreCallSite<?> getBranchDataStoreCallSite() {
            if (hierarchy.isLeaf()) {
                return getLeafSite();
            }

            return new BranchDataStoreCallSite<ShellStore>() {

                @Override
                public Class<ShellStore> getApplicableClass() {
                    return ShellStore.class;
                }

                @Override
                public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
                    return new SimpleStringProperty(hierarchy.getBase().get().getName());
                }

                @Override
                public boolean isDynamicallyGenerated() {
                    return true;
                }

                @Override
                public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
                    return new LabelGraphic.IconGraphic("mdi2p-play-box-multiple-outline");
                }

                @Override
                public List<? extends ActionProvider> getChildren(DataStoreEntryRef<ShellStore> store) {
                    return hierarchy.getChildren().stream()
                            .map(c -> new ScriptActionProvider(c))
                            .toList();
                }
            };
        }

        @Override
        public BatchDataStoreCallSite<?> getBatchDataStoreCallSite() {
            return new BatchDataStoreCallSite<ShellStore>() {

                @Override
                public ObservableValue<String> getName() {
                    return new SimpleStringProperty(hierarchy.getBase().get().getName());
                }

                @Override
                public LabelGraphic getIcon() {
                    return new LabelGraphic.IconGraphic("mdi2p-play-box-multiple-outline");
                }

                @Override
                public Class<?> getApplicableClass() {
                    return ShellStore.class;
                }

                @Override
                public Action createAction(DataStoreEntryRef<ShellStore> store) {
                    return null;
                }

                @Override
                public List<? extends ActionProvider> getChildren(List<DataStoreEntryRef<ShellStore>> batch) {
                    if (hierarchy.isLeaf()) {
                        return List.of(
                                new TerminalRunActionProvider(hierarchy),
                                new HubRunActionProvider(hierarchy),
                                new BackgroundRunActionProvider(hierarchy));
                    }

                    return hierarchy.getChildren().stream()
                            .map(c -> new ScriptActionProvider(c))
                            .toList();
                }
            };
        }
    }

    private static class NoScriptsActionProvider implements ActionProvider {

        private static class Action implements ActionProvider.Action {

            @Override
            public void execute() {
                StoreViewState.get().getAllScriptsCategory().select();
            }
        }

        @Override
        public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
            return new LeafDataStoreCallSite<ShellStore>() {
                @Override
                public Action createAction(DataStoreEntryRef<ShellStore> store) {
                    return new Action();
                }

                @Override
                public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
                    return AppI18n.observable("noScriptsAvailable");
                }

                @Override
                public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
                    return new LabelGraphic.IconGraphic("mdi2i-image-filter-none");
                }

                @Override
                public Class<?> getApplicableClass() {
                    return ShellStore.class;
                }
            };
        }

        @Override
        public BatchDataStoreCallSite<?> getBatchDataStoreCallSite() {
            return new BatchDataStoreCallSite<ShellStore>() {
                @Override
                public ObservableValue<String> getName() {
                    return AppI18n.observable("noScriptsAvailable");
                }

                @Override
                public LabelGraphic getIcon() {
                    return new LabelGraphic.IconGraphic("mdi2i-image-filter-none");
                }

                @Override
                public Class<?> getApplicableClass() {
                    return ShellStore.class;
                }

                @Override
                public ActionProvider.Action createAction(List<DataStoreEntryRef<ShellStore>> stores) {
                    return new Action();
                }
            };
        }
    }

    private static class NoStateActionProvider implements ActionProvider {

        @Override
        public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
            return new LeafDataStoreCallSite<ShellStore>() {
                @Override
                public Action createAction(DataStoreEntryRef<ShellStore> store) {
                    return new Action() {
                        @Override
                        public void execute() {
                            store.get().validate();
                        }
                    };
                }

                @Override
                public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
                    return AppI18n.observable("noScriptStateAvailable");
                }

                @Override
                public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
                    return new LabelGraphic.IconGraphic("mdi2i-image-filter-none");
                }

                @Override
                public Class<?> getApplicableClass() {
                    return ShellStore.class;
                }
            };
        }

        @Override
        public BatchDataStoreCallSite<?> getBatchDataStoreCallSite() {
            return new BatchDataStoreCallSite<ShellStore>() {
                @Override
                public ObservableValue<String> getName() {
                    return AppI18n.observable("noScriptStateAvailable");
                }

                @Override
                public LabelGraphic getIcon() {
                    return new LabelGraphic.IconGraphic("mdi2i-image-filter-none");
                }

                @Override
                public Class<?> getApplicableClass() {
                    return ShellStore.class;
                }

                @Override
                public ActionProvider.Action createAction(DataStoreEntryRef<ShellStore> store) {
                    return new Action() {
                        @Override
                        public void execute() {
                            store.get().validate();
                        }
                    };
                }
            };
        }
    }

    @Override
    public BranchDataStoreCallSite<?> getBranchDataStoreCallSite() {
        return new BranchDataStoreCallSite<ShellStore>() {

            @Override
            public Class<ShellStore> getApplicableClass() {
                return ShellStore.class;
            }

            @Override
            public boolean isMajor(DataStoreEntryRef<ShellStore> o) {
                return true;
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
                return AppI18n.observable("runScript");
            }

            @Override
            public boolean isDynamicallyGenerated() {
                return true;
            }

            @Override
            public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
                return new LabelGraphic.IconGraphic("mdi2p-play-box-multiple-outline");
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
                var state = o.get().getStorePersistentState();
                if (state instanceof SystemState systemState) {
                    return (systemState.getShellDialect() == null
                                    || systemState
                                            .getShellDialect()
                                            .getDumbMode()
                                            .supportsAnyPossibleInteraction())
                            && (systemState.getTtyState() == null || systemState.getTtyState() == ShellTtyState.NONE);
                } else {
                    return false;
                }
            }

            @Override
            public List<? extends ActionProvider> getChildren(DataStoreEntryRef<ShellStore> store) {
                var replacement = ProcessControlProvider.get().replace(store);
                var state = replacement.get().getStorePersistentState();
                if (!(state instanceof SystemState systemState) || systemState.getShellDialect() == null) {
                    return List.of(new NoStateActionProvider());
                }

                var hierarchy = ScriptHierarchy.buildEnabledHierarchy(ref -> {
                    if (!ref.getStore().isRunnableScript()) {
                        return false;
                    }

                    if (!ref.getStore().isCompatible(systemState.getShellDialect())) {
                        return false;
                    }

                    return true;
                });
                var list = hierarchy.getChildren().stream()
                        .map(c -> new ScriptActionProvider(c))
                        .toList();
                if (list.isEmpty()) {
                    return List.of(new NoScriptsActionProvider());
                } else {
                    return list;
                }
            }
        };
    }

    @Override
    public BatchDataStoreCallSite<?> getBatchDataStoreCallSite() {
        return new BatchDataStoreCallSite<ShellStore>() {

            @Override
            public Class<ShellStore> getApplicableClass() {
                return ShellStore.class;
            }

            @Override
            public ObservableValue<String> getName() {
                return AppI18n.observable("runScript");
            }

            @Override
            public LabelGraphic getIcon() {
                return new LabelGraphic.IconGraphic("mdi2p-play-box-multiple-outline");
            }

            @Override
            public List<ActionProvider> getChildren(List<DataStoreEntryRef<ShellStore>> batch) {
                var stateMissing = batch.stream().anyMatch(ref -> {
                    var state = ref.get().getStorePersistentState();
                    if (state instanceof SystemState systemState) {
                        if (systemState.getShellDialect() == null) {
                            return true;
                        }

                        if (systemState.getTtyState() == null || systemState.getTtyState() != ShellTtyState.NONE) {
                            return true;
                        }
                    }
                    return false;
                });

                if (stateMissing) {
                    return List.of(new NoStateActionProvider());
                }

                var hierarchy = ScriptHierarchy.buildEnabledHierarchy(scriptRef -> {
                    var compatible = batch.stream().allMatch(ref -> {
                        var state = ref.get().getStorePersistentState();
                        if (state instanceof SystemState systemState) {
                            return scriptRef
                                    .getStore()
                                    .getMinimumDialect()
                                    .isCompatibleTo(systemState.getShellDialect());
                        } else {
                            return false;
                        }
                    });
                    if (!compatible) {
                        return false;
                    }

                    if (!scriptRef.getStore().isRunnableScript()) {
                        return false;
                    }

                    return true;
                });
                var list = hierarchy.getChildren().stream()
                        .<ActionProvider>map(c -> new ScriptActionProvider(c))
                        .toList();
                if (list.isEmpty()) {
                    return List.of(new NoScriptsActionProvider());
                } else {
                    return list;
                }
            }
        };
    }
}
