package io.xpipe.ext.base.script;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.*;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.util.ScriptHelper;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class RunFileScriptMenuProvider implements BrowserMenuBranchProvider {

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-code-greater-than");
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.ACTION;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("runScript");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        if (model.getFileSystem().getShell().isEmpty()) {
            return false;
        }

        return model.getBrowserModel() instanceof BrowserFullSessionModel;
    }

    @Override
    public List<? extends BrowserMenuItemProvider> getBranchingActions(
            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var config =
                DataStorage.get().getEffectiveCategoryConfig(model.getEntry().get());
        if (Boolean.TRUE.equals(config.getDontAllowScripts())) {
            return List.of(new BrowserMenuLeafProvider() {
                @Override
                public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                    return AppI18n.observable("scriptsDisabled");
                }
            });
        }

        var actions = createActionForScriptHierarchy(model, entries);
        if (actions.isEmpty()) {
            actions = List.of(new BrowserMenuLeafProvider() {
                @Override
                public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                    StoreViewState.get().getAllScriptsCategory().select();
                    AppLayoutModel.get().selectConnections();
                }

                @Override
                public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                    return AppI18n.observable("noScriptsAvailable");
                }
            });
        }
        return actions;
    }

    private List<? extends BrowserMenuItemProvider> createActionForScriptHierarchy(
            BrowserFileSystemTabModel model, List<BrowserEntry> selected) {
        var sc = model.getFileSystem().getShell().orElseThrow();
        var hierarchy = ScriptHierarchy.buildEnabledHierarchy(ref -> {
            if (!ref.getStore().isFileScript()) {
                return false;
            }

            if (!ref.getStore().isCompatible(sc)) {
                return false;
            }
            return true;
        });
        return createActionForScriptHierarchy(hierarchy).getBranchingActions(model, selected);
    }

    private BrowserMenuBranchProvider createActionForScriptHierarchy(ScriptHierarchy hierarchy) {
        if (hierarchy.isLeaf()) {
            return createActionForScript(hierarchy.getLeafBase());
        }

        var list = hierarchy.getChildren().stream()
                .map(c -> createActionForScriptHierarchy(c))
                .toList();
        return new BrowserMenuBranchProvider() {
            @Override
            public LabelGraphic getIcon() {
                return new LabelGraphic.CompGraphic(
                        PrettyImageHelper.ofFixedSize(hierarchy.getBase().get().getEffectiveIconFile(), 16, 16));
            }

            @Override
            public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                var b = hierarchy.getBase();
                return new SimpleStringProperty(b != null ? b.get().getName() : null);
            }

            @Override
            public List<? extends BrowserMenuItemProvider> getBranchingActions(
                    BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                return list;
            }
        };
    }

    private BrowserMenuBranchProvider createActionForScript(DataStoreEntryRef<SimpleScriptStore> ref) {
        return new MultiExecuteMenuProvider() {

            @Override
            public LabelGraphic getIcon() {
                return new LabelGraphic.CompGraphic(
                        PrettyImageHelper.ofFixedSize(ref.get().getEffectiveIconFile(), 16, 16));
            }

            @Override
            public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                return new SimpleStringProperty(ref.get().getName());
            }

            @Override
            protected List<CommandBuilder> createCommand(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                var sc = model.getFileSystem().getShell().orElseThrow();
                var content = ref.getStore().assembleScriptChain(sc);
                var script = ScriptHelper.createExecScript(sc, content);
                var builder = CommandBuilder.of().add(sc.getShellDialect().runScriptCommand(sc, script.toString()));
                for (BrowserEntry entry : entries) {
                    builder.addFile(entry.getRawFileEntry().getPath());
                }
                return List.of(builder);
            }
        };
    }
}
