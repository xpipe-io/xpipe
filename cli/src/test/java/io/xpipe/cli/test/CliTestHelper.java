package io.xpipe.cli.test;

import lombok.SneakyThrows;

import java.io.InputStream;
import java.util.List;

public class CliTestHelper {

    @SneakyThrows
    public static void exec(List<String> args) {
        CliInterface.get().exec(args, InputStream.nullInputStream());
    }
}
