package io.xpipe.cli.test;

import io.xpipe.extension.test.DaemonExtensionTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class SourceIoCommandTest extends DaemonExtensionTest {

    @ParameterizedTest
    @EnumSource(value = TestDataSources.Config.class)
    public void testWriteToStreamWithProvider(TestDataSources.Config c) throws Exception {
        c.addDefault();
        try (var cap = CliInterface.get().execAndCapture(c.addConfigArgs(List.of("source", "write", "--type", c.provider)), InputStream.nullInputStream())) {
            cap.checkEquals(c.getOutputReference());
        }
    }

    @ParameterizedTest
    @EnumSource(value = TestDataSources.Config.class)
    public void testFileReadWithoutProvider(TestDataSources.Config c) throws Exception {
        CliTestHelper.exec(c.addConfigArgs(
                List.of("source", "add", "--quiet", c.getInputFile().toString())));
        try (var cap = CliInterface.get().execAndCapture(c.addConfigArgs(List.of("source", "write", "--type", c.provider)), InputStream.nullInputStream())) {
            cap.checkEquals(c.getOutputReference());
        }
    }

    @ParameterizedTest
    @EnumSource(value = TestDataSources.Config.class)
    public void testPipeRead(TestDataSources.Config c) throws Exception {
        try (var in = Files.newInputStream(c.getInputFile())) {
            CliInterface.get().exec(c.addConfigArgs(List.of("source", "add", "--quiet", "--type", c.provider)), in);
            try (var cap = CliInterface.get().execAndCapture(c.addConfigArgs(List.of("source", "write", "--type", c.provider)), InputStream.nullInputStream())) {
                cap.checkEquals(c.getOutputReference());
            }
        }
    }
}
