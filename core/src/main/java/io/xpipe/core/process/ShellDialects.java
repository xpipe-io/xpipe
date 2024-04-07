package io.xpipe.core.process;

import io.xpipe.core.util.ModuleLayerLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public class ShellDialects {

    public static final List<ShellDialect> ALL = new ArrayList<>();
    public static ShellDialect OPNSENSE;
    public static ShellDialect PFSENSE;
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

    public static ShellDialect CISCO;
    public static ShellDialect MIKROTIK;
    public static ShellDialect RBASH;
    public static ShellDialect OVH_BASTION;

    public static List<ShellDialect> getStartableDialects() {
        return ALL.stream()
                .filter(dialect -> dialect.getOpenCommand(null) != null)
                .toList();
    }

    private static ShellDialect byId(String name) {
        return ALL.stream()
                .filter(shellType -> shellType.getId().equals(name))
                .findFirst()
                .orElseThrow();
    }

    public static boolean isPowershell(ShellControl sc) {
        return sc.getShellDialect().equals(POWERSHELL) || sc.getShellDialect().equals(POWERSHELL_CORE);
    }

    public static ShellDialect byName(String name) {
        return byNameIfPresent(name).orElseThrow();
    }

    public static Optional<ShellDialect> byNameIfPresent(String name) {
        return ALL.stream().filter(shellType -> shellType.getId().equals(name)).findFirst();
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ServiceLoader.load(layer, ShellDialect.class).stream().forEach(moduleLayerLoaderProvider -> {
                ALL.add(moduleLayerLoaderProvider.get());
            });

            CMD = byId("cmd");
            POWERSHELL = byId("powershell");
            POWERSHELL_CORE = byId("pwsh");
            OPNSENSE = byId("opnsense");
            PFSENSE = byId("pfsense");
            FISH = byId("fish");
            DASH = byId("dash");
            BASH = byId("bash");
            ZSH = byId("zsh");
            CSH = byId("csh");
            ASH = byId("ash");
            SH = byId("sh");
            CISCO = byId("cisco");
            MIKROTIK = byId("mikrotik");
            RBASH = byId("rbash");
            OVH_BASTION = byId("ovhBastion");
        }
    }
}
