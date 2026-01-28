package io.xpipe.app.ext;

import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;

import java.util.LinkedHashMap;
import java.util.Map;

public class ShellDialectIcons {

    private static final Map<ShellDialect, String> ICONS = new LinkedHashMap<>();

    static {
        ICONS.put(ShellDialects.CMD, "cmd_icon.svg");
        ICONS.put(ShellDialects.POWERSHELL, "powershell_icon.svg");
        ICONS.put(ShellDialects.POWERSHELL_CORE, "pwsh_icon.svg");
        ICONS.put(ShellDialects.SH, "sh_icon.svg");
        ICONS.put(ShellDialects.ASH, "sh_icon.svg");
        ICONS.put(ShellDialects.DASH, "sh_icon.svg");
        ICONS.put(ShellDialects.BASH, "bash_icon.svg");
        ICONS.put(ShellDialects.FISH, "fish_icon.svg");
        ICONS.put(ShellDialects.ZSH, "zsh_icon.svg");
        ICONS.put(ShellDialects.NUSHELL, "nushell_icon.svg");
        ICONS.put(ShellDialects.XONSH, "xonsh_icon.svg");
    }

    public static String getImageName(ShellDialect t) {
        if (t == null) {
            return "proc:defaultShell_icon.svg";
        }

        return "proc:" + ICONS.get(t);
    }
}
