package test;

import io.xpipe.api.DataText;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.test.DaemonExtensionTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;

public class FileTest extends DaemonExtensionTest {

    static DataText referenceFile;

    @BeforeAll
    public static void setupStorage() throws Exception {
        referenceFile = getSource("text", "utf8-bom-lf.txt").asText();
    }

    @ParameterizedTest
    @MethodSource("test.item.ShellTestItem#getAll")
    public void testReadAndWrite(ShellStore store) throws Exception {
        try (var pc = store.create().start()) {
            var file = getTestFile(pc);
            var fileStore = FileStore.builder()
                    .fileSystem((FileSystemStore) store)
                    .file(file)
                    .build();
            var source = getSource("text", fileStore).asText();
            referenceFile.forwardTo(source);

            var read = source.readAll();

            Assertions.assertEquals(referenceFile.readAll(), read);
        }
    }

    private String getTestFile(ShellProcessControl pc) throws Exception {
        return getTemporaryDirectory(pc) + "/xpipe_test/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".txt";
    }

    private String getTemporaryDirectory(ShellProcessControl pc) throws Exception {
        if (pc.getOsType().equals(OsType.WINDOWS)) {
            return pc.executeStringSimpleCommand(ShellTypes.CMD, "echo %TEMP%");
        }

        return "/var/tmp";
    }
}
