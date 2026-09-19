package io.xpipe.app.prefs;

import io.xpipe.app.core.AppI18n;

import javafx.beans.value.ObservableValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

public enum SidebarLocation implements PrefsChoiceValue {
    LEFT {
        @Override
        public String getId() {
            return "left";
        }

        @Override
        public ObservableValue<String> toTranslatedString() {
            return AppI18n.observable("leftSide");
        }
    },
    RIGHT {
        @Override
        public String getId() {
            return "right";
        }

        @Override
        public ObservableValue<String> toTranslatedString() {
            return AppI18n.observable("rightSide");
        }
    }
}
