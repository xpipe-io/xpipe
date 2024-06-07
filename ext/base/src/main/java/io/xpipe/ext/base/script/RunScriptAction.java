package io.xpipe.ext.base.script;

import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.ShellControl;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RunScriptAction implements BrowserAction, BranchAction {

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2l-linux");
    }

    @Override
    public Category getCategory() {
        return Category.OPEN;
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("runScript");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        var sc = model.getFileSystem().getShell().orElseThrow();
        return model.getBrowserModel() instanceof BrowserSessionModel
                && !getInstances(sc).isEmpty();
    }

    private Map<String, SimpleScriptStore> getInstances(ShellControl sc) {
        var scripts = ScriptStore.flatten(ScriptStore.getDefaultEnabledScripts());
        var map = new LinkedHashMap<String, SimpleScriptStore>();
        for (SimpleScriptStore script : scripts) {
            if (script.assemble(sc) == null) {
                continue;
            }

            var entry = DataStorage.get().getStoreEntryIfPresent(script, true);
            if (entry.isPresent()) {
                map.put(entry.get().getName(), script);
            }
        }
        return map;
    }

    @Override
    public List<LeafAction> getBranchingActions(OpenFileSystemModel model, List<BrowserEntry> entries) {
        var sc = model.getFileSystem().getShell().orElseThrow();
        var scripts = getInstances(sc);
        List<LeafAction> actions = scripts.entrySet().stream()
                .map(e -> {
                    return new LeafAction() {
                        @Override
                        public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
                            var args = entries.stream().map(browserEntry -> browserEntry.getOptionallyQuotedFileName()).collect(Collectors.joining(" "));
                            execute(model, args);
                        }

                        private void execute(OpenFileSystemModel model, String args) throws Exception {
                            if (model.getBrowserModel() instanceof BrowserSessionModel bm) {
                                var content = e.getValue().assemble(sc);
                                var script = ScriptHelper.createExecScript(sc, content);
                                sc.executeSimpleCommand(sc.getShellDialect().runScriptCommand(sc, script.toString()) + " " + args);
                            }
                        }

                        @Override
                        public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                            return new SimpleStringProperty(e.getKey());
                        }
                    };
                })
                .map(leafAction -> (LeafAction) leafAction)
                .toList();
        return actions;
    }
}
