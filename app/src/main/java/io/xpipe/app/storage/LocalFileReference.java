package io.xpipe.app.storage;

import io.xpipe.core.process.OsType;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Value
@AllArgsConstructor
public class LocalFileReference {

    public static LocalFileReference of(String s) {
        if (s == null) {
            return null;
        }

        var replaced = s.trim().replace("<DATA>", DataStorage.get().getDataDir().toString());
        try {
            return new LocalFileReference(Path.of(replaced).toString());
        } catch (InvalidPathException ex) {
            return new LocalFileReference(replaced);
        }
    }

    @NonNull
    String path;

    public String serialize() {
        var start = DataStorage.get().getDataDir();
        try {
            if (Path.of(path).startsWith(start)) {
                return "<DATA>" + OsType.getLocal().getFileSystemSeparator() + start.relativize(Path.of(path));
            }
        } catch (InvalidPathException ignored) {}

        return path.toString();
    }
}
