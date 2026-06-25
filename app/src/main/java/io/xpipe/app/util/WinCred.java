package io.xpipe.app.util;


import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.sun.jna.LastErrorException;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.win32.W32APIOptions;
import io.xpipe.app.issue.ErrorEventFactory;
import lombok.Value;

public class WinCred {

    @Value
    public static class Credential {
        String target;
        String username;
        String password;
    }

    private static Boolean libraryLoaded;
    private static CredAdvapi32 INSTANCE;

    private static synchronized boolean isLibrarySupported() {
        if (libraryLoaded != null) {
            return libraryLoaded;
        }

        try {
            INSTANCE = Native.load("Advapi32", CredAdvapi32.class, W32APIOptions.UNICODE_OPTIONS);
            return (libraryLoaded = true);
        } catch (Throwable t) {
            libraryLoaded = false;
            ErrorEventFactory.fromThrowable(t)
                    .description("Unable to load native library Advapi32.dll for Windows credentials queries."
                            + " Credential operations will fail and some functionality will be unavailable")
                    .handle();
            return false;
        }
    }

    public static synchronized Optional<Credential> getCredential(String target, int type) {
        if (!isLibrarySupported()) {
            return Optional.empty();
        }

        CredAdvapi32.PCREDENTIAL pcredMem = new CredAdvapi32.PCREDENTIAL();

        try {
            if (INSTANCE.CredRead(target, type, 0, pcredMem)) {
                CredAdvapi32.CREDENTIAL credMem = new CredAdvapi32.CREDENTIAL(pcredMem.credential);
                if (credMem.CredentialBlob == null) {
                    return Optional.empty();
                }

                byte[] passwordBytes = credMem.CredentialBlob.getByteArray(0, credMem.CredentialBlobSize);
                String password = new String(passwordBytes, StandardCharsets.UTF_16LE);
                Credential cred = new Credential(credMem.TargetName, credMem.UserName, password);
                return Optional.of(cred);
            } else {
                return Optional.empty();
            }
        } finally {
            INSTANCE.CredFree(pcredMem.credential);
        }
    }

    public static void setCredential(String target, int type, int persist, String userName, SecretValue password) {
        if (!isLibrarySupported()) {
            return;
        }

        CredAdvapi32.CREDENTIAL credMem = new CredAdvapi32.CREDENTIAL();
        credMem.Flags = 0;
        credMem.TargetName = target;
        credMem.Type = type;
        credMem.UserName = userName;
        credMem.AttributeCount = 0;
        credMem.Persist = persist;
        byte[] bpassword = password.getSecretValue().getBytes(StandardCharsets.UTF_16LE);
        credMem.CredentialBlobSize = bpassword.length;
        credMem.CredentialBlob = getPointer(bpassword);
        if (!INSTANCE.CredWrite(credMem, 0)) {
            int err = Native.getLastError();
            throw new LastErrorException(err);
        }
    }

    public static void deleteCredential(String target, int type) {
        if (!INSTANCE.CredDelete(target, type, 0)) {
            int err = Native.getLastError();
            throw new LastErrorException(err);
        }
    }

    private static Pointer getPointer(byte[] array) {
        Pointer p = new Memory(array.length);
        p.write(0, array, 0, array.length);
        return p;
    }
}
