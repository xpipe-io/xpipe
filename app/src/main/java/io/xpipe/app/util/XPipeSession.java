package io.xpipe.app.util;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.UuidHelper;
import lombok.Value;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

@Value
public class XPipeSession {

    boolean isNewSystemSession;

    boolean isNewBuildSession;

    /**
     * Unique identifier that resets on every XPipe restart.
     */
    UUID sessionId;

    /**
     * Unique identifier that resets on every XPipe update.
     */
    UUID buildSessionId;

    /**
     * Unique identifier that resets on system restarts.
     */
    UUID systemSessionId;

    private static XPipeSession INSTANCE;

    public static void init(UUID buildSessionId) {
        if (INSTANCE != null) {
            return;
        }

        var sessionFile = Path.of(System.getProperty("java.io.tmpdir")).resolve("xpipe_session");
        var isNewSystemSession = !Files.exists(sessionFile);
        var systemSessionId = isNewSystemSession
                ? UUID.randomUUID()
                : UuidHelper.parse(() -> Files.readString(sessionFile)).orElse(UUID.randomUUID());

        try {
            //TODO: People might move their page file to another drive
            if (OsType.getLocal().equals(OsType.WINDOWS)) {
                var pf = Path.of("C:\\pagefile.sys");
                BasicFileAttributes attr = Files.readAttributes(pf, BasicFileAttributes.class);
                var timeUuid = UUID.nameUUIDFromBytes(
                        attr.creationTime().toInstant().toString().getBytes());
                isNewSystemSession = isNewSystemSession && timeUuid.equals(systemSessionId);
                systemSessionId = timeUuid;
            }
        } catch (Exception ex) {
            isNewSystemSession = true;
            systemSessionId = UUID.randomUUID();
        }

        try {
            Files.writeString(sessionFile, systemSessionId.toString());
        } catch (Exception ignored) {
        }

        var s = AppCache.get("lastBuild", String.class, () -> buildSessionId.toString());
        var isBuildChanged = !buildSessionId.toString().equals(s);
        AppCache.update("lastBuild", AppProperties.get().getVersion());

        INSTANCE = new XPipeSession(isNewSystemSession, isBuildChanged, UUID.randomUUID(), buildSessionId, systemSessionId);
    }

    public static XPipeSession get() {
        return INSTANCE;
    }
}
