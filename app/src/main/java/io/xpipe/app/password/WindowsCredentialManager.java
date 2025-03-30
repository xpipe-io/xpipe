package io.xpipe.app.password;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.LocalShell;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

@JsonTypeName("windowsCredentialManager")
@Value
public class WindowsCredentialManager implements PasswordManager {

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

    @Override
    public String getKeyPlaceholder() {
        return "<credential name>";
    }
}
