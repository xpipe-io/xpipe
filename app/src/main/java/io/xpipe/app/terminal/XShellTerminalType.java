package io.xpipe.app.terminal;

import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.SshLocalBridge;
import io.xpipe.app.util.WindowsRegistry;

import java.nio.file.Path;
import java.util.Optional;

public class XShellTerminalType implements ExternalApplicationType.WindowsType, ExternalTerminalType {

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.TABBED;
    }

    @Override
    public boolean detach() {
        return false;
    }

    @Override
    public String getExecutable() {
        return "Xshell";
    }

    @Override
    public Optional<Path> determineInstallation() {
        try {
            var r = WindowsRegistry.local()
                    .readStringValueIfPresent(
                            WindowsRegistry.HKEY_LOCAL_MACHINE,
                            "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\Xshell.exe");
            return r.map(Path::of);
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).omit().handle();
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
    public boolean useColoredTitle() {
        return false;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        SshLocalBridge.init();
        if (!showInfo()) {
            return;
        }

        var b = SshLocalBridge.get();
        var keyName = b.getIdentityKey().getFileName().toString();
        var command = CommandBuilder.of()
                .add("-url")
                .addQuoted("ssh://" + b.getUser() + "@localhost:" + b.getPort())
                .add("-i", keyName);
        launch(command);
    }

    private boolean showInfo() {
        boolean set = AppCache.getBoolean("xshellSetup", false);
        if (set) {
            return true;
        }

        var b = SshLocalBridge.get();
        var keyName = b.getIdentityKey().getFileName().toString();
        var activated =
                AppI18n.get().getMarkdownDocumentation("app:xshellSetup").formatted(b.getIdentityKey(), keyName);
        var modal = ModalOverlay.of("xshellSetup", new MarkdownComp(activated, s -> s, false).prefWidth(450));
        modal.addButton(ModalButton.ok(() -> {
            AppCache.update("xshellSetup", true);
        }));
        modal.showAndWait();
        return AppCache.getBoolean("xshellSetup", false);
    }

    @Override
    public String getId() {
        return "app.xShell";
    }
}
