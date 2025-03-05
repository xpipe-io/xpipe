package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserActionFormatter;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.core.process.CommandBuilder;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class JavapAction extends ToFileCommandAction implements FileTypeAction, JavaAction {

    @Override
    public Category getCategory() {
        return Category.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new SimpleStringProperty("javap -c -p " + BrowserActionFormatter.filesArgument(entries));
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return super.isApplicable(model, entries) && FileTypeAction.super.isApplicable(model, entries);
    }

    @Override
    public BrowserIconFileType getType() {
        return BrowserIconFileType.byId("class");
    }

    @Override
    protected CommandBuilder createCommand(BrowserFileSystemTabModel model, BrowserEntry entry) {
        return CommandBuilder.of()
                .add("javap", "-c", "-p")
                .addFile(entry.getRawFileEntry().getPath());
    }
}
