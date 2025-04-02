package io.xpipe.app.terminal;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.GithubReleaseDownloader;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;
import javafx.beans.property.Property;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
@ToString
@Jacksonized
@JsonTypeName("starship")
public class OhMyPoshTerminalPrompt extends ConfigFileTerminalPrompt {

    public static OptionsBuilder createOptions(Property<OhMyPoshTerminalPrompt> p) {
        return createOptions(p, "toml", s -> OhMyPoshTerminalPrompt.builder().configuration(s).build());
    }

    @Override
    public String getDocsLink() {
        return "https://starship.rs/guide/";
    }

    @Override
    public String getId() {
        return "starship";
    }

    @Override
    public void checkCanInstall(ShellControl sc) throws Exception {
        if (sc.getOsType() != OsType.WINDOWS) {
            CommandSupport.isInPathOrThrow(sc, "curl");
        }
    }

    @Override
    public boolean checkIfInstalled(ShellControl sc) throws Exception {
        if (sc.getShellDialect() == ShellDialects.CMD && !ClinkHelper.checkIfInstalled(sc)) {
            return false;
        }

        if (sc.view().findProgram("starship").isPresent()) {
            return true;
        }

        var extension = OsType.getLocal() == OsType.WINDOWS ? ".exe" : "";
        return sc.view().fileExists(getBinaryDirectory(sc).join("starship" + extension));
    }

    @Override
    public void install(ShellControl sc) throws Exception {
        if (sc.getShellDialect() == ShellDialects.CMD) {
            ClinkHelper.install(sc);
            var configDir = getConfigurationDirectory(sc);
            sc.view().mkdir(configDir);
            sc.view().writeTextFile(configDir.join("starship.lua"), "load(io.popen('starship init cmd'):read(\"*a\"))()");
        }

        var dir = getBinaryDirectory(sc);
        sc.view().mkdir(dir);
        if (sc.getOsType() == OsType.WINDOWS) {
            var file = GithubReleaseDownloader.getDownloadTempFile(
                    "JanDeDobbeleer/oh-my-posh",
                    "posh-windows-amd64.exe",
                    s -> s.equals("posh-windows-amd64.exe"));
            sc.view().transferLocalFile(file, dir.join("starship.exe"));
        } else {
            sc.command("curl -sS https://starship.rs/install.sh | sh /dev/stdin -y --bin-dir \"" + dir + "\" > /dev/null").execute();
        }
    }

    @Override
    public FilePath prepareCustomConfigFile(ShellControl sc) throws Exception {
        var file = getConfigurationDirectory(sc).join("starship.toml");
        sc.view().writeTextFile(file, configuration);
        return file;
    }

    @Override
    public FilePath getDefaultConfigFile(ShellControl sc) throws Exception {
        return sc.view().userHome().join(".config").join("starship.toml");
    }

    @Override
    protected ShellScript setupTerminalCommand(ShellControl shellControl, FilePath config) throws Exception {
        var lines = new ArrayList<String>();
        if (shellControl.getShellDialect() == ShellDialects.CMD) {
            lines.add(shellControl.getShellDialect().addToPathVariableCommand(List.of(ClinkHelper.getTargetDir(shellControl).toString()), false));
        }
        lines.add(shellControl.getShellDialect().getSetEnvironmentVariableCommand("STARSHIP_CONFIG", config.toString()));
        if (shellControl.getShellDialect() == ShellDialects.CMD) {
            lines.add("clink inject --quiet --profile \"" + getConfigurationDirectory(shellControl) + "\"");
        } else if (ShellDialects.isPowershell(shellControl)) {
            lines.add("Invoke-Expression (&starship init powershell)");
        } else if (shellControl.getShellDialect() == ShellDialects.FISH) {
            lines.add("starship init fish | source");
        } else {
            lines.add("eval \"$(starship init " + shellControl.getShellDialect().getId() + ")\"");
        }
        return ShellScript.lines(lines);
    }

    @Override
    public List<ShellDialect> getSupportedDialects() {
        return List.of(ShellDialects.BASH, ShellDialects.ZSH, ShellDialects.FISH, ShellDialects.CMD, ShellDialects.POWERSHELL, ShellDialects.POWERSHELL_CORE);
    }
}
