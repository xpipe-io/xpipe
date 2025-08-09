package io.xpipe.app.terminal;

import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.process.TerminalInitScriptConfig;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface TerminalMultiplexer {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(TmuxTerminalMultiplexer.class);
        l.add(ZellijTerminalMultiplexer.class);
        l.add(ScreenTerminalMultiplexer.class);
        return l;
    }

    String getDocsLink();

    void checkSupported(ShellControl sc) throws Exception;

    ShellScript launchForExistingSession(ShellControl control, String command, TerminalInitScriptConfig config);

    ShellScript launchNewSession(ShellControl control, String command, TerminalInitScriptConfig config);
}
