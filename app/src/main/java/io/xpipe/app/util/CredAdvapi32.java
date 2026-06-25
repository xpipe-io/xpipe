package io.xpipe.app.util;

import com.sun.jna.LastErrorException;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class exposes functions from credential manager on Windows platform
 * via JNA.
 *
 * Please refer to MSDN documentations for each method usage pattern
 */
public interface CredAdvapi32 extends StdCallLibrary {

    CredAdvapi32 INSTANCE = Native.load("Advapi32", CredAdvapi32.class, W32APIOptions.UNICODE_OPTIONS);
    /**
     * CredRead flag
     */
    int CRED_FLAGS_PROMPT_NOW = 0x0002;
    int CRED_FLAGS_USERNAME_TARGET = 0x0004;

    /**
     * Type of Credential
     */
    int CRED_TYPE_GENERIC = 1;
    int CRED_TYPE_DOMAIN_PASSWORD = 2;
    int CRED_TYPE_DOMAIN_CERTIFICATE = 3;
    int CRED_TYPE_DOMAIN_VISIBLE_PASSWORD = 4;
    int CRED_TYPE_GENERIC_CERTIFICATE = 5;
    int CRED_TYPE_DOMAIN_EXTENDED = 6;
    int CRED_TYPE_MAXIMUM = 7;       // Maximum supported cred type
    int CRED_TYPE_MAXIMUM_EX = CRED_TYPE_MAXIMUM + 1000;

    /**
     * CredWrite flag
     */
    int CRED_PRESERVE_CREDENTIAL_BLOB = 0x1;

    /**
     * Values of the Credential Persist field
     */
    int CRED_PERSIST_NONE = 0;
    int CRED_PERSIST_SESSION = 1;
    int CRED_PERSIST_LOCAL_MACHINE = 2;
    int CRED_PERSIST_ENTERPRISE = 3;

    /**
     * Credential attributes
     *
     * https://msdn.microsoft.com/en-us/library/windows/desktop/aa374790(v=vs.85).aspx
     *
     * typedef struct _CREDENTIAL_ATTRIBUTE {
     *   LPTSTR Keyword;
     *   DWORD  Flags;
     *   DWORD  ValueSize;
     *   LPBYTE Value;
     * } CREDENTIAL_ATTRIBUTE, *PCREDENTIAL_ATTRIBUTE;
     *
     */
    class CREDENTIAL_ATTRIBUTE extends Structure {

        public static class ByReference extends CREDENTIAL_ATTRIBUTE implements Structure.ByReference { }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("Keyword",
                    "Flags",
                    "ValueSize",
                    "Value");
        }

        /**
         *    Name of the application-specific attribute. Names should be of the form <CompanyName>_<Name>.
         *    This member cannot be longer than CRED_MAX_STRING_LENGTH (256) characters.
         */
        public String Keyword;

        /**
         *   Identifies characteristics of the credential attribute. This member is reserved and should be originally
         *   initialized as zero and not otherwise altered to permit future enhancement.
         */
        public int Flags;

        /**
         *   Length of Value in bytes. This member cannot be larger than CRED_MAX_VALUE_SIZE (256).
         */
        public int ValueSize;

