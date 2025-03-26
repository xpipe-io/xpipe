package io.xpipe.app.terminal;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.password.KeePassXcAssociationKey;
import io.xpipe.app.password.KeePassXcManager;
import io.xpipe.app.password.KeePassXcProxyClient;
import io.xpipe.app.password.PasswordManager;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.process.TerminalInitScriptConfig;
import io.xpipe.core.store.FilePath;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@JsonTypeName("zellij")
public class ZellijTerminalMultiplexer implements TerminalMultiplexer {

    @Override
    public String getDocsLink() {
        return "https://zellij.dev/documentation/creating-a-layout.html#default-tab-template";
    }

    @Override
    public void checkSupported(ShellControl sc) throws Exception {
        CommandSupport.isInPathOrThrow(sc, "zellij");
    }

    @Override
    public ShellScript launchScriptExternal(ShellControl control, String command, TerminalInitScriptConfig config) throws Exception {
        return ShellScript.lines(
                "zellij attach --create-background xpipe",
                "zellij -s xpipe action new-tab --name \"" + escape(config.getDisplayName(), false, true) + "\"",
                "zellij -s xpipe action write-chars -- " + escape(command, true, true),
                "zellij -s xpipe action write 10"
        );
    }

    @Override
    public ShellScript launchScriptSession(ShellControl control, String command, TerminalInitScriptConfig config) throws Exception {
        return ShellScript.lines(
                "zellij delete-session -f xpipe",
                "zellij attach --create-background xpipe",
                "zellij -s xpipe run --name \"" + escape(config.getDisplayName(), false, true) + "\" -- " + escape(command, false, false),
                "zellij attach xpipe"
        );
    }

    private String escape(String s, boolean spaces, boolean quotes) {
        var r = s.replaceAll("\\\\", "\\\\\\\\");
        if (quotes) {
            r = r.replaceAll("\"", "\\\\\"");
        }
        if (spaces) {
            r = r.replaceAll(" ", "\\\\ ");
        }
        return r;
    }
}
