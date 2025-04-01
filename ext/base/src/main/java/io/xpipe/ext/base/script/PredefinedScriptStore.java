package io.xpipe.ext.base.script;

import io.xpipe.app.resources.AppResources;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.process.ShellDialects;

import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Getter
public enum PredefinedScriptStore {
    CLINK_SETUP("Clink Setup", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.CLINK.getEntry())
            .minimumDialect(ShellDialects.CMD)
            .commands(file("clink.bat"))
            .initScript(true)
            .build()),
    CLINK_INJECT("Clink Inject", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.CLINK.getEntry())
            .minimumDialect(ShellDialects.CMD)
            .script(CLINK_SETUP.getEntry())
            .initScript(true)
            .commands("""
                            clink inject --quiet
                            """)
            .build()),
    STARSHIP_BASH("Starship Bash", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.STARSHIP.getEntry())
            .minimumDialect(ShellDialects.BASH)
            .commands(file("starship_bash.sh"))
            .initScript(true)
            .build()),
    STARSHIP_ZSH("Starship Zsh", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.STARSHIP.getEntry())
            .minimumDialect(ShellDialects.ZSH)
            .commands(file("starship_zsh.sh"))
            .initScript(true)
            .build()),
    STARSHIP_FISH("Starship Fish", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.STARSHIP.getEntry())
            .minimumDialect(ShellDialects.FISH)
            .commands(file("starship_fish.fish"))
            .initScript(true)
            .build()),
    STARSHIP_CMD("Starship Cmd", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.STARSHIP.getEntry())
            .minimumDialect(ShellDialects.CMD)
            .script(CLINK_SETUP.getEntry())
            .commands(file(("starship_cmd.bat")))
            .initScript(true)
            .build()),
    STARSHIP_POWERSHELL("Starship Powershell", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.STARSHIP.getEntry())
            .minimumDialect(ShellDialects.POWERSHELL)
            .commands(file("starship_powershell.ps1"))
            .initScript(true)
            .build()),
    APT_UPDATE("Apt upgrade", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.MANAGEMENT.getEntry())
            .minimumDialect(ShellDialects.SH)
            .commands(file(("apt_upgrade.sh")))
            .shellScript(true)
            .runnableScript(true)
            .build()),
    REMOVE_CR("CRLF to LF", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.FILES.getEntry())
            .minimumDialect(ShellDialects.SH)
            .commands(file(("crlf_to_lf.sh")))
            .fileScript(true)
            .shellScript(true)
            .build()),
    DIFF("Diff", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.FILES.getEntry())
            .minimumDialect(ShellDialects.SH)
            .commands(file(("diff.sh")))
            .fileScript(true)
            .build()),
    GIT_CONFIG("Git Config", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.MANAGEMENT.getEntry())
            .minimumDialect(null)
            .commands(file(("git_config.sh")))
            .runnableScript(true)
            .build()),
    OHMYPOSH_CMD("Oh My Posh cmd", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.OHMYPOSH.getEntry())
            .minimumDialect(ShellDialects.CMD)
            .script(CLINK_SETUP.getEntry())
            .commands(file(("ohmyposh.bat")))
            .initScript(true)
            .build()),
    OHMYPOSH_BASH("Oh My Posh bash", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.OHMYPOSH.getEntry())
            .minimumDialect(ShellDialects.BASH)
            .commands(file(("ohmyposh.sh")))
            .initScript(true)
            .build()),
    OHMYPOSH_ZSH("Oh My Posh zsh", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.OHMYPOSH.getEntry())
            .minimumDialect(ShellDialects.ZSH)
            .commands(file(("ohmyposh.sh")))
            .initScript(true)
            .build()),
    OHMYPOSH_POWERSHELL("Oh My Posh Powershell", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.OHMYPOSH.getEntry())
            .minimumDialect(ShellDialects.POWERSHELL)
            .commands(file(("ohmyposh.ps1")))
            .initScript(true)
            .build());

    private final String name;
    private final Supplier<ScriptStore> scriptStore;
    private final UUID uuid;

    @Setter
    private DataStoreEntryRef<ScriptStore> entry;

    PredefinedScriptStore(String name, Supplier<ScriptStore> scriptStore) {
        this.name = name;
        this.scriptStore = scriptStore;
        this.uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    public static String file(String name) {
        AtomicReference<String> string = new AtomicReference<>();
        AppResources.with("io.xpipe.ext.base", "scripts/" + name, var1 -> {
            string.set(Files.readString(var1));
        });
        return string.get();
    }
}
