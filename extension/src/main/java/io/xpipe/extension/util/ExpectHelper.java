package io.xpipe.extension.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

public class ExpectHelper {

    private static Path extractedExecutable = null;

    private  static Path getExtractedExecutable() {
        if (extractedExecutable == null) {
            XPipeDaemon.getInstance().withResource("io.xpipe.extension", "bin/expect.exe", path -> {
                extractedExecutable = Files.createTempFile(null, ".exe");
                Files.copy(path, extractedExecutable, StandardCopyOption.REPLACE_EXISTING);
            });
        }

        return extractedExecutable;
    }

    public static List<String> executeExpect(List<String> command, String password) throws IOException {
        var file = Files.createTempFile(null, ".lua");
        Files.writeString(file, expectFile(command, password));
        return List.of(getExtractedExecutable().toString(), file.toString());
    }

    private static String expectFile(List<String> command, String password) {
        return String.format("""
                        echo(false)
                        if spawn(%s) then
                            expect(":")
                            sendln("%s")
                        	echo(true)
                        end
                        """, command.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")), password);
    }
}
