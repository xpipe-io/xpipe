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
    public static ShellDialect NUSHELL;
    public static ShellDialect XONSH;

    public static ShellDialect NO_INTERACTION;
    public static ShellDialect CISCO;
    public static ShellDialect MIKROTIK;
    public static ShellDialect PALO_ALTO;
    public static ShellDialect RBASH;
    public static ShellDialect CONSTRAINED_POWERSHELL;
    public static ShellDialect OVH_BASTION;
    public static ShellDialect HETZNER_BOX;
    public static ShellDialect JUMP_SERVER;

    public static List<ShellDialect> getStartableDialects() {
        return ALL.stream()
                .filter(dialect ->
                        dialect.getDumbMode().supportsAnyPossibleInteraction() && dialect.getLaunchCommand() != null)
                .toList();
    }

    private static ShellDialect byId(String name) {
        return ALL.stream()
                .filter(shellType -> shellType.getId().equals(name))
                .findFirst()
                .orElseThrow();
    }

    public static boolean isPowershell(ShellControl sc) {
        if (sc.getShellDialect() == null) {
            return false;
        }

        return isPowershell(sc.getShellDialect());
    }

    public static boolean isPowershell(ShellDialect d) {
        return d == POWERSHELL || d == POWERSHELL_CORE;
    }

    public static Optional<ShellDialect> byNameIfPresent(String name) {
        return ALL.stream().filter(shellType -> shellType.getId().equals(name)).findFirst();
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            var services = layer != null
                    ? ServiceLoader.load(layer, ShellDialect.class)
                    : ServiceLoader.load(ShellDialect.class);
            services.stream().forEach(moduleLayerLoaderProvider -> {
                ALL.add(moduleLayerLoaderProvider.get());
            });

            if (ALL.isEmpty()) {
                return;
            }

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
            NUSHELL = byId("nushell");
            XONSH = byId("xonsh");
            NO_INTERACTION = byId("noInteraction");
            CISCO = byId("cisco");
            MIKROTIK = byId("mikrotik");
            PALO_ALTO = byId("paloAlto");
            RBASH = byId("rbash");
            CONSTRAINED_POWERSHELL = byId("constrainedPowershell");
            OVH_BASTION = byId("ovhBastion");
            HETZNER_BOX = byId("hetznerBox");
            JUMP_SERVER = byId("jumpServer");
        }
    }
}
