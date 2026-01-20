package io.xpipe.ext.base.script;

import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;

import java.util.List;

public class ScriptDialects {

    public static List<ShellDialect> getSupported() {
        var availableDialects = List.of(
                ShellDialects.SH,
                ShellDialects.BASH,
                ShellDialects.ZSH,
                ShellDialects.FISH,
                ShellDialects.CMD,
                ShellDialects.POWERSHELL,
                ShellDialects.POWERSHELL_CORE);
        return availableDialects;
    }
}
