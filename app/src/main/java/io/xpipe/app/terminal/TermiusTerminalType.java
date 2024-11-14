package io.xpipe.app.terminal;

import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.SshLocalBridge;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.OsType;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TermiusTerminalType implements ExternalTerminalType {

    @Override
    public String getId() {
        return "app.termius";
    }

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.TABBED;
    }

    @Override
    public boolean isAvailable() {
        try (var sc = LocalShell.getShell()) {
            return switch (OsType.getLocal()) {
                case OsType.Linux linux -> {
                    yield Files.exists(Path.of("/opt/Termius"));
                }
                case OsType.MacOs macOs -> {
                    yield Files.exists(Path.of("/Applications/Termius.app"));
                }
                case OsType.Windows windows -> {
                    var r = WindowsRegistry.local().readValue(WindowsRegistry.HKEY_CURRENT_USER, "SOFTWARE\\Classes\\termius");
                    yield r.isPresent();
                }
            };
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).omit().handle();
            return false;
        }
    }

    @Override
    public String getWebsite() {
        return "https://termius.com/";
    }

    @Override
    public boolean isRecommended() {
        return false;
    }

    @Override
    public boolean supportsColoredTitle() {
        return true;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        SshLocalBridge.init();
        if (!showInfo()) {
            return;
        }

        var host = "localhost";
        var b = SshLocalBridge.get();
        var port = b.getPort();
        var user = b.getUser();
        var name = b.getIdentityKey().getFileName().toString();
        Hyperlinks.open("termius://app/host-sharing#label=" + name + "&ip=" + host + "&port=" + port + "&username=" + user + "&os=undefined");
    }

    private boolean showInfo() throws IOException {
        boolean set = AppCache.getBoolean("termiusSetup", false);
        if (set) {
            return true;
        }

        var b = SshLocalBridge.get();
        var keyContent = Files.readString(b.getIdentityKey());
        var r = AppWindowHelper.showBlockingAlert(alert -> {
            alert.setTitle(AppI18n.get("termiusSetup"));
            alert.setAlertType(Alert.AlertType.NONE);

            var activated = AppI18n.get().getMarkdownDocumentation("app:termiusSetup").formatted(b.getIdentityKey(), keyContent);
            var markdown = new MarkdownComp(activated, s -> s).prefWidth(450).prefHeight(450).createRegion();
            alert.getDialogPane().setContent(markdown);

            alert.getButtonTypes().add(new ButtonType(AppI18n.get("ok"), ButtonBar.ButtonData.OK_DONE));
        });
        r.filter(buttonType -> buttonType.getButtonData().isDefaultButton());
        r.ifPresent(buttonType -> {
            AppCache.update("termiusSetup", true);
        });
        return r.isPresent();
    }
}
