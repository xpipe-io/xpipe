package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppResources;
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
    SETUP_CLINK(
            "Setup Clink",
            () -> SimpleScriptStore.builder()
                    .group(PredefinedScriptGroup.CLINK.getEntry())
                    .minimumDialect(ShellDialects.CMD)
                    .commands(file("starship_cmd.bat"))
                    .executionType(SimpleScriptStore.ExecutionType.TERMINAL_ONLY)
                    .build()),
    CLINK_INJECT(
            "Inject Clink",
            () -> SimpleScriptStore.builder()
                    .group(PredefinedScriptGroup.CLINK.getEntry())
                    .minimumDialect(ShellDialects.CMD)
                    .script(SETUP_CLINK.getEntry())
                    .commands(
                            """
                            clink inject --quiet
                            """)
                    .executionType(SimpleScriptStore.ExecutionType.TERMINAL_ONLY)
                    .build()),
    STARSHIP_BASH("Starship Bash", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.STARSHIP.getEntry())
            .minimumDialect(ShellDialects.BASH)
            .commands(file("starship_bash.sh"))
            .executionType(SimpleScriptStore.ExecutionType.TERMINAL_ONLY)
            .build()),
    STARSHIP_CMD(
            "Starship Cmd",
            () -> SimpleScriptStore.builder()
                    .group(PredefinedScriptGroup.STARSHIP.getEntry())
                    .minimumDialect(ShellDialects.CMD)
                    .script(SETUP_CLINK.getEntry())
                    .commands(
                            """
                            WHERE starship >NUL 2>NUL
                            IF NOT %ERRORLEVEL%==0 (
                                winget install starship
                                SET "PATH=%PATH%;C:\\Program Files\\starship\\bin"
                            )

                            MKDIR "%USERPROFILE%\\.xpipe\\scriptdata\\starship" >NUL 2>NUL
                            echo load(io.popen('starship init cmd'):read("*a"))() > "%USERPROFILE%\\.xpipe\\scriptdata\\starship\\starship.lua"
                            clink inject --quiet --profile "%USERPROFILE%\\.xpipe\\scriptdata\\starship"
                            """)
                    .executionType(SimpleScriptStore.ExecutionType.TERMINAL_ONLY)
                    .build()),
    STARSHIP_POWERSHELL(
            "Starship Powershell",
            () -> SimpleScriptStore.builder()
                    .group(PredefinedScriptGroup.STARSHIP.getEntry())
                    .minimumDialect(ShellDialects.POWERSHELL)
                    .commands(
                            """
                            if (-not $(Get-Command -ErrorAction SilentlyContinue starship)) {
                                winget install starship
                                
                                # Update current process PATH environment variable
                                $env:Path=([System.Environment]::GetEnvironmentVariable("Path", "Machine"), [System.Environment]::GetEnvironmentVariable("Path", "User")) -match '.' -join ';'
                            }

                            Invoke-Expression (&starship init powershell)
                            """)
                    .executionType(SimpleScriptStore.ExecutionType.TERMINAL_ONLY)
                    .build());

    public static String file(String name) {
        AtomicReference<String> string = new AtomicReference<>();
        AppResources.with("io.xpipe.ext.base", "scripts/" + name, var1 -> {
            string.set(Files.readString(var1));
        });
        return string.get();
    }

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
}
