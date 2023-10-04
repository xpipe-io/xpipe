package test;

import io.xpipe.api.DataSource;
import io.xpipe.app.test.DaemonExtensionTest;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.store.FileStore;
import io.xpipe.core.impl.TextSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TextFileTest extends DaemonExtensionTest {

    static Path utf8File =
            Path.of("ext/base/src/test/resources/utf8-bom-lf.txt").toAbsolutePath();
    static Path utf16File =
            Path.of("ext/base/src/test/resources/utf16-crlf.txt").toAbsolutePath();
    static Path appendReferenceFile =
            Path.of("ext/base/src/test/resources/append-reference.txt").toAbsolutePath();
    static Path appendOutputFile;
    static Path writeReferenceFile =
            Path.of("ext/base/src/test/resources/write-reference.txt").toAbsolutePath();
    static Path writeOutputFile;

    static DataSource utf8;
    static DataSource utf16;
    static DataSource appendReference;
    static DataSource writeReference;
    static DataSource appendOutput;
    static DataSource writeOutput;

    @BeforeAll
    public static void setupStorage() throws Exception {
        utf8 = getSource("text", "utf8-bom-lf.txt");
        utf16 = DataSource.create(null, "text", FileStore.local(utf16File));
        appendReference = DataSource.create(null, "text", FileStore.local(appendReferenceFile));
        writeReference = DataSource.create(null, "text", FileStore.local(writeReferenceFile));

        appendOutputFile = Files.createTempFile(null, null);
        appendOutput = DataSource.create(
                null,
                TextSource.builder()
                        .store(FileStore.local(appendOutputFile))
                        .charset(StreamCharset.get("windows-1252"))
                        .newLine(NewLine.LF)
                        .build());

        writeOutputFile = Files.createTempFile(null, null);
        writeOutput = DataSource.create(
                null,
                TextSource.builder()
                        .store(FileStore.local(writeOutputFile))
                        .charset(StreamCharset.UTF16_LE_BOM)
                        .newLine(NewLine.CRLF)
                        .build());
    }

    @Test
    public void testDetection() throws IOException {
        TextSource first = (TextSource) utf8.getInternalSource();
        Assertions.assertEquals(StreamCharset.UTF8_BOM, first.getCharset());
    }

    @Test
    public void testRead() throws IOException {
        var first = utf8.asText();
        var firstText = first.readAll();
        var firstTextLines = first.readLines(5);

        Assertions.assertEquals(firstText, "hello\nworld");
        Assertions.assertEquals(firstTextLines, List.of("hello", "world"));

        var second = utf16.asText();
        var secondText = second.readAll();
        var secondTextLines = second.readLines(5);

        Assertions.assertEquals(secondText, "how\nis\nit\ngoing");
        Assertions.assertEquals(secondTextLines, List.of("how", "is", "it", "going"));
    }

    @Test
    public void testWrite() throws IOException {
        var empty = Files.createTempFile(null, null);
        var emptySource = DataSource.create(
                null,
                TextSource.builder()
                        .store(FileStore.local(empty))
                        .charset(StreamCharset.UTF32_BE)
                        .newLine(NewLine.CRLF)
                        .build());
        emptySource.asText().forwardTo(writeOutput);

        var first = utf8.asText();
        first.forwardTo(writeOutput);
        var second = utf16.asText();
        second.forwardTo(writeOutput);

        var text = writeOutput.asText().readAll();
        var referenceText = writeReference.asText().readAll();
        Assertions.assertEquals(referenceText, text);

        var bytes = Files.readAllBytes(writeOutputFile);
        var referenceBytes = Files.readAllBytes(writeReferenceFile);
        Assertions.assertArrayEquals(bytes, referenceBytes);
    }

    @Test
    public void testAppend() throws IOException {
        var first = utf8.asText();
        first.appendTo(appendOutput);
        var second = utf16.asText();
        second.appendTo(appendOutput);

        var text = appendOutput.asText().readAll();
        var referenceText = appendReference.asText().readAll();
        Assertions.assertEquals(referenceText, text);

        var bytes = Files.readAllBytes(appendOutputFile);
        var referenceBytes = Files.readAllBytes(appendReferenceFile);
        Assertions.assertArrayEquals(bytes, referenceBytes);
    }
}
