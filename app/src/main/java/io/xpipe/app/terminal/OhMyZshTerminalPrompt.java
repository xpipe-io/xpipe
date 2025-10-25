package io.xpipe.app.terminal;

import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.process.ShellScript;
import io.xpipe.core.FilePath;

import javafx.beans.property.Property;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@SuperBuilder
@ToString
@Jacksonized
@JsonTypeName("ohmyzsh")
public class OhMyZshTerminalPrompt extends ConfigFileTerminalPrompt {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<OhMyZshTerminalPrompt> p) {
        return createOptions(
                p, s -> OhMyZshTerminalPrompt.builder().configuration(s).build());
    }

    @SuppressWarnings("unused")
    public static OhMyZshTerminalPrompt createDefault() {
        return OhMyZshTerminalPrompt.builder()
                .configuration(
                        """
                                                             # Set name of the theme to load --- if set to "random", it will
                                                             # load a random theme each time Oh My Zsh is loaded, in which case,
                                                             # to know which specific one was loaded, run: echo $RANDOM_THEME
                                                             # See https://github.com/ohmyzsh/ohmyzsh/wiki/Themes
                                                             ZSH_THEME="robbyrussell"

                                                             # Set list of themes to pick from when loading at random
                                                             # Setting this variable when ZSH_THEME=random will cause zsh to load
                                                             # a theme from this variable instead of looking in $ZSH/themes/
                                                             # If set to an empty array, this variable will have no effect.
                                                             # ZSH_THEME_RANDOM_CANDIDATES=( "robbyrussell" "agnoster" )

                                                             # Uncomment the following line to use case-sensitive completion.
                                                             # CASE_SENSITIVE="true"

                                                             # Uncomment the following line to use hyphen-insensitive completion.
                                                             # Case-sensitive completion must be off. _ and - will be interchangeable.
                                                             # HYPHEN_INSENSITIVE="true"

                                                             # Uncomment one of the following lines to change the auto-update behavior
                                                             # zstyle ':omz:update' mode disabled  # disable automatic updates
                                                             # zstyle ':omz:update' mode auto      # update automatically without asking
                                                             # zstyle ':omz:update' mode reminder  # just remind me to update when it's time

                                                             # Uncomment the following line to change how often to auto-update (in days).
                                                             # zstyle ':omz:update' frequency 13

                                                             # Uncomment the following line if pasting URLs and other text is messed up.
                                                             # DISABLE_MAGIC_FUNCTIONS="true"

                                                             # Uncomment the following line to disable colors in ls.
                                                             # DISABLE_LS_COLORS="true"

                                                             # Uncomment the following line to disable auto-setting terminal title.
                                                             # DISABLE_AUTO_TITLE="true"

                                                             # Uncomment the following line to enable command auto-correction.
                                                             # ENABLE_CORRECTION="true"

                                                             # Uncomment the following line to display red dots whilst waiting for completion.
                                                             # You can also set it to another string to have that shown instead of the default red dots.
                                                             # e.g. COMPLETION_WAITING_DOTS="%F{yellow}waiting...%f"
                                                             # Caution: this setting can cause issues with multiline prompts in zsh < 5.7.1 (see #5765)
                                                             # COMPLETION_WAITING_DOTS="true"

                                                             # Uncomment the following line if you want to disable marking untracked files
                                                             # under VCS as dirty. This makes repository status check for large repositories
                                                             # much, much faster.
                                                             # DISABLE_UNTRACKED_FILES_DIRTY="true"

                                                             # Uncomment the following line if you want to change the command execution time
                                                             # stamp shown in the history command output.
                                                             # You can set one of the optional three formats:
                                                             # "mm/dd/yyyy"|"dd.mm.yyyy"|"yyyy-mm-dd"
                                                             # or set a custom format using the strftime function format specifications,
                                                             # see 'man strftime' for details.
                                                             # HIST_STAMPS="mm/dd/yyyy"

                                                             # Would you like to use another custom folder than $ZSH/custom?
                                                             # ZSH_CUSTOM=/path/to/new-custom-folder

                                                             # Which plugins would you like to load?
                                                             # Standard plugins can be found in $ZSH/plugins/
                                                             # Custom plugins may be added to $ZSH_CUSTOM/plugins/
                                                             # Example format: plugins=(rails git textmate ruby lighthouse)
                                                             # Add wisely, as too many plugins slow down shell startup.
                                                             plugins=(git)
                                                             """)
                .build();
    }

    @Override
    public String getDocsLink() {
        return "https://github.com/ohmyzsh/ohmyzsh";
    }

    @Override
    public String getId() {
        return "oh-my-zsh";
    }

    @Override
    public void checkCanInstall(ShellControl sc) throws Exception {
        CommandSupport.isInPathOrThrow(sc, "curl");
    }

    @Override
    public boolean checkIfInstalled(ShellControl sc) throws Exception {
        var configDir = getConfigurationDirectory(sc);
        return sc.view().fileExists(configDir.join("oh-my-zsh.sh"));
    }

    @Override
    public void install(ShellControl sc) throws Exception {
        var configDir = getConfigurationDirectory(sc);
        sc.view().deleteDirectory(configDir);
        sc.command(
                        "KEEP_ZSHRC=yes ZSH=\"" + configDir
                                + "\" sh -c \"$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)\" \"\" --unattended")
                .execute();
    }

    @Override
    public List<ShellDialect> getSupportedDialects() {
        return List.of(ShellDialects.ZSH);
    }

    @Override
    protected FilePath getTargetConfigFile(ShellControl shellControl) throws Exception {
        FilePath configFile =
                getConfigurationDirectory(shellControl).join(getId() + "-custom." + getConfigFileExtension());
        return configFile;
    }

    @Override
    protected String getConfigFileExtension() {
        return "sh";
    }

    @Override
    protected ShellScript setupTerminalCommand(ShellControl shellControl, FilePath config) throws Exception {
        var script = config != null ? shellControl.view().readTextFile(config) : "";
        var fixed = script != null
                ? script.replaceAll("source \\$ZSH/oh-my-zsh.sh", "")
                        .replaceAll("export ZSH=\"\\$HOME/.oh-my-zsh\"", "")
                : null;
        return ShellScript.lines(
                "export ZSH=\"" + getConfigurationDirectory(shellControl) + "\"", fixed, "source $ZSH/oh-my-zsh.sh");
    }
}
