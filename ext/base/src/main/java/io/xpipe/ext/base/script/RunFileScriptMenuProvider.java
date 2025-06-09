package io.xpipe.ext.base.script;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuBranchProvider;
import io.xpipe.app.browser.menu.BrowserMenuItemProvider;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.browser.menu.MultiExecuteSelectionMenuProvider;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ScriptHelper;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class RunFileScriptMenuProvider implements BrowserMenuBranchProvider {

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2c-code-greater-than");
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("runScript");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
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
                public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {}

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
        return createActionForScriptHierarchy(model, hierarchy).getBranchingActions(model, selected);
    }

    private BrowserMenuBranchProvider createActionForScriptHierarchy(
            BrowserFileSystemTabModel model, ScriptHierarchy hierarchy) {
        if (hierarchy.isLeaf()) {
            return createActionForScript(model, hierarchy.getLeafBase());
        }

        var list = hierarchy.getChildren().stream()
                .map(c -> createActionForScriptHierarchy(model, c))
                .toList();
        return new BrowserMenuBranchProvider() {
            @Override
            public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                return PrettyImageHelper.ofFixedSize(hierarchy.getBase().get().getEffectiveIconFile(), 16, 16)
                        .createRegion();
            }

            @Override
            public List<? extends BrowserMenuItemProvider> getBranchingActions(
                    BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                return list;
            }

            @Override
            public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                var b = hierarchy.getBase();
                return new SimpleStringProperty(b != null ? b.get().getName() : null);
            }
        };
    }

    private BrowserMenuBranchProvider createActionForScript(
            BrowserFileSystemTabModel model, DataStoreEntryRef<SimpleScriptStore> ref) {
        return new MultiExecuteSelectionMenuProvider() {

            @Override
            public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                return PrettyImageHelper.ofFixedSize(ref.get().getEffectiveIconFile(), 16, 16)
                        .createRegion();
            }

            @Override
            public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                return new SimpleStringProperty(ref.get().getName());
            }

            @Override
            protected String createCommand(BrowserFileSystemTabModel model) {
                var sc = model.getFileSystem().getShell().orElseThrow();
                var content = ref.getStore().assembleScriptChain(sc);
                var script = ScriptHelper.createExecScript(sc, content);
                return sc.getShellDialect().runScriptCommand(sc, script.toString());
            }

            @Override
            protected String getTerminalTitle() {
                return ref.get().getName() + " - " + model.getName().getValue();
            }
        };
    }
}
