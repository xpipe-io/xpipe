package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserActionFormatter;
import io.xpipe.app.browser.action.MultiExecuteAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class JarAction extends MultiExecuteAction implements JavaAction, FileTypeAction {

    @Override
    public Category getCategory() {
        return Category.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new SimpleStringProperty("java -jar " + BrowserActionFormatter.filesArgument(entries));
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return super.isApplicable(model, entries) && FileTypeAction.super.isApplicable(model, entries);
    }

    @Override
    protected CommandBuilder createCommand(ShellControl sc, OpenFileSystemModel model, BrowserEntry entry) {
        return CommandBuilder.of()
                .add("java", "-jar")
                .addFile(entry.getRawFileEntry().getPath());
    }

    @Override
    public BrowserIconFileType getType() {
        return BrowserIconFileType.byId("jar");
    }
}
