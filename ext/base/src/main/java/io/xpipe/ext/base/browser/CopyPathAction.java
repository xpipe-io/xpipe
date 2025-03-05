package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionFormatter;
import io.xpipe.app.browser.action.BrowserBranchAction;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.ClipboardHelper;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileNames;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.List;
import java.util.stream.Collectors;

public class CopyPathAction implements BrowserAction, BrowserBranchAction {

    @Override
    public Category getCategory() {
        return Category.COPY_PASTE;
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("copyLocation");
    }

    @Override
    public List<BrowserLeafAction> getBranchingActions(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return List.of(
                new BrowserLeafAction() {
                    @Override
                    public KeyCombination getShortcut() {
                        return new KeyCodeCombination(KeyCode.C, KeyCombination.ALT_DOWN, KeyCombination.SHORTCUT_DOWN);
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>(BrowserActionFormatter.centerEllipsis(
                                    entries.getFirst().getRawFileEntry().getPath().toString(), 50));
                        }

                        return AppI18n.observable("absolutePaths");
                    }

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath().toString())
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new BrowserLeafAction() {
                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>(BrowserActionFormatter.centerEllipsis(
                                    entries.getFirst().getRawFileEntry().getPath().toString(), 50));
                        }

                        return AppI18n.observable("absoluteLinkPaths");
                    }

                    @Override
                    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return entries.stream()
                                .allMatch(browserEntry ->
                                        browserEntry.getRawFileEntry().getKind() == FileKind.LINK);
                    }

                    @Override
                    public boolean automaticallyResolveLinks() {
                        return false;
                    }

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath().toString())
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new BrowserLeafAction() {
                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>("\""
                                    + BrowserActionFormatter.centerEllipsis(
                                            entries.getFirst().getRawFileEntry().getPath().toString(), 50)
                                    + "\"");
                        }

                        return AppI18n.observable("absolutePathsQuoted");
                    }

                    @Override
                    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return entries.stream()
                                .anyMatch(entry ->
                                        entry.getRawFileEntry().getPath().toString().contains(" "));
                    }

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> "\"" + entry.getRawFileEntry().getPath() + "\"")
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new BrowserLeafAction() {
                    @Override
                    public KeyCombination getShortcut() {
                        return new KeyCodeCombination(
                                KeyCode.C, KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN);
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>(BrowserActionFormatter.centerEllipsis(
                                    entries.getFirst().getRawFileEntry().getPath().getFileName(),
                                    50));
                        }

                        return AppI18n.observable("fileNames");
                    }

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath().getFileName())
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new BrowserLeafAction() {
                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>(BrowserActionFormatter.centerEllipsis(
                                    entries.getFirst().getRawFileEntry().getPath().getFileName(),
                                    50));
                        }

                        return AppI18n.observable("linkFileNames");
                    }

                    @Override
                    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return entries.stream()
                                        .allMatch(browserEntry ->
                                                browserEntry.getRawFileEntry().getKind() == FileKind.LINK)
                                && entries.stream().anyMatch(browserEntry -> !browserEntry
                                        .getFileName()
                                        .equals(browserEntry
                                                .getRawFileEntry()
                                                .resolved()
                                                .getPath().getFileName()));
                    }

                    @Override
                    public boolean automaticallyResolveLinks() {
                        return false;
                    }

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath().getFileName())
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new BrowserLeafAction() {
                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>("\""
                                    + BrowserActionFormatter.centerEllipsis(entries.getFirst()
                                                    .getRawFileEntry()
                                                    .getPath().getFileName(),
                                            50)
                                    + "\"");
                        }

                        return AppI18n.observable("fileNamesQuoted");
                    }

                    @Override
                    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return entries.stream().anyMatch(entry -> entry.getRawFileEntry().getPath().getFileName()
                                .contains(" "));
                    }

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> "\""
                                        + entry.getRawFileEntry().getPath().getFileName() + "\"")
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                });
    }
}
