package io.xpipe.ext.base.script;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserBranchAction;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.ext.base.browser.MultiExecuteSelectionAction;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class RunScriptAction implements BrowserAction, BrowserBranchAction {

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
    public List<? extends BrowserAction> getBranchingActions(
            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var actions = createActionForScriptHierarchy(model, entries);
        if (actions.isEmpty()) {
            actions = List.of(new BrowserLeafAction() {
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

    private List<? extends BrowserAction> createActionForScriptHierarchy(
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

    private BrowserBranchAction createActionForScriptHierarchy(
            BrowserFileSystemTabModel model, ScriptHierarchy hierarchy) {
        if (hierarchy.isLeaf()) {
            return createActionForScript(model, hierarchy.getLeafBase());
        }

        var list = hierarchy.getChildren().stream()
                .map(c -> createActionForScriptHierarchy(model, c))
                .toList();
        return new BrowserBranchAction() {
            @Override
            public List<? extends BrowserAction> getBranchingActions(
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

    private BrowserBranchAction createActionForScript(
            BrowserFileSystemTabModel model, DataStoreEntryRef<SimpleScriptStore> ref) {
        return new MultiExecuteSelectionAction() {

            @Override
            public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                return new SimpleStringProperty(ref.get().getName());
            }

            @Override
            protected CommandBuilder createCommand(
                    ShellControl sc, BrowserFileSystemTabModel model, List<BrowserEntry> selected) {
                if (!(model.getBrowserModel() instanceof BrowserFullSessionModel)) {
                    return null;
                }

                var content = ref.getStore().assembleScriptChain(sc);
                var script = ScriptHelper.createExecScript(sc, content);
                var builder = CommandBuilder.of().add(sc.getShellDialect().runScriptCommand(sc, script.toString()));
                selected.stream()
                        .map(browserEntry -> browserEntry.getRawFileEntry().getPath())
                        .forEach(s -> {
                            builder.addFile(s);
                        });
                return builder;
            }

            @Override
            protected String getTerminalTitle() {
                return ref.get().getName() + " - " + model.getName().getValue();
            }
        };
    }
}
