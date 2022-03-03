package io.xpipe.extension.prefs;

import io.xpipe.extension.I18n;
import io.xpipe.extension.Translatable;

public interface PrefsChoiceValue extends Translatable {

    @Override
    default String toTranslatedString() {
        return I18n.get(getId());
    }

    String getId();
}
