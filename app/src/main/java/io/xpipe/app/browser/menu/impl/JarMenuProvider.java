package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.app.browser.menu.*;
import io.xpipe.app.process.CommandBuilder;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class JarMenuProvider extends MultiExecuteMenuProvider
        implements BrowserApplicationPathMenuProvider, FileTypeMenuProvider {

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var arg = entries.size() == 1 ? entries.getFirst().getFileName() : "(" + entries.size() + ")";
        return new SimpleStringProperty("java -jar " + arg);
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return super.isApplicable(model, entries) && FileTypeMenuProvider.super.isApplicable(model, entries);
    }

    @Override
    public BrowserIconFileType getType() {
        return BrowserIconFileType.byId("jar");
    }

    @Override
    public String getExecutable() {
        return "java";
    }

    @Override
    protected List<CommandBuilder> createCommand(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return entries.stream().map(browserEntry -> {
            return CommandBuilder.of()
                    .add("java", "-jar")
                    .addFile(browserEntry.getRawFileEntry().getPath());
        }).toList();
    }
}
