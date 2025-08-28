package io.xpipe.app.terminal;

import io.xpipe.app.process.*;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.GithubReleaseDownloader;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import javafx.beans.property.Property;

import com.fasterxml.jackson.annotation.JsonTypeName;
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
@JsonTypeName("ohmyposh")
public class OhMyPoshTerminalPrompt extends ConfigFileTerminalPrompt {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<OhMyPoshTerminalPrompt> p) {
        return createOptions(
                p, s -> OhMyPoshTerminalPrompt.builder().configuration(s).build());
    }

    @SuppressWarnings("unused")
    public static OhMyPoshTerminalPrompt createDefault() {
        return OhMyPoshTerminalPrompt.builder()
                .configuration(
                        """
                                                              {
                                                                "$schema": "https://raw.githubusercontent.com/JanDeDobbeleer/oh-my-posh/main/themes/schema.json",
                                                                "blocks": [
                                                                  {
                                                                    "segments": [
                                                                      {
                                                                        "foreground": "#007ACC",
                                                                        "template": " {{ .CurrentDate | date .Format }} ",
                                                                        "properties": {
                                                                          "time_format": "15:04:05"
                                                                        },
                                                                        "style": "plain",
                                                                        "type": "time"
                                                                      }
                                                                    ],
                                                                    "type": "rprompt"
                                                                  },
                                                                  {
                                                                    "alignment": "left",
                                                                    "newline": true,
                                                                    "segments": [
                                                                      {
                                                                        "background": "#ffb300",
                                                                        "foreground": "#ffffff",
                                                                        "leading_diamond": "",
                                                                        "template": " {{ .UserName }} ",
                                                                        "style": "diamond",
                                                                        "trailing_diamond": "",
                                                                        "type": "session"
                                                                      },
                                                                      {
                                                                        "background": "#61AFEF",
                                                                        "foreground": "#ffffff",
                                                                        "powerline_symbol": "",
                                                                        "template": " {{ .Path }} ",
                                                                        "properties": {
                                                                          "style": "folder"
                                                                        },
                                                                        "exclude_folders": [
                                                                          "/super/secret/project"
                                                                        ],
                                                                        "style": "powerline",
                                                                        "type": "path"
                                                                      },
                                                                      {
                                                                        "background": "#2e9599",
                                                                        "background_templates": [
                                                                          "{{ if or (.Working.Changed) (.Staging.Changed) }}#f36943{{ end }}",
                                                                          "{{ if and (gt .Ahead 0) (gt .Behind 0) }}#a8216b{{ end }}",
                                                                          "{{ if gt .Ahead 0 }}#35b5ff{{ end }}",
                                                                          "{{ if gt .Behind 0 }}#f89cfa{{ end }}"
                                                                        ],
                                                                        "foreground": "#193549",
                                                                        "foreground_templates": [
                                                                          "{{ if and (gt .Ahead 0) (gt .Behind 0) }}#ffffff{{ end }}"
                                                                        ],
                                                                        "powerline_symbol": "",
                                                                        "template": " {{ .HEAD }}{{if .BranchStatus }} {{ .BranchStatus }}{{ end }} ",
                                                                        "properties": {
                                                                          "branch_template": "{{ trunc 25 .Branch }}",
                                                                          "fetch_status": true
                                                                        },
                                                                        "style": "powerline",
                                                                        "type": "git"
                                                                      },
                                                                      {
                                                                        "background": "#00897b",
                                                                        "background_templates": [
                                                                          "{{ if gt .Code 0 }}#e91e63{{ end }}"
                                                                        ],
                                                                        "foreground": "#ffffff",
                                                                        "template": "<parentBackground></>  ",
                                                                        "properties": {
                                                                          "always_enabled": true
                                                                        },
                                                                        "style": "diamond",
                                                                        "trailing_diamond": "",
                                                                        "type": "status"
                                                                      }
                                                                    ],
                                                                    "type": "prompt"
                                                                  }
                                                                ],
                                                                "final_space": true,
                                                                "version": 3
                                                              }
                                                              """)
                .build();
    }

    @Override
    public String getDocsLink() {
        return "https://ohmyposh.dev/docs";
    }

    @Override
    public String getId() {
        return "oh-my-posh";
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

        if (sc.view().findProgram("oh-my-posh").isPresent()) {
            return true;
        }

        var extension = sc.getOsType() == OsType.WINDOWS ? ".exe" : "";
        return sc.view().fileExists(getBinaryDirectory(sc).join("oh-my-posh" + extension));
    }

    @Override
    public void install(ShellControl sc) throws Exception {
        if (sc.getShellDialect() == ShellDialects.CMD) {
            ClinkHelper.install(sc);
        }

        var dir = getBinaryDirectory(sc);
        sc.view().mkdir(dir);
        if (sc.getOsType() == OsType.WINDOWS) {
            var file = GithubReleaseDownloader.getDownloadTempFile(
                    "JanDeDobbeleer/oh-my-posh", "posh-windows-amd64.exe", s -> s.equals("posh-windows-amd64.exe"));
            sc.view().transferLocalFile(file, dir.join("oh-my-posh.exe"));
        } else {
            var configDir = getConfigurationDirectory(sc);
            sc.command("curl -s https://ohmyposh.dev/install.sh | bash -s -- -d \"" + dir + "\" -t \"" + configDir
                            + "\"")
                    .execute();
        }
    }

    @Override
    public List<ShellDialect> getSupportedDialects() {
        return List.of(
                ShellDialects.BASH,
                ShellDialects.ZSH,
                ShellDialects.FISH,
                ShellDialects.CMD,
                ShellDialects.POWERSHELL,
                ShellDialects.POWERSHELL_CORE);
    }

    @Override
    protected String getConfigFileExtension() {
        return "json";
    }

    @Override
    protected ShellScript setupTerminalCommand(ShellControl shellControl, FilePath config) throws Exception {
        var lines = new ArrayList<String>();
        var dialect = shellControl.getOriginalShellDialect();
        if (dialect == ShellDialects.CMD) {
            var configDir = getConfigurationDirectory(shellControl);
            shellControl.view().mkdir(configDir);
            var configFile = configDir.join("oh-my-posh.lua");
            if (!shellControl.view().fileExists(configFile)) {
                shellControl.view().writeTextFile(configFile, "load(io.popen('oh-my-posh init cmd'):read(\"*a\"))()");
            }

            lines.add(dialect.addToPathVariableCommand(
                    List.of(ClinkHelper.getTargetDir(shellControl).toString()), false));
            lines.add("clink inject --quiet --profile \"" + configDir + "\"");
        } else {
            var configArg = config != null ? " --config \"" + config + "\"" : "";
            if (ShellDialects.isPowershell(shellControl)) {
                lines.add("& ([ScriptBlock]::Create((oh-my-posh init $(oh-my-posh get shell) --print" + configArg
                        + ") -join \"`n\"))");
            } else if (dialect == ShellDialects.FISH) {
                lines.add("oh-my-posh init fish" + configArg + " | source");
            } else {
                lines.add("eval \"$(oh-my-posh init " + dialect.getId() + configArg + ")\"");
            }
        }
        return ShellScript.lines(lines);
    }
}
