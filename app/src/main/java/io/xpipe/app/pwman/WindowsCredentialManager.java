package io.xpipe.app.pwman;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.LocalShell;

import io.xpipe.app.util.CredAdvapi32;
import io.xpipe.app.util.WinCred;
import javafx.beans.property.Property;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("windowsCredentialManager")
@Builder
@Jacksonized
public class WindowsCredentialManager implements PasswordManager {

    @Override
    public boolean supportsKeyConfiguration() {
        return false;
    }

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.none();
    }

    @Override
    public boolean selectInitial() {
        return false;
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<WindowsCredentialManager> p) {
        return new OptionsBuilder()
                .nameAndDescription("passwordManagerTest")
                .addComp(new PasswordManagerTestComp(true));
    }

    @Override
    public synchronized Result query(String key) {
        try {
            var generic = WinCred.getCredential(key, CredAdvapi32.CRED_TYPE_GENERIC);
            if (generic.isPresent()) {
                return Result.of(Credentials.of(generic.get().getUsername(), generic.get().getPassword()), null);
            }

            var windows = WinCred.getCredential(key, CredAdvapi32.CRED_TYPE_DOMAIN_PASSWORD);
            if (windows.isPresent()) {
                return Result.of(Credentials.of(windows.get().getUsername(), windows.get().getPassword()), null);
            }

            return null;
        } catch (Throwable ex) {
            ErrorEventFactory.fromThrowable(ex).expected().handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return "Credential name";
    }

    @Override
    public String getWebsite() {
        return "https://support.microsoft.com/en-us/windows/credential-manager-in-windows-1b5c916a-6a16-889f-8581-fc16e8165ac0";
    }
}
