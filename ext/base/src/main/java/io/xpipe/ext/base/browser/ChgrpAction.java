package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class ChgrpAction implements BranchAction {

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2a-account-group-outline");
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("chgrp");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        var os = model.getFileSystem().getShell().orElseThrow().getOsType();
        return os != OsType.WINDOWS && os != OsType.MACOS;
    }

    @Override
    public List<LeafAction> getBranchingActions(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return model.getCache().getGroups().entrySet().stream()
                .filter(e -> !e.getValue().equals("nohome")
                        && !e.getValue().equals("nogroup")
                        && !e.getValue().equals("nobody")
                        && (e.getKey().equals(0) || e.getKey() >= 1000))
                .map(e -> e.getValue())
                .map(s -> (LeafAction) new Chgrp(s))
                .toList();
    }

    private static class Chgrp implements LeafAction {

        private final String option;

        private Chgrp(String option) {
            this.option = option;
        }

        @Override
        public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
            return new SimpleStringProperty(option);
        }

        @Override
        public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
            model.getFileSystem()
                    .getShell()
                    .orElseThrow()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("chgrp", option)
                            .addFiles(entries.stream()
                                    .map(browserEntry ->
                                            browserEntry.getRawFileEntry().getPath())
                                    .toList()));
        }
    }
}
