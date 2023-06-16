package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionFormatter;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.core.impl.FileNames;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.stream.Collectors;

public class CopyPathAction implements BrowserAction, BranchAction {

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "Copy location";
    }

    @Override
    public Category getCategory() {
        return Category.COPY_PASTE;
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public List<LeafAction> getBranchingActions() {
        return List.of(
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return " "
                                    + BrowserActionFormatter.centerEllipsis(
                                            entries.get(0).getRawFileEntry().getPath(), 50);
                        }

                        return "Absolute Path";
                    }

                    @Override
                    public KeyCombination getShortcut() {
                        return new KeyCodeCombination(KeyCode.C, KeyCombination.ALT_DOWN, KeyCombination.SHORTCUT_DOWN);
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath())
                                .collect(Collectors.joining("\n"));
                        var selection = new StringSelection(s);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selection, selection);
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return "\""
                                    + BrowserActionFormatter.centerEllipsis(
                                            entries.get(0).getRawFileEntry().getPath(), 50)
                                    + "\"";
                        }

                        return "Absolute Path (Quoted)";
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return entries.stream()
                                .anyMatch(entry ->
                                        entry.getRawFileEntry().getPath().contains(" "));
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> "\"" + entry.getRawFileEntry().getPath() + "\"")
                                .collect(Collectors.joining("\n"));
                        var selection = new StringSelection(s);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selection, selection);
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return " "
                                    + BrowserActionFormatter.centerEllipsis(
                                            FileNames.getFileName(entries.get(0)
                                                    .getRawFileEntry()
                                                    .getPath()),
                                            50);
                        }

                        return "File Name";
                    }

                    @Override
                    public KeyCombination getShortcut() {
                        return new KeyCodeCombination(
                                KeyCode.C, KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN);
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> FileNames.getFileName(
                                        entry.getRawFileEntry().getPath()))
                                .collect(Collectors.joining("\n"));
                        var selection = new StringSelection(s);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selection, selection);
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return "\""
                                    + BrowserActionFormatter.centerEllipsis(
                                            FileNames.getFileName(entries.get(0)
                                                    .getRawFileEntry()
                                                    .getPath()),
                                            50)
                                    + "\"";
                        }

                        return "File Name (Quoted)";
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return entries.stream().anyMatch(entry -> FileNames.getFileName(
                                        entry.getRawFileEntry().getPath())
                                .contains(" "));
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> "\""
                                        + FileNames.getFileName(
                                                entry.getRawFileEntry().getPath()) + "\"")
                                .collect(Collectors.joining("\n"));
                        var selection = new StringSelection(s);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selection, selection);
                    }
                });
    }
}
