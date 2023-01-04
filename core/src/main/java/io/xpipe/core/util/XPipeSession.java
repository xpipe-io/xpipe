package io.xpipe.core.util;

import io.xpipe.core.process.OsType;
import lombok.Value;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

@Value
public class XPipeSession {

    boolean isNewSystemSession;

    /**
     * Unique identifier that resets on every X-Pipe restart.
     */
    UUID sessionId;

    /**
     * Unique identifier that resets on every X-Pipe update.
     */
    UUID buildSessionId;

    /**
     * Unique identifier that resets on system restarts.
     */
    UUID systemSessionId;

    private static XPipeSession INSTANCE;

    public static void init(UUID buildSessionId) throws Exception {
        var sessionFile = XPipeTempDirectory.getLocal().resolve("xpipe_session");
        var isNew = !Files.exists(sessionFile);
        var systemSessionId = isNew ? UUID.randomUUID() : UUID.fromString(Files.readString(sessionFile));

        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            var pf = Path.of("C:\\pagefile.sys");
            BasicFileAttributes attr = Files.readAttributes(pf, BasicFileAttributes.class);
            var timeUuid = UUID.nameUUIDFromBytes(attr.creationTime().toInstant().toString().getBytes());
            isNew = isNew && timeUuid.equals(systemSessionId);
            systemSessionId = timeUuid;
        }

        Files.writeString(sessionFile, systemSessionId.toString());
        INSTANCE = new XPipeSession(isNew, UUID.randomUUID(), buildSessionId, systemSessionId);
    }

    public static XPipeSession get() {
        return INSTANCE;
    }
}
