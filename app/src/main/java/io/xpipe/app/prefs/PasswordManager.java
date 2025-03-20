package io.xpipe.app.prefs;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.*;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.ValidationException;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PasswordManager.None.class),
    @JsonSubTypes.Type(value = PasswordManager.WindowsCredentialManager.class)
})
public interface PasswordManager {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(PasswordManager.None.class);
        if (OsType.getLocal() == OsType.WINDOWS) {
            l.add(PasswordManager.WindowsCredentialManager.class);
        }
        return l;
    }

    default void checkComplete() throws ValidationException {}

    String getDocsLink();

    String retrievePassword(String key);

    @JsonTypeName("none")
    @Value
    class None implements PasswordManager {

        static OptionsBuilder createOptions(Property<None> property) {
            return new OptionsBuilder().name("test").addString(new SimpleObjectProperty<>());
        }

        @Override
        public String getDocsLink() {
            return null;
        }

        @Override
        public String retrievePassword(String key) {
            throw ErrorEvent.expected(new UnsupportedOperationException("No password manager has been configured"));
        }
    }

    @JsonTypeName("windowsCredentialManager")
    @Value
    class WindowsCredentialManager implements io.xpipe.app.prefs.PasswordManager {

        static OptionsBuilder createOptions(Property<WindowsCredentialManager> property) {
            return new OptionsBuilder();
        }

        private static boolean loaded = false;

        @Override
        public String getDocsLink() {
            return "https://support.microsoft.com/en-us/windows/accessing-credential-manager-1b5c916a-6a16-889f-8581-fc16e8165ac0";
        }

        @Override
        public synchronized String retrievePassword(String key) {
            try {
                if (!loaded) {
                    loaded = true;
                    var cmd =
                            """
                   $code = @"
                   using System.Text;
                   using System;
                   using System.Runtime.InteropServices;

                   namespace CredManager {
                     [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
                     public struct CredentialMem
                     {
                       public int flags;
                       public int type;
                       public string targetName;
                       public string comment;
                       public System.Runtime.InteropServices.ComTypes.FILETIME lastWritten;
                       public int credentialBlobSize;
                       public IntPtr credentialBlob;
                       public int persist;
                       public int attributeCount;
                       public IntPtr credAttribute;
                       public string targetAlias;
                       public string userName;
                     }

                     public class Credential {
                       [DllImport("advapi32.dll", EntryPoint = "CredReadW", CharSet = CharSet.Unicode, SetLastError = true)]
                       private static extern bool CredRead(string target, int type, int reservedFlag, out IntPtr credentialPtr);

                       public static string GetUserPassword(string target)
                       {
                         CredentialMem credMem;
                         IntPtr credPtr;

                         if (CredRead(target, 1, 0, out credPtr))
                         {
                           credMem = Marshal.PtrToStructure<CredentialMem>(credPtr);
                           byte[] passwordBytes = new byte[credMem.credentialBlobSize];
                           Marshal.Copy(credMem.credentialBlob, passwordBytes, 0, credMem.credentialBlobSize);
                           return Encoding.Unicode.GetString(passwordBytes);
                         } else {
                           throw new Exception("No credentials found for target: " + target);
                         }
                       }
                     }
                   }
                   "@
                   Add-Type -TypeDefinition $code -Language CSharp
                   """;
                    LocalShell.getLocalPowershell().command(cmd).execute();
                }

                return LocalShell.getLocalPowershell()
                        .command("[CredManager.Credential]::GetUserPassword(\"" + key.replaceAll("\"", "`\"") + "\")")
                        .readStdoutOrThrow();
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).expected().handle();
                return null;
            }
        }
    }
}
