package io.xpipe.app.terminal;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.password.KeePassXcAssociationKey;
import io.xpipe.app.password.KeePassXcManager;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.ShellTerminalInitCommand;
import io.xpipe.core.store.FilePath;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Optional;

@Getter
@SuperBuilder
@ToString
@Jacksonized
@JsonTypeName("starship")
public class StarshipTerminalPrompt extends ConfigFileTerminalPrompt {

    public static OptionsBuilder createOptions(Property<StarshipTerminalPrompt> p) {
        return createOptions(p, "toml", s -> StarshipTerminalPrompt.builder().configuration(s).build());
    }

    @Override
    public String getDocsLink() {
        return "";
    }

    @Override
    public void checkCanInstall(ShellControl sc) throws Exception {
        CommandSupport.isInPathOrThrow(sc, "curl");
    }

    @Override
    public boolean checkIfInstalled(ShellControl sc) throws Exception {
        if (sc.view().findProgram("starship").isPresent()) {
            return true;
        }

        return false;
    }

    @Override
    public void install(ShellControl sc) throws Exception {
        var dir = getBinaryDirectory(sc).join("starship");
        sc.command("curl -sS https://starship.rs/install.sh | sh /dev/stdin -y --bin-dir \"" + dir + "\" > /dev/null").execute();
    }

    @Override
    public FilePath prepareCustomConfigFile(ShellControl sc) throws Exception {
        var file = getConfigurationDirectory(sc).join("starship").join("starship.toml");
        sc.view().writeTextFile(file, configuration);
        return file;
    }

    @Override
    public FilePath getDefaultConfigFile(ShellControl sc) throws Exception {
        return sc.view().userHome().join(".config").join("starship.toml");
    }

    @Override
    public ShellTerminalInitCommand terminalCommand(ShellControl shellControl, FilePath configFile) throws Exception {
        return new ShellTerminalInitCommand() {
            @Override
            public Optional<String> terminalContent(ShellControl shellControl) throws Exception {
                var s = shellControl.getShellDialect().getSetEnvironmentVariableCommand("STARSHIP_CONFIG", "") + "\n" + "eval \"$(starship init bash)\"";
                return Optional.empty();
            }

            @Override
            public boolean canPotentiallyRunInDialect(ShellDialect dialect) {
                return false;
            }


        };
    }

    @Override
    public List<ShellDialect> getSupportedDialects() {
        return List.of(ShellDialects.BASH);
    }
}
