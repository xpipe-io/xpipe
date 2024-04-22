package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.core.process.OsType;

import java.util.List;
import java.util.stream.Stream;

public interface ExternalPasswordManager extends PrefsChoiceValue {

    String getTemplate();

    ExternalPasswordManager BITWARDEN = new ExternalPasswordManager() {
        @Override
        public String getTemplate() {
            return "bw get password $KEY --nointeraction --raw";
        }

        @Override
        public String getId() {
            return "bitwarden";
        }
    };

    ExternalPasswordManager ONEPASSWORD = new ExternalPasswordManager() {
        @Override
        public String getTemplate() {
            return "op read $KEY --force";
        }

        @Override
        public String getId() {
            return "1password";
        }
    };

    ExternalPasswordManager DASHLANE = new ExternalPasswordManager() {
        @Override
        public String getTemplate() {
            return "dcli password --output console $KEY";
        }

        @Override
        public String getId() {
            return "dashlane";
        }
    };

    ExternalPasswordManager LASTPASS = new ExternalPasswordManager() {
        @Override
        public String getTemplate() {
            return "lpass show --password $KEY";
        }

        @Override
        public String getId() {
            return "lastpass";
        }
    };

    ExternalPasswordManager MACOS_KEYCHAIN = new ExternalPasswordManager() {
        @Override
        public String getTemplate() {
            return "security find-generic-password -w -l $KEY";
        }

        @Override
        public String getId() {
            return "macosKeychain";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal() == OsType.MACOS;
        }
    };

    List<ExternalPasswordManager> ALL = Stream.of(ONEPASSWORD, BITWARDEN, DASHLANE, LASTPASS, MACOS_KEYCHAIN)
            .filter(externalPasswordManager -> externalPasswordManager.isSelectable())
            .toList();
}
