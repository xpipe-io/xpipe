package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.validators.CustomValidator;
import com.dlsc.formsfx.model.validators.Validator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CustomValidators {

    public static Validator<String> absolutePath() {
        return CustomValidator.forPredicate(
                (String s) -> {
                    try {
                        var p = Path.of(s);
                        return p.isAbsolute();
                    } catch (Exception ex) {
                        return false;
                    }
                },
                "notAnAbsolutePath");
    }

    public static Validator<String> directory() {
        return CustomValidator.forPredicate(
                (String s) -> {
                    var p = Path.of(s);
                    return Files.exists(p) && Files.isDirectory(p);
                },
                "notADirectory");
    }

    public static Validator<String> emptyStorageDirectory() {
        return CustomValidator.forPredicate(
                (String s) -> {
                    var p = Path.of(s);
                    if (AppPrefs.get() == null) {
                        return true;
                    }

                    if (p.equals(AppPrefs.get().storageDirectory().getValue())) {
                        return true;
                    }

                    try {
                        return Files.list(p).findAny().isEmpty();
                    } catch (IOException ignored) {
                        return false;
                    }
                },
                "notAnEmptyDirectory");
    }
}
