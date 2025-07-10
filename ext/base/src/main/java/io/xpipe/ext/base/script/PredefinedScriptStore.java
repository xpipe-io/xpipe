package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppResources;
import io.xpipe.app.process.ShellDialects;
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
    SYSTEM_HEALTH_STATUS("System health status", () -> SimpleScriptStore.builder()
            .group(PredefinedScriptGroup.MANAGEMENT.getEntry())
            .minimumDialect(ShellDialects.SH)
            .commands(file(("system_health.sh")))
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
