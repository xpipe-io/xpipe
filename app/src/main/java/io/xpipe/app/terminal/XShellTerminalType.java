package io.xpipe.app.terminal;

import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.SshLocalBridge;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.CommandBuilder;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class XShellTerminalType extends ExternalTerminalType.WindowsType {

    public XShellTerminalType() {
        super("app.xShell", "Xshell");
    }

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.TABBED;
    }

    @Override
    protected Optional<Path> determineInstallation() {
        try {
            var r = WindowsRegistry.local()
                    .readValue(
                            WindowsRegistry.HKEY_LOCAL_MACHINE,
                            "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\Xshell.exe");
            return r.map(Path::of);
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).omit().handle();
            return Optional.empty();
        }
    }

    @Override
    public String getWebsite() {
        return "https://www.netsarang.com/en/xshell/";
    }

    @Override
    public boolean isRecommended() {
        return false;
    }

    @Override
    public boolean supportsColoredTitle() {
        return false;
    }

    @Override
    protected void execute(Path file, TerminalLaunchConfiguration configuration) throws Exception {
        SshLocalBridge.init();
        if (!showInfo()) {
            return;
        }

        try (var sc = LocalShell.getShell()) {
            var b = SshLocalBridge.get();
            var keyName = b.getIdentityKey().getFileName().toString();
            var command = CommandBuilder.of()
                    .addFile(file.toString())
                    .add("-url")
                    .addQuoted("ssh://" + b.getUser() + "@localhost:" + b.getPort())
                    .add("-i", keyName);
            sc.executeSimpleCommand(command);
        }
    }

    private boolean showInfo() {
        boolean set = AppCache.getBoolean("xshellSetup", false);
        if (set) {
            return true;
        }

        var b = SshLocalBridge.get();
        var keyName = b.getIdentityKey().getFileName().toString();
        var activated = AppI18n.get()
                .getMarkdownDocumentation("app:xshellSetup")
                .formatted(b.getIdentityKey(), keyName);
        var modal = ModalOverlay.of("xshellSetup", new MarkdownComp(activated, s -> s).prefWidth(450));
        modal.addButton(ModalButton.ok(() -> {
            AppCache.update("xshellSetup", true);
        }));
        modal.showAndWait();
        return AppCache.getBoolean("xshellSetup", false);
    }
}
