package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.core.process.OsType;

import java.util.List;
import java.util.stream.Stream;

public interface ExternalPasswordManager extends PrefsChoiceValue {

    String getTemplate();

    static ExternalPasswordManager BITWARDEN = new ExternalPasswordManager() {
        @Override
        public String getTemplate() {
            return "bw get password $KEY --nointeraction --raw";
        }

        @Override
        public String getId() {
            return "bitwarden";
        }
    };

    static ExternalPasswordManager ONEPASSWORD = new ExternalPasswordManager() {
        @Override
        public String getTemplate() {
            return "op read $KEY --force";
        }

        @Override
        public String getId() {
            return "1Password";
        }
    };

    static ExternalPasswordManager DASHLANE = new ExternalPasswordManager() {
        @Override
        public String getTemplate() {
            return "dcli password --output console $KEY";
        }

        @Override
        public String getId() {
            return "Dashlane";
        }
    };

    static ExternalPasswordManager LASTPASS = new ExternalPasswordManager() {
        @Override
        public String getTemplate() {
            return "lpass show --password $KEY";
        }

        @Override
        public String getId() {
            return "LastPass";
        }
    };

    static ExternalPasswordManager MACOS_KEYCHAIN = new ExternalPasswordManager() {
        @Override
        public String getTemplate() {
            return "security find-generic-password -w -l $KEY";
        }

        @Override
        public String getId() {
            return "macOS keychain";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal() == OsType.MACOS;
        }
    };

    static List<ExternalPasswordManager> ALL = Stream.of(ONEPASSWORD, BITWARDEN, DASHLANE, LASTPASS, MACOS_KEYCHAIN)
            .filter(externalPasswordManager -> externalPasswordManager.isSelectable())
            .toList();
}
