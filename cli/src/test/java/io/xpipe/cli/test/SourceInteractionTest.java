package io.xpipe.cli.test;

import io.xpipe.extension.test.DaemonExtensionTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class SourceInteractionTest extends DaemonExtensionTest {

    @ParameterizedTest
    @EnumSource(TestDataSources.Config.class)
    public void info(TestDataSources.Config c) throws IOException {
        c.addDefault();
        CliTestHelper.exec(List.of("source", "info"));
    }

    @ParameterizedTest
    @EnumSource(TestDataSources.Config.class)
    public void peek(TestDataSources.Config c) throws IOException {
        c.addDefault();
        CliTestHelper.exec(List.of("source", "peek"));
    }

    @ParameterizedTest
    @EnumSource(TestDataSources.Config.class)
    public void edit(TestDataSources.Config c) throws IOException {
        c.addDefault();
        CliTestHelper.exec(List.of("source", "info"));
        CliTestHelper.exec(List.of("source", "edit", "-o", "delimiter=comma"));
        CliTestHelper.exec(List.of("source", "info"));
        CliTestHelper.exec(List.of("source", "edit", "-o", "delimiter=semicolon"));
    }

    @ParameterizedTest
    @EnumSource(TestDataSources.Config.class)
    public void convert(TestDataSources.Config c) throws IOException {
        c.addDefault();
        CliTestHelper.exec(List.of("source", "convert", "--category", "text"));
        CliTestHelper.exec(List.of("source", "convert", "--type", "binary"));
        CliTestHelper.exec(List.of("source", "convert", "--type", "text"));
        CliTestHelper.exec(List.of("source", "convert", "--type", c.getProvider(), "--new", ":" + UUID.randomUUID()));
    }

    @ParameterizedTest
    @EnumSource(TestDataSources.Config.class)
    public void ops(TestDataSources.Config c) throws IOException {
        var oldName = ":" + UUID.randomUUID();
        var newName = ":" + UUID.randomUUID();
        CliTestHelper.exec(c.addConfigArgs(List.of(
                "source",
                "add",
                "--quiet",
                "--id",
                oldName,
                "--type",
                c.getProvider(),
                c.getInputFile().toString())));
        CliTestHelper.exec(List.of("source", "mv", oldName, newName));
        CliTestHelper.exec(List.of("source", "mv", newName, oldName));
        CliTestHelper.exec(List.of("source", "mv", oldName, newName));
        CliTestHelper.exec(List.of("source", "rm", newName));
    }

    @Test
    public void list() throws IOException {
        CliTestHelper.exec(List.of("source", "list"));
    }
}
