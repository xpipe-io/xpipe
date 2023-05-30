package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.FileOpener;
import io.xpipe.core.store.FileKind;
import javafx.scene.Node;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class EditFileAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        for (BrowserEntry entry : entries) {
            FileOpener.openInTextEditor(entry.getRawFileEntry());
        }
    }

    @Override
    public Category getCategory() {
        return Category.OPEN;
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2p-pencil");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.FILE);
    }

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "Edit with " + AppPrefs.get().externalEditor().getValue().toTranslatedString();
    }
}
