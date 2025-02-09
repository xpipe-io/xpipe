package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserBranchAction;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ProcessOutputException;
import io.xpipe.core.process.ShellControl;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public abstract class MultiExecuteSelectionAction implements BrowserBranchAction {

    protected abstract CommandBuilder createCommand(
            ShellControl sc, BrowserFileSystemTabModel model, List<BrowserEntry> entries);

    protected abstract String getTerminalTitle();

    @Override
    public List<BrowserLeafAction> getBranchingActions(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return List.of(
                new BrowserLeafAction() {

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    var c = createCommand(pc, model, entries);
                                    if (c == null) {
                                        return;
                                    }

                                    var cmd = pc.command(c);
                                    model.openTerminalAsync(
                                            getTerminalTitle(),
                                            model.getCurrentDirectory() != null
                                                    ? model.getCurrentDirectory()
                                                            .getPath()
                                                    : null,
                                            cmd,
                                            true);
                                },
                                false);
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var t = AppPrefs.get().terminalType().getValue();
                        return AppI18n.observable(
                                "executeInTerminal",
                                t != null ? t.toTranslatedString().getValue() : "?");
                    }

                    @Override
                    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppPrefs.get().terminalType().getValue() != null;
                    }
                },
                new BrowserLeafAction() {

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    var c = createCommand(pc, model, entries);
                                    if (c == null) {
                                        return;
                                    }

                                    var cmd = pc.command(c);
                                    String out;
                                    try {
                                        out = cmd.readStdoutOrThrow();
                                        if (out.isEmpty()) {
                                            out = "<empty>";
                                        }

                                        if (out.length() > 10000) {
                                            var counter = new AtomicInteger();
                                            var start = out.lines().filter(s -> {
                                                counter.incrementAndGet();
                                                return true;
                                                    })
                                                    .limit(100)
                                                    .collect(Collectors.joining("\n"));
                                            var notShownLines = counter.get() - 100;
                                            if (notShownLines > 0) {
                                                out = start + "\n\n... " + notShownLines + " more lines";
                                            } else {
                                                out = start;
                                            }
                                        }

                                    } catch (ProcessOutputException e) {
                                        out = e.getMessage();
                                    }

                                    String finalOut = out;
                                    var modal = ModalOverlay.of("commandOutput", Comp.of(() -> {
                                                var text = new TextArea(finalOut);
                                                text.setWrapText(true);
                                                text.setEditable(false);
                                                text.setPrefRowCount(Math.max(8,( int) finalOut.lines().count()));
                                                var sp = new StackPane(text);
                                                return sp;
                                            })
                                            .prefWidth(650));
                                    modal.show();
                                },
                                true);
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("runCommand");
                    }
                },
                new BrowserLeafAction() {

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    var cmd = createCommand(pc, model, entries);
                                    AtomicReference<String> out = new AtomicReference<>();
                                    AtomicReference<String> err = new AtomicReference<>();
                                    long exitCode;
                                    try (var command = pc.command(cmd)
                                            .withWorkingDirectory(
                                                    model.getCurrentDirectory().getPath())
                                            .start()) {
                                        var r = command.readStdoutAndStderr();
                                        out.set(r[0]);
                                        err.set(r[1]);
                                        exitCode = command.getExitCode();
                                    }
                                    // Only throw actual error output
                                    if (exitCode != 0) {
                                        throw ErrorEvent.expected(
                                                ProcessOutputException.of(exitCode, out.get(), err.get()));
                                    }
                                },
                                false);
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("runSilent");
                    }
                });
    }
}
