package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppNames;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStoreEntryRef;

import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Getter
public enum PredefinedScriptStore {
    APT_UPDATE("Apt upgrade", () -> ScriptStore.builder()
            .textSource(ScriptTextSource.InPlace.builder()
                    .dialect(ShellDialects.SH)
                    .text(file("apt_upgrade.sh"))
                    .build())
            .shellScript(true)
            .runnableScript(true)
            .build()),
    REMOVE_CR("CRLF to LF", () -> ScriptStore.builder()
            .textSource(ScriptTextSource.InPlace.builder()
                    .dialect(ShellDialects.SH)
                    .text(file("crlf_to_lf.sh"))
                    .build())
            .fileScript(true)
            .shellScript(true)
            .build()),
    DIFF("Diff", () -> ScriptStore.builder()
            .textSource(ScriptTextSource.InPlace.builder()
                    .dialect(ShellDialects.SH)
                    .text(file("diff.sh"))
                    .build())
            .fileScript(true)
            .build()),
    GIT_CONFIG("Git Config", () -> ScriptStore.builder()
            .textSource(ScriptTextSource.InPlace.builder()
                    .text(file("git_config.sh"))
                    .build())
            .runnableScript(true)
            .build()),
    SYSTEM_HEALTH_STATUS("System health status", () -> ScriptStore.builder()
            .textSource(ScriptTextSource.InPlace.builder()
                    .dialect(ShellDialects.SH)
                    .text(file("system_health.sh"))
                    .build())
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

    public static ShellScript file(String name) {
        AtomicReference<String> string = new AtomicReference<>();
        AppResources.with(AppNames.extModuleName("base"), "scripts/" + name, var1 -> {
            string.set(Files.readString(var1));
        });
        return ShellScript.of(string.get());
    }
}
