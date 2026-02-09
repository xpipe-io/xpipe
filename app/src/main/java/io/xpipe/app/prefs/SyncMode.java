package io.xpipe.app.prefs;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.PrefsChoiceValue;

import javafx.beans.value.ObservableValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SyncMode implements PrefsChoiceValue {
    INSTANT {
        @Override
        public String getId() {
            return "instant";
        }

        @Override
        public ObservableValue<String> toTranslatedString() {
            return AppI18n.observable("syncModeInstant");
        }
    },
    SESSION {
        @Override
        public String getId() {
            return "session";
        }

        @Override
        public ObservableValue<String> toTranslatedString() {
            return AppI18n.observable("syncModeSession");
        }
    },
    MANUAL {
        @Override
        public String getId() {
            return "manual";
        }

        @Override
        public ObservableValue<String> toTranslatedString() {
            return AppI18n.observable("syncModeManual");
        }
    }
}
