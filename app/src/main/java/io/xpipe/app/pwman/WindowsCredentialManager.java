package io.xpipe.app.pwman;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.InPlaceSecretValue;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

@JsonTypeName("windowsCredentialManager")
@Value
public class WindowsCredentialManager implements PasswordManager {

    private static boolean loaded = false;

    @Override
    public synchronized CredentialResult retrieveCredentials(String key) {
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
                LocalShell.getLocalPowershell().command(cmd).execute();
            }

            var username = LocalShell.getLocalPowershell()
                    .command("[CredManager.Credential]::GetUserName(\"" + key.replaceAll("\"", "`\"") + "\")")
                    .sensitive()
                    .readStdoutOrThrow();
            var password = LocalShell.getLocalPowershell()
                    .command("[CredManager.Credential]::GetUserPassword(\"" + key.replaceAll("\"", "`\"") + "\")")
                    .sensitive()
                    .readStdoutOrThrow();
            return new CredentialResult(username, password.isEmpty() ? null : InPlaceSecretValue.of(password));
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).expected().handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return "Credential name";
    }
}
