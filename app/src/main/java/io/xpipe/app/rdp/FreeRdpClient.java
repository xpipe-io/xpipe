package io.xpipe.app.rdp;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.util.FlatpakCache;
import io.xpipe.app.util.OsType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@JsonTypeName("freeRdp")
@Value
@Jacksonized
@Builder
public class FreeRdpClient implements ExternalRdpClient {

    @Value
    private static class Executable {

        CommandBuilder commandBase;
        boolean v3;
    }

    private Executable getX11CommandBase() throws Exception {
        CommandBuilder exec;
        var v3 = CommandSupport.isInLocalPath("xfreerdp3");
        if (!v3) {
            var v2 = CommandSupport.isInLocalPath("xfreerdp");
            if (!v2 && OsType.ofLocal() == OsType.LINUX) {
                var flatpak = FlatpakCache.getApp("com.freerdp.FreeRDP");
                if (flatpak.isPresent()) {
                    exec = FlatpakCache.getRunCommand("com.freerdp.FreeRDP");
                    v3 = true;
                } else {
                    return null;
                }
            } else {
                CommandSupport.isInPathOrThrow(LocalShell.getShell(), "xfreerdp");
                exec = CommandBuilder.of().add("xfreerdp");
                // macOS uses xfreerdp3 by default
                v3 = true;
            }
        } else {
            exec = CommandBuilder.of().add("xfreerdp3");
        }
        return new Executable(exec, v3);
    }

    private Executable getWaylandCommandBase() throws Exception {
        CommandBuilder exec;
        var v3 = CommandSupport.isInLocalPath("wlfreerdp3");
        if (!v3) {
            var v2 = CommandSupport.isInLocalPath("wlfreerdp");
            if (!v2 && OsType.ofLocal() == OsType.LINUX) {
                var flatpak = FlatpakCache.getApp("com.freerdp.FreeRDP");
                if (flatpak.isPresent()) {
                    exec = FlatpakCache.getRunCommand("com.freerdp.FreeRDP", "sdl-freerdp");
                    v3 = true;
                } else {
                    return null;
                }
            } else {
                // No wayland build exists on macOS
                return null;
            }
        } else {
            exec = CommandBuilder.of().add("wlfreerdp3");
        }
        return new Executable(exec, v3);
    }

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var preferWayland = OsType.ofLocal() == OsType.LINUX && "wayland".equalsIgnoreCase(System.getenv("XDG_SESSION_TYPE"));
        var exec = preferWayland ? getWaylandCommandBase() : getX11CommandBase();
        if (exec == null && preferWayland) {
            exec = getX11CommandBase();
        }

        if (exec == null) {
            throw ErrorEventFactory.expected(new IllegalStateException("Unable to find a FreeRDP v2 or v3 installation"));
        }

        var file = writeRdpConfigFile(configuration.getTitle(), configuration.getConfig());
        var b = CommandBuilder.of()
                .add(exec.getCommandBase())
                .addFile(file.toString())
                .add(exec.isV3() ? "/cert:ignore" : "/cert-ignore")
                .add("/dynamic-resolution")
                .add("/network:auto")
                .add("/compression")
                .add("+clipboard")
                .add("-themes")
                .add("/size:100%");

        if (configuration.getPassword() != null) {
            b.add(argument("/p", configuration.getPassword().getSecretValue()));
        }

        var gateway = configuration.getGateway();
        if (gateway != null) {
            if (exec.isV3()) {
                // Horrible quoting rules: https://github.com/FreeRDP/FreeRDP/issues/11396
                String s = "g:" + gateway.getHost();
                if (gateway.getUsername() != null) {
                    s += "," + gatewayArgument("u", gateway.getUsername());
                }
                if (gateway.getPassword() != null) {
                    s += "," + gatewayArgument("p", gateway.getPassword().getSecretValue());
                }
                b.add(gatewayArgument("/gateway", s));
            } else {
                b.add(argument("/g", gateway.getHost()));
                if (gateway.getUsername() != null) {
                    b.add(argument("/gu", gateway.getUsername()));
                }
                if (gateway.getPassword() != null) {
                    b.add(argument("/gp", gateway.getPassword().getSecretValue()));
                }
            }
        }

        b.fixedEnvironment("FREERDP_ASKPASS", AppInstallation.ofCurrent().getCliExecutablePath().toString());

        try (var sc = LocalShell.getShell().start()) {
            var cmd = sc.getShellDialect().launchAsync(b, true);
            sc.command(cmd).sensitive().execute();
        }
    }

    private String gatewayArgument(String key, String value) {
        var escaped = value.replaceAll("\"", "\\\\\"");
        return "\"" + key + ":" + escaped + "\"";
    }

    private String argument(String key, String value) {
        var escaped = value.replaceAll("'", "\\\\'");
        return key + ":'" + escaped + "'";
    }

    @Override
    public boolean supportsPasswordPassing(RdpLaunchConfig config) {
        return true;
    }

    @Override
    public String getWebsite() {
        return "https://www.freerdp.com/";
    }
}
