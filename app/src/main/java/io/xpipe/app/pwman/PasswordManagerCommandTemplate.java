package io.xpipe.app.pwman;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.core.OsType;

import java.util.List;
import java.util.stream.Stream;

public interface PasswordManagerCommandTemplate extends PrefsChoiceValue {

    PasswordManagerCommandTemplate BITWARDEN = new PasswordManagerCommandTemplate() {
        @Override
        public String getTemplate() {
            return "bw get password $KEY --nointeraction --raw";
        }

        @Override
        public String getId() {
            return "bitwarden";
        }
    };
    PasswordManagerCommandTemplate ONEPASSWORD = new PasswordManagerCommandTemplate() {
        @Override
        public String getTemplate() {
            return "op read $KEY --force";
        }

        @Override
        public String getId() {
            return "1password";
        }
    };
    PasswordManagerCommandTemplate DASHLANE = new PasswordManagerCommandTemplate() {
        @Override
        public String getTemplate() {
            return "dcli password --output console $KEY";
        }

        @Override
        public String getId() {
            return "dashlane";
        }
    };
    PasswordManagerCommandTemplate LASTPASS = new PasswordManagerCommandTemplate() {
        @Override
        public String getTemplate() {
            return "lpass show --password $KEY";
        }

        @Override
        public String getId() {
            return "lastpass";
        }
    };
    PasswordManagerCommandTemplate KEEPER = new PasswordManagerCommandTemplate() {
        @Override
        public String getTemplate() {
            var exec = OsType.getLocal() == OsType.WINDOWS ? "@keeper" : "keeper";
            return exec + " get $KEY --format password --unmask";
        }

        @Override
        public String getId() {
            return "keeper";
        }
    };
    List<PasswordManagerCommandTemplate> ALL =
            Stream.of(ONEPASSWORD, BITWARDEN, DASHLANE, LASTPASS, KEEPER).toList();

    String getTemplate();
}
