package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import javafx.scene.Node;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class ChmodAction implements BranchAction {

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS;
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2w-wrench");
    }

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "Chmod";
    }

    private static class Chmod implements LeafAction {

        private final String option;

        private Chmod(String option) {this.option = option;}

        @Override
        public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
            return option;
          }

        @Override
        public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
            model.getFileSystem().getShell().orElseThrow().executeSimpleCommand(CommandBuilder.of().add("chmod", option)
                    .addFiles(entries.stream().map(browserEntry -> browserEntry.getRawFileEntry().getPath()).toList()));
        }
    }

    @Override
    public List<LeafAction> getBranchingActions(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return List.of(new Chmod("400"), new Chmod("600"), new Chmod("644"), new Chmod("700"), new Chmod("755"), new Chmod("777"), new Chmod("u+x"), new Chmod("a+x"));
    }
}
