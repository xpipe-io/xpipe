package io.xpipe.app.terminal;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.password.KeePassXcAssociationKey;
import io.xpipe.app.password.KeePassXcManager;
import io.xpipe.app.password.KeePassXcProxyClient;
import io.xpipe.app.password.PasswordManager;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.store.FilePath;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder(toBuilder = true)
@ToString
@Jacksonized
@JsonTypeName("zellij")
public class ZellijTerminalMultiplexer implements TerminalMultiplexer {

    private final String wslDistribution;
    private final FilePath config;

    public static OptionsBuilder createOptions(Property<ZellijTerminalMultiplexer> p) {
        var config = new SimpleObjectProperty<FilePath>(p.getValue() != null ? p.getValue().getConfig() : null);
        return new OptionsBuilder()
                .addProperty(config)
                .bind(() -> {
                    return null; //new ZellijTerminalMultiplexer(config.getValue());
                }, p);
    }

    @Override
    public String getDocsLink() {
        return "";
    }

    @Override
    public ShellScript launchScriptExternal(String command) throws Exception {
        return ShellScript.lines(
                "zellij attach --create-background xpipe",
                "zellij run --close-on-exit -- " + command
        );
    }

    @Override
    public ShellScript launchScriptSession(String command) throws Exception {
        return ShellScript.lines(
                "zellij attach --create-background xpipe",
                "zellij run --close-on-exit -- " + command,
                "zellij attach xpipe"
        );
    }
}
