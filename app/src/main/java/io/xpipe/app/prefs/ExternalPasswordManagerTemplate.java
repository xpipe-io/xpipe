package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.core.process.OsType;

import java.util.List;
import java.util.stream.Stream;

public interface ExternalPasswordManagerTemplate extends PrefsChoiceValue {

    String getTemplate();

    String getDocsLink();

    ExternalPasswordManagerTemplate BITWARDEN = new ExternalPasswordManagerTemplate() {
        @Override
        public String getTemplate() {
            return "bw get password $KEY --nointeraction --raw";
        }

        @Override
        public String getDocsLink() {
            return "https://bitwarden.com/help/cli/#get";
        }

        @Override
        public String getId() {
            return "bitwarden";
        }
    };

    ExternalPasswordManagerTemplate ONEPASSWORD = new ExternalPasswordManagerTemplate() {
        @Override
        public String getTemplate() {
            return "op read $KEY --force";
        }

        @Override
        public String getDocsLink() {
            return "https://developer.1password.com/docs/cli/reference/commands/read";
        }

        @Override
        public String getId() {
            return "1password";
        }
    };

    ExternalPasswordManagerTemplate DASHLANE = new ExternalPasswordManagerTemplate() {
        @Override
        public String getTemplate() {
            return "dcli password --output console $KEY";
        }

        @Override
        public String getDocsLink() {
            return "https://cli.dashlane.com/personal/vault";
        }

        @Override
        public String getId() {
            return "dashlane";
        }
    };

    ExternalPasswordManagerTemplate LASTPASS = new ExternalPasswordManagerTemplate() {
        @Override
        public String getTemplate() {
            return "lpass show --password $KEY";
        }

        @Override
        public String getDocsLink() {
            return "https://askubuntu.com/questions/872842/how-does-one-basically-use-lastpass-cli";
        }

        @Override
        public String getId() {
            return "lastpass";
        }
    };

    ExternalPasswordManagerTemplate MACOS_KEYCHAIN = new ExternalPasswordManagerTemplate() {
        @Override
        public String getTemplate() {
            return "security find-generic-password -w -l $KEY";
        }

        @Override
        public String getId() {
            return "macosKeychain";
        }

        @Override
        public String getDocsLink() {
            return "https://scriptingosx.com/2021/04/get-password-from-keychain-in-shell-scripts/";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal() == OsType.MACOS;
        }
    };

    ExternalPasswordManagerTemplate KEEPER = new ExternalPasswordManagerTemplate() {
        @Override
        public String getTemplate() {
            var exec = OsType.getLocal() == OsType.WINDOWS ? "@keeper" : "keeper";
            return exec + " get $KEY --format password --unmask";
        }

        @Override
        public String getDocsLink() {
            return "https://docs.keeper.io/en/secrets-manager/commander-cli/command-reference/record-commands#get-command";
        }

        @Override
        public String getId() {
            return "keeper";
        }
    };

    List<ExternalPasswordManagerTemplate> ALL = Stream.of(
                    ONEPASSWORD, BITWARDEN, DASHLANE, LASTPASS, KEEPER, MACOS_KEYCHAIN)
            .filter(externalPasswordManager -> externalPasswordManager.isSelectable())
            .toList();
}
