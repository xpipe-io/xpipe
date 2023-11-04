package io.xpipe.core.util;

import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileNames;

import java.nio.file.Files;
import java.util.UUID;

public class XPipeSystemId {

    private static UUID localId;

    public static void init() {
        try {
            var file = XPipeInstallation.getDataDir().resolve("system_id");
            if (!Files.exists(file)) {
                Files.writeString(file, UUID.randomUUID().toString());
            }
            localId = UUID.fromString(Files.readString(file).trim());
        } catch (Exception ex) {
            localId = UUID.randomUUID();
        }
    }

    public static UUID getLocal() {
        return localId;
    }

    public static UUID getSystemId(ShellControl proc) throws Exception {
        var file = FileNames.join(XPipeInstallation.getDataDir(proc), "system_id");
        if (file == null) {
            return UUID.randomUUID();
        }

        if (!proc.getShellDialect().createFileExistsCommand(proc, file).executeAndCheck()) {
            return writeRandom(proc, file);
        }

        try {
            return UUID.fromString(proc.getShellDialect().getFileReadCommand(proc, file).readStdoutOrThrow().trim());
        } catch (IllegalArgumentException ex) {
            // Handle invalid UUID content case
            return writeRandom(proc, file);
        }
    }

    private static UUID writeRandom(ShellControl proc, String file) throws Exception {
        proc.executeSimpleCommand(proc.getShellDialect().getMkdirsCommand(FileNames.getParent(file)), "Unable to access or create directory " + file);
        var id = UUID.randomUUID();
        proc.getShellDialect().createTextFileWriteCommand(proc, id.toString(), file).execute();
        return id;
    }
}
