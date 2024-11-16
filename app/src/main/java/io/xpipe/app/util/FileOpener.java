package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialects;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Consumer;

public class FileOpener {

    public static void openInTextEditor(String localFile) {
        var editor = AppPrefs.get().externalEditor().getValue();
        if (editor == null) {
            return;
        }

        try {
            editor.launch(Path.of(localFile).toRealPath());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(
                            "Unable to launch editor "
                                    + editor.toTranslatedString().getValue()
                                    + ".\nMaybe try to use a different editor in the settings.",
                            e)
                    .expected()
                    .handle();
        }
    }

    public static void openWithAnyApplication(String localFile) {
        try {
            switch (OsType.getLocal()) {
                case OsType.Windows windows -> {
                    var cmd = CommandBuilder.of().add("rundll32.exe", "shell32.dll,OpenAs_RunDLL", localFile);
                    LocalShell.getShell().executeSimpleCommand(cmd);
                }
                case OsType.Linux linux -> {
                    throw new UnsupportedOperationException();
                }
                case OsType.MacOs macOs -> {
                    throw new UnsupportedOperationException();
                }
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable("Unable to open file " + localFile, e).handle();
        }
    }

    public static void openInDefaultApplication(String localFile) {
        try (var pc = LocalShell.getShell().start()) {
            if (pc.getOsType().equals(OsType.WINDOWS)) {
                if (pc.getShellDialect() == ShellDialects.POWERSHELL) {
                    pc.command(CommandBuilder.of().add("Invoke-Item").addFile(localFile))
                            .execute();
                } else {
                    pc.executeSimpleCommand("start \"\" \"" + localFile + "\"");
                }
            } else if (pc.getOsType().equals(OsType.LINUX)) {
                pc.executeSimpleCommand("xdg-open \"" + localFile + "\"");
            } else {
                pc.executeSimpleCommand("open \"" + localFile + "\"");
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable("Unable to open file " + localFile, e).handle();
        }
    }

    public static void openReadOnlyString(String input) {
        if (input == null) {
            input = "";
        }

        var id = UUID.randomUUID();
        String s = input;
        FileBridge.get()
                .openIO(
                        id.toString(),
                        id,
                        null,
                        () -> new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)),
                        null,
                        v -> openInTextEditor(v));
    }

    public static void openString(String keyName, Object key, String input, Consumer<String> output) {
        if (input == null) {
            input = "";
        }

        String s = input;
        FileBridge.get()
                .openIO(
                        keyName,
                        key,
                        null,
                        () -> new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)),
                        (size) -> new ByteArrayOutputStream(s.length()) {
                            @Override
                            public void close() throws IOException {
                                super.close();
                                output.accept(new String(toByteArray(), StandardCharsets.UTF_8));
                            }
                        },
                        file -> openInTextEditor(file));
    }
}
