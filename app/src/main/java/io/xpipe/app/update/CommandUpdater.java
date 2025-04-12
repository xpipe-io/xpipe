package io.xpipe.app.update;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;

import java.nio.file.Files;
import java.util.List;

public class CommandUpdater extends PortableUpdater {

    private final CommandBuilder command;

    public CommandUpdater(CommandBuilder command) {
        super(true);
        this.command = command;
    }

    @Override
    public boolean supportsDirectInstallation() {
        return true;
    }

    @Override
    public List<ModalButton> createActions() {
        var l = super.createActions();
        l.add(new ModalButton("ignore", null, true, false));
        l.add(new ModalButton(
                "checkOutUpdate",
                () -> {
                    if (getPreparedUpdate().getValue() == null) {
                        return;
                    }

                    Hyperlinks.open(getPreparedUpdate().getValue().getReleaseUrl());
                },
                false,
                false));
        l.add(new ModalButton(
                "install",
                () -> {
                    executeUpdateAndClose();
                },
                true,
                true));
        return l;
    }

    @Override
    public void executeUpdate() {
        try {
            var p = preparedUpdate.getValue();
            var performedUpdate = new PerformedUpdate(p.getVersion(), p.getBody(), p.getVersion());
            AppCache.update("performedUpdate", performedUpdate);
            TerminalLauncher.openDirect("Update", command);
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).handle();
            preparedUpdate.setValue(null);
        }
    }
}
