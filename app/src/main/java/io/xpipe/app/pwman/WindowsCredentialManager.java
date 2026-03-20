package io.xpipe.app.pwman;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.LocalShell;

import javafx.beans.property.Property;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("windowsCredentialManager")
@Builder
@Jacksonized
public class WindowsCredentialManager implements PasswordManager {

    private static boolean loaded = false;

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
            if (!loaded) {
                loaded = true;

                var shell = LocalShell.getLocalPowershell();
                if (shell.isEmpty()) {
                    return null;
                }

                var cmd = """
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

                              public static string GetUserName(string target)
                              {
                                CredentialMem credMem;
                                IntPtr credPtr;

                                if (CredRead(target, 1, 0, out credPtr))
                                {
                                  credMem = Marshal.PtrToStructure<CredentialMem>(credPtr);
                                  return credMem.userName;
                                } else {
                                  throw new Exception("No credentials found for target: " + target);
                                }
                              }

                              public static string GetUserPassword(string target)
                              {
                                CredentialMem credMem;
                                IntPtr credPtr;

                                if (CredRead(target, 1, 0, out credPtr))
                                {
                                  credMem = Marshal.PtrToStructure<CredentialMem>(credPtr);
                                  if (credMem.credentialBlobSize == 0)
                                  {
                                    return "";
                                  }
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
                shell.get().command(cmd).execute();
            }

            var shell = LocalShell.getLocalPowershell();
            if (shell.isEmpty()) {
                return null;
            }

            var username = shell.get()
                    .command("[CredManager.Credential]::GetUserName(\"" + key.replaceAll("\"", "`\"") + "\")")
                    .sensitive()
                    .readStdoutOrThrow();
            var password = shell.get()
                    .command("[CredManager.Credential]::GetUserPassword(\"" + key.replaceAll("\"", "`\"") + "\")")
                    .sensitive()
                    .readStdoutOrThrow();
            return Result.of(Credentials.of(username, password), null);
        } catch (Exception ex) {
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
