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

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
@ToString
@Jacksonized
@JsonTypeName("starship")
public class StarshipTerminalPrompt extends ConfigFileTerminalPrompt {

    public static OptionsBuilder createOptions(Property<StarshipTerminalPrompt> p) {
        return createOptions(p, s -> StarshipTerminalPrompt.builder().configuration(s).build());
    }

    public static StarshipTerminalPrompt createDefault() {
        return StarshipTerminalPrompt.builder().configuration(
                """
# Get editor completions based on the config schema
"$schema" = 'https://starship.rs/config-schema.json'

# Inserts a blank line between shell prompts
add_newline = true

# Replace the '❯' symbol in the prompt with '➜'
[character] # The name of the module we are configuring is 'character'
success_symbol = '[➜](bold green)' # The 'success_symbol' segment is being set to '➜' with the color 'bold green'

# Disable the package module, hiding it from the prompt completely
[package]
disabled = true
                """
        ).build();
    }

    @Override
    protected String getConfigFileExtension() {
        return "toml";
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

        var extension = sc.getOsType() == OsType.WINDOWS ? ".exe" : "";
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
            var file = GithubReleaseDownloader.getDownloadTempFile("starship/starship",
                    "starship-x86_64-pc-windows-msvc.zip",
                    s -> s.equals("starship-x86_64-pc-windows-msvc.zip"));
            try (var fs = FileSystems.newFileSystem(file)) {
                var exeFile = fs.getPath("starship.exe");
                sc.view().transferLocalFile(exeFile, dir.join("starship.exe"));
            }
        } else {
            sc.command("curl -sS https://starship.rs/install.sh | sh /dev/stdin -y --bin-dir \"" + dir + "\"").execute();
        }
    }

    @Override
    protected ShellScript setupTerminalCommand(ShellControl shellControl, FilePath config) throws Exception {
        var lines = new ArrayList<String>();
        var dialect = shellControl.getOriginalShellDialect();
        if (dialect == ShellDialects.CMD) {
            lines.add(dialect.addToPathVariableCommand(List.of(ClinkHelper.getTargetDir(shellControl).toString()), false));
        }
        if (config != null) {
            lines.add(dialect.getSetEnvironmentVariableCommand("STARSHIP_CONFIG", config.toString()));
        }
        if (dialect == ShellDialects.CMD) {
            lines.add("clink inject --quiet --profile \"" + getConfigurationDirectory(shellControl) + "\"");
        } else if (ShellDialects.isPowershell(shellControl)) {
            lines.add("Invoke-Expression (&starship init powershell)");
        } else if (dialect == ShellDialects.FISH) {
            lines.add("starship init fish | source");
        } else {
            lines.add("eval \"$(starship init " + dialect.getId() + ")\"");
        }
        return ShellScript.lines(lines);
    }

    @Override
    public List<ShellDialect> getSupportedDialects() {
        return List.of(ShellDialects.BASH, ShellDialects.ZSH, ShellDialects.FISH, ShellDialects.CMD, ShellDialects.POWERSHELL, ShellDialects.POWERSHELL_CORE);
    }
}
