package io.xpipe.app.terminal;

import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.process.TerminalInitScriptConfig;

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

    default void checkComplete() {}

    String getDocsLink();

    void checkSupported(ShellControl sc) throws Exception;

    ShellScript launchScriptExternal(ShellControl control, String command, TerminalInitScriptConfig config);

    ShellScript launchScriptSession(ShellControl control, String command, TerminalInitScriptConfig config);
}