        /**
         *   Data associated with the attribute. By convention, if Value is a text string, then Value should not
         *   include the trailing zero character and should be in UNICODE.
         *
         *   Credentials are expected to be portable. The application should take care to ensure that the data in
         *   value is portable. It is the responsibility of the application to define the byte-endian and alignment
         *   of the data in Value.
         */
        public Pointer Value;
    }

    /**
     * Pointer to {@See CREDENTIAL_ATTRIBUTE} struct
     */
    class PCREDENTIAL_ATTRIBUTE extends Structure {

        @Override
        protected List<String> getFieldOrder() {
            return Collections.singletonList("credential_attribute");
        }

        public PCREDENTIAL_ATTRIBUTE() {
            super();
        }

        public PCREDENTIAL_ATTRIBUTE(byte[] data) {
            super(new Memory(data.length));
            getPointer().write(0, data, 0, data.length);
            read();
        }

        public PCREDENTIAL_ATTRIBUTE(Pointer memory) {
            super(memory);
            read();
        }

        public Pointer credential_attribute;
    }

    /**
     * The CREDENTIAL structure contains an individual credential
     *
     * https://msdn.microsoft.com/en-us/library/windows/desktop/aa374788(v=vs.85).aspx
     *
     * typedef struct _CREDENTIAL {
     *   DWORD                 Flags;
     *   DWORD                 Type;
     *   LPTSTR                TargetName;
     *   LPTSTR                Comment;
     *   FILETIME              LastWritten;
     *   DWORD                 CredentialBlobSize;
     *   LPBYTE                CredentialBlob;
     *   DWORD                 Persist;
     *   DWORD                 AttributeCount;
     *   PCREDENTIAL_ATTRIBUTE Attributes;
     *   LPTSTR                TargetAlias;
     *   LPTSTR                UserName;
     * } CREDENTIAL, *PCREDENTIAL;
     */
    class CREDENTIAL extends Structure {

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("Flags",
                    "Type",
                    "TargetName",
                    "Comment",
                    "LastWritten",
                    "CredentialBlobSize",
                    "CredentialBlob",
                    "Persist",
                    "AttributeCount",
                    "Attributes",
                    "TargetAlias",
                    "UserName");
        }

        public CREDENTIAL() {
            super();
        }

        public CREDENTIAL(final int size) {
            super(new Memory(size));
        }

        public CREDENTIAL(Pointer memory) {
            super(memory);
            read();
        }

        /**
         *   A bit member that identifies characteristics of the credential. Undefined bits should be initialized
         *   as zero and not otherwise altered to permit future enhancement.
         *
         *   See MSDN doc for all possible flags
         */
        public int Flags;

        /**
         *   The type of the credential. This member cannot be changed after the credential is created.
         *
         *   See MSDN doc for all possible types
         */
        public int Type;

        /**
         *   The name of the credential. The TargetName and Type members uniquely identify the credential.
         *   This member cannot be changed after the credential is created. Instead, the credential with the old
         *   name should be deleted and the credential with the new name created.
         *
         *   See MSDN doc for additional requirement
         */
        public String TargetName;

        /**
         *   A string comment from the user that describes this credential. This member cannot be longer than
         *   CRED_MAX_STRING_LENGTH (256) characters.
         */
        public String Comment;

        /**
         *   The time, in Coordinated Universal Time (Greenwich Mean Time), of the last modification of the credential.
         *   For write operations, the value of this member is ignored.
         */
        public WinBase.FILETIME LastWritten;

        /**
         *   The size, in bytes, of the CredentialBlob member. This member cannot be larger than
         *   CRED_MAX_CREDENTIAL_BLOB_SIZE (512) bytes.
         */
        public int CredentialBlobSize;

        /**
         *   Secret data for the credential. The CredentialBlob member can be both read and written.
         *   If the Type member is CRED_TYPE_DOMAIN_PASSWORD, this member contains the plaintext Unicode password
         *   for UserName. The CredentialBlob and CredentialBlobSize members do not include a trailing zero character.
         *   Also, for CRED_TYPE_DOMAIN_PASSWORD, this member can only be read by the authentication packages.
         *
         *   If the Type member is CRED_TYPE_DOMAIN_CERTIFICATE, this member contains the clear test
         *   Unicode PIN for UserName. The CredentialBlob and CredentialBlobSize members do not include a trailing
         *   zero character. Also, this member can only be read by the authentication packages.
         *
         *   If the Type member is CRED_TYPE_GENERIC, this member is defined by the application.
         *   Credentials are expected to be portable. Applications should ensure that the data in CredentialBlob is
         *   portable. The application defines the byte-endian and alignment of the data in CredentialBlob.
         */
        public Pointer CredentialBlob;

        /**
         *   Defines the persistence of this credential. This member can be read and written.
         *
         *   See MSDN doc for all possible values
         */
        public int Persist;

        /**
         *   The number of application-defined attributes to be associated with the credential. This member can be
         *   read and written. Its value cannot be greater than CRED_MAX_ATTRIBUTES (64).
         */
        public int AttributeCount;

        /**
         *   Application-defined attributes that are associated with the credential. This member can be read
         *   and written.
         */
        //notTODO: Need to make this into array
        public CREDENTIAL_ATTRIBUTE.ByReference Attributes;

        /**
         *   Alias for the TargetName member. This member can be read and written. It cannot be longer than
         *   CRED_MAX_STRING_LENGTH (256) characters.
         *
         *   If the credential Type is CRED_TYPE_GENERIC, this member can be non-NULL, but the credential manager
         *   ignores the member.
         */
        public String TargetAlias;

        /**
         *   The user name of the account used to connect to TargetName.
         *   If the credential Type is CRED_TYPE_DOMAIN_PASSWORD, this member can be either a DomainName\UserName
         *   or a UPN.
         *
         *   If the credential Type is CRED_TYPE_DOMAIN_CERTIFICATE, this member must be a marshaled certificate
         *   reference created by calling CredMarshalCredential with a CertCredential.
         *
         *   If the credential Type is CRED_TYPE_GENERIC, this member can be non-NULL, but the credential manager
         *   ignores the member.
         *
         *   This member cannot be longer than CRED_MAX_USERNAME_LENGTH (513) characters.
         */
        public String UserName;
    }

    /**
     *  Pointer to {@see CREDENTIAL} struct
     */
    class PCREDENTIAL extends Structure {

        @Override
        protected List<String> getFieldOrder() {
            return Collections.singletonList("credential");
        }

        public PCREDENTIAL() {
            super();
        }

        public PCREDENTIAL(byte[] data) {
            super(new Memory(data.length));
            getPointer().write(0, data, 0, data.length);
            read();
        }

        public PCREDENTIAL(Pointer memory) {
            super(memory);
            read();
        }

        public Pointer credential;
    }

    /**
     * The CredRead function reads a credential from the user's credential set.
     *
     * The credential set used is the one associated with the logon session of the current token.
     * The token must not have the user's SID disabled.
     *
     * https://msdn.microsoft.com/en-us/library/windows/desktop/aa374804(v=vs.85).aspx
     *
     * @param targetName
     *      String that contains the name of the credential to read.
     * @param type
     *      Type of the credential to read. Type must be one of the CRED_TYPE_* defined types.
     * @param flags
     *      Currently reserved and must be zero.
     * @param pcredential
     *      Out - Pointer to a single allocated block buffer to return the credential.
     *      Any pointers contained within the buffer are pointers to locations within this single allocated block.
     *      The single returned buffer must be freed by calling <code>CredFree</code>.
     *
     * @return
     *      True if CredRead succeeded, false otherwise
     *
     * @throws LastErrorException
     *      GetLastError
     */
    boolean CredRead(String targetName, int type, int flags, PCREDENTIAL pcredential) throws LastErrorException;

    /**
     * The CredWrite function creates a new credential or modifies an existing credential in the user's credential set.
     * The new credential is associated with the logon session of the current token. The token must not have the
     * user's security identifier (SID) disabled.
     *
     * https://msdn.microsoft.com/en-us/library/windows/desktop/aa375187(v=vs.85).aspx
     *
     * @param credential
     *      A CREDENTIAL structure to be written.
     * @param flags
     *      Flags that control the function's operation. The following flag is defined.
     *      CRED_PRESERVE_CREDENTIAL_BLOB:
     *          The credential BLOB from an existing credential is preserved with the same
     *          credential name and credential type. The CredentialBlobSize of the passed
     *          in Credential structure must be zero.
     *
     * @return
     *      True if CredWrite succeeded, false otherwise
     *
     * @throws LastErrorException
     *      GetLastError
     */
    boolean CredWrite(CREDENTIAL credential, int flags) throws LastErrorException;

    /**
     * The CredDelete function deletes a credential from the user's credential set. The credential set used is the one
     * associated with the logon session of the current token. The token must not have the user's SID disabled.
     *
     * https://msdn.microsoft.com/en-us/library/windows/desktop/aa374787(v=vs.85).aspx
     *
     * @param targetName
     *      String that contains the name of the credential to read.
     * @param type
     *      Type of the credential to delete. Must be one of the CRED_TYPE_* defined types. For a list of the
     *      defined types, see the Type member of the CREDENTIAL structure.
     *      If the value of this parameter is CRED_TYPE_DOMAIN_EXTENDED, this function can delete a credential that
     *      specifies a user name when there are multiple credentials for the same target. The value of the TargetName
     *      parameter must specify the user name as Target|UserName.
     * @param flags
     *      Reserved and must be zero.
     *
     * @return
     *      True if CredDelete succeeded, false otherwise
     *
     * @throws LastErrorException
     *      GetLastError
     */
    boolean CredDelete(String targetName, int type, int flags) throws LastErrorException;

    /**
     * The CredFree function frees a buffer returned by any of the credentials management functions.
     *
     * https://msdn.microsoft.com/en-us/library/windows/desktop/aa374796(v=vs.85).aspx
     *
     * @param credential
     *      Pointer to CREDENTIAL to be freed
     *
     * @throws LastErrorException
     *      GetLastError
     */
    void CredFree(Pointer credential) throws LastErrorException;
}
