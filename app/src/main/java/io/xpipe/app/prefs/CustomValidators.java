package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.validators.CustomValidator;
import com.dlsc.formsfx.model.validators.Validator;
import io.xpipe.app.core.AppI18n;

import java.nio.file.Files;
import java.nio.file.Path;

public class CustomValidators {

    public static Validator<String> absolutePath() {
        return CustomValidator.forPredicate((String s) -> {
            try {
                var p = Path.of(s);
                return p.isAbsolute();
            } catch (Exception ex) {
                return false;
            }
        }, AppI18n.get("notAnAbsolutePath"));
    }

    public static Validator<String> directory() {
        return CustomValidator.forPredicate((String s) -> {
            var p = Path.of(s);
            return Files.exists(p) && Files.isDirectory(p);
        }, AppI18n.get("notADirectory"));
    }
}
