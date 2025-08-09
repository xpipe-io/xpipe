package io.xpipe.app.update;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.Hyperlinks;

import java.util.ArrayList;
import java.util.List;

public class CommandUpdater extends PortableUpdater {

    private final ShellScript script;

    public CommandUpdater(ShellScript script) {
        super(true);
        this.script = script;
    }

    @Override
    public boolean supportsDirectInstallation() {
        return true;
    }

    @Override
    public List<ModalButton> createActions() {
        var l = new ArrayList<ModalButton>();
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
            OperationMode.executeAfterShutdown(() -> {
                TerminalLaunch.builder().title("XPipe Updater").localScript(script).launch();
            });
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).handle();
            preparedUpdate.setValue(null);
        }
    }
}
