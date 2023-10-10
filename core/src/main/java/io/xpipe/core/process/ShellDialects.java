package io.xpipe.core.process;

import io.xpipe.core.util.ModuleLayerLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class ShellDialects {

    public static final List<ShellDialect> ALL = new ArrayList<>();
    public static ShellDialect OPNSENSE;
    public static ShellDialect POWERSHELL;
    public static ShellDialect POWERSHELL_CORE;
    public static ShellDialect CMD;
    public static ShellDialect ASH;
    public static ShellDialect SH;
    public static ShellDialect DASH;
    public static ShellDialect BASH;
    public static ShellDialect ZSH;
    public static ShellDialect CSH;
    public static ShellDialect FISH;

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ServiceLoader.load(layer, ShellDialect.class).stream().forEach(moduleLayerLoaderProvider -> {
                ALL.add(moduleLayerLoaderProvider.get());
            });

            CMD = byName("cmd");
            POWERSHELL = byName("powershell");
            POWERSHELL_CORE = byName("pwsh");
            OPNSENSE = byName("opnsense");
            FISH = byName("fish");
            DASH = byName("dash");
            BASH = byName("bash");
            ZSH = byName("zsh");
            CSH = byName("csh");
            ASH = byName("ash");
            SH = byName("sh");
        }

        @Override
        public boolean requiresFullDaemon() {
            return false;
        }

        @Override
        public boolean prioritizeLoading() {
            return true;
        }
    }

    private static ShellDialect byName(String name) {
        return ALL.stream()
                .filter(shellType -> shellType.getId().equals(name))
                .findFirst()
                .orElseThrow();
    }

    public static ShellDialect getPlatformDefault() {
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return CMD;
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return BASH;
        } else {
            return ZSH;
        }
    }
}
