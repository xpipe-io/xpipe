package io.xpipe.app.password;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
import org.bouncycastle.util.Arrays;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cryptographic helper for KeePassXC communication.
 *
 * This implementation properly mimics TweetNaCl.js behavior using BouncyCastle,
 * implementing X25519 key exchange and XSalsa20-Poly1305 authenticated encryption
 * which is what KeePassXC expects.
 */
public class TweetNaClHelper {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final int KEY_SIZE = 32;
    public static final int NONCE_SIZE = 24;

    // Sigma constant ("expand 32-byte k")
    private static final byte[] SIGMA = {101, 120, 112, 97, 110, 100, 32, 51, 50, 45, 98, 121, 116, 101, 32, 107};

    public static class KeyPair {
        private final byte[] publicKey;
        private final byte[] secretKey;

        public KeyPair(byte[] publicKey, byte[] secretKey) {
            this.publicKey = publicKey;
            this.secretKey = secretKey;
        }

        public byte[] getPublicKey() {
            return publicKey;
        }

        public byte[] getSecretKey() {
            return secretKey;
        }
    }

    /**
     * Generate a new key pair.
     */
    public static KeyPair generateKeyPair() {
        X25519KeyPairGenerator keyGen = new X25519KeyPairGenerator();
        keyGen.init(new KeyGenerationParameters(SECURE_RANDOM, 0));
        AsymmetricCipherKeyPair keyPair = keyGen.generateKeyPair();

        X25519PrivateKeyParameters privateKey = (X25519PrivateKeyParameters) keyPair.getPrivate();
        X25519PublicKeyParameters publicKey = (X25519PublicKeyParameters) keyPair.getPublic();

        return new KeyPair(publicKey.getEncoded(), privateKey.getEncoded());
    }

    /**
     * Generate random bytes.
     */
    public static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * Encrypt a message using NaCl box.
     *
     * This uses X25519 for key exchange and XSalsa20-Poly1305 for authenticated encryption.
     * Follows the TweetNaCl.js implementation exactly.
     */
    public static byte[] box(byte[] message, byte[] nonce, byte[] theirPublicKey, byte[] ourSecretKey) {
        // Create a shared secret key for encryption - this is the 'beforenm' step
        byte[] k = boxBeforeNm(theirPublicKey, ourSecretKey);

        // Now use this key with secretbox (the 'afternm' step)
        return secretbox(message, nonce, k);
    }

    /**
     * Compute the shared key for box encryption (equivalent to nacl.box.before)
     */
    private static byte[] boxBeforeNm(byte[] theirPublicKey, byte[] ourSecretKey) {
        // First compute the X25519 shared secret
        byte[] sharedSecret = computeSharedSecret(theirPublicKey, ourSecretKey);

        // Then use hsalsa20 to derive the key for XSalsa20
        byte[] k = new byte[32];
        hsalsa20(k, new byte[16], sharedSecret, SIGMA);

        return k;
    }

    /**
     * Decrypt a message using NaCl box open.
     * Follows the TweetNaCl.js implementation exactly.
     */
    public static byte[] boxOpen(byte[] encryptedMessage, byte[] nonce, byte[] theirPublicKey, byte[] ourSecretKey) {
        // Create a shared secret key for decryption - this is the 'beforenm' step
        byte[] k = boxBeforeNm(theirPublicKey, ourSecretKey);

        // Now use this key with secretbox_open (the 'afternm' step)
        return secretboxOpen(encryptedMessage, nonce, k);
    }

    /**
     * Compute a shared secret using X25519.
     */
    private static byte[] computeSharedSecret(byte[] publicKey, byte[] secretKey) {
        try {
            X25519PublicKeyParameters publicParams = new X25519PublicKeyParameters(publicKey, 0);
            X25519PrivateKeyParameters privateParams = new X25519PrivateKeyParameters(secretKey, 0);

            X25519Agreement agreement = new X25519Agreement();
            agreement.init(privateParams);

            byte[] sharedSecret = new byte[agreement.getAgreementSize()];
            agreement.calculateAgreement(publicParams, sharedSecret, 0);

            return sharedSecret;
        } catch (Exception e) {
            throw new RuntimeException("Error computing shared secret: " + e.getMessage(), e);
        }
    }

    /**
     * Proper implementation of HSalsa20 function from NaCl, used to derive the subkey.
     * This matches the TweetNaCl.js implementation.
     */
    private static void hsalsa20(byte[] out, byte[] nonce, byte[] key, byte[] constants) {
        int[] x = new int[16]; // Working state

        // Load constants (sigma)
        x[0] = load32(constants, 0);
        x[5] = load32(constants, 4);
        x[10] = load32(constants, 8);
        x[15] = load32(constants, 12);

        // Load key
        x[1] = load32(key, 0);
        x[2] = load32(key, 4);
        x[3] = load32(key, 8);
        x[4] = load32(key, 12);
        x[11] = load32(key, 16);
        x[12] = load32(key, 20);
        x[13] = load32(key, 24);
        x[14] = load32(key, 28);

        // Load nonce
        x[6] = load32(nonce, 0);
        x[7] = load32(nonce, 4);
        x[8] = load32(nonce, 8);
        x[9] = load32(nonce, 12);

        // Perform 20 rounds of the Salsa20 core
        for (int i = 0; i < 20; i += 2) {
            // Column round
            x[4] ^= rotl32(x[0] + x[12], 7);
            x[8] ^= rotl32(x[4] + x[0], 9);
            x[12] ^= rotl32(x[8] + x[4], 13);
            x[0] ^= rotl32(x[12] + x[8], 18);

            x[9] ^= rotl32(x[5] + x[1], 7);
            x[13] ^= rotl32(x[9] + x[5], 9);
            x[1] ^= rotl32(x[13] + x[9], 13);
            x[5] ^= rotl32(x[1] + x[13], 18);

            x[14] ^= rotl32(x[10] + x[6], 7);
            x[2] ^= rotl32(x[14] + x[10], 9);
            x[6] ^= rotl32(x[2] + x[14], 13);
            x[10] ^= rotl32(x[6] + x[2], 18);

            x[3] ^= rotl32(x[15] + x[11], 7);
            x[7] ^= rotl32(x[3] + x[15], 9);
            x[11] ^= rotl32(x[7] + x[3], 13);
            x[15] ^= rotl32(x[11] + x[7], 18);

            // Diagonal round
            x[1] ^= rotl32(x[0] + x[3], 7);
            x[2] ^= rotl32(x[1] + x[0], 9);
            x[3] ^= rotl32(x[2] + x[1], 13);
            x[0] ^= rotl32(x[3] + x[2], 18);

            x[6] ^= rotl32(x[5] + x[4], 7);
            x[7] ^= rotl32(x[6] + x[5], 9);
            x[4] ^= rotl32(x[7] + x[6], 13);
            x[5] ^= rotl32(x[4] + x[7], 18);

            x[11] ^= rotl32(x[10] + x[9], 7);
            x[8] ^= rotl32(x[11] + x[10], 9);
            x[9] ^= rotl32(x[8] + x[11], 13);
            x[10] ^= rotl32(x[9] + x[8], 18);

            x[12] ^= rotl32(x[15] + x[14], 7);
            x[13] ^= rotl32(x[12] + x[15], 9);
            x[14] ^= rotl32(x[13] + x[12], 13);
            x[15] ^= rotl32(x[14] + x[13], 18);
        }

        // Extract the output
        store32(out, 0, x[0]);
        store32(out, 4, x[5]);
        store32(out, 8, x[10]);
        store32(out, 12, x[15]);
        store32(out, 16, x[6]);
        store32(out, 20, x[7]);
        store32(out, 24, x[8]);
        store32(out, 28, x[9]);
    }

    /**
     * Implementation of secretbox from NaCl.
     */
    private static byte[] secretbox(byte[] message, byte[] nonce, byte[] key) {
        // For compatibility with TweetNaCl, we implement the same logic
        // Our secretbox will combine XSalsa20 encryption with Poly1305 MAC

        try {
            // In TweetNaCl.js, secretbox adds 32 zero bytes before the message
            byte[] paddedMessage = new byte[32 + message.length];
            System.arraycopy(message, 0, paddedMessage, 32, message.length);

            // Apply XSalsa20 encryption
            byte[] c = new byte[paddedMessage.length];
            streamXorXSalsa20(c, paddedMessage, paddedMessage.length, nonce, key);

            // The first 16 bytes are used for the Poly1305 tag (MAC)
            byte[] tag = new byte[16];
            crypto_onetimeauth(tag, c, 32, c.length - 32, Arrays.copyOf(c, 32));

            // Copy tag into the first 16 bytes of c
            System.arraycopy(tag, 0, c, 16, 16);

            // Clear the first 16 bytes (not used in the result)
            for (int i = 0; i < 16; i++) {
                c[i] = 0;
            }

            // Return result skipping the first 16 bytes (boxzerobytes)
            return Arrays.copyOfRange(c, 16, c.length);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Implementation of secretbox_open from NaCl.
     */
    private static byte[] secretboxOpen(byte[] encryptedMessage, byte[] nonce, byte[] key) {
        // Check if the message is long enough
        if (encryptedMessage.length < 16) {
            return null; // Not enough data
        }

        try {
            // Reconstruct the ciphertext with boxzerobytes prefix
            byte[] c = new byte[16 + encryptedMessage.length];
            System.arraycopy(encryptedMessage, 0, c, 16, encryptedMessage.length);

            // Verify the Poly1305 authentication tag
            byte[] subkey = Arrays.copyOf(new byte[32], 32); // First 32 bytes of the keystream
            streamXSalsa20(subkey, 32, nonce, key);

            if (crypto_onetimeauth_verify(c, 16, c, 32, c.length - 32, subkey) != 0) {
                return null; // MAC verification failed
            }

            // Decrypt the message
            byte[] m = new byte[c.length];
            streamXorXSalsa20(m, c, c.length, nonce, key);

            // Return the actual message (skipping the 32 zero bytes prefix)
            return Arrays.copyOfRange(m, 32, m.length);
        } catch (Exception e) {
            return null; // Return null on decryption failure (as in NaCl)
        }
    }

    /**
     * Core XSalsa20 stream cipher function.
     */
    private static void streamXSalsa20(byte[] out, int outLength, byte[] nonce, byte[] key) {
        // First, derive a subkey using HSalsa20
        byte[] subkey = new byte[32];
        hsalsa20(subkey, Arrays.copyOf(nonce, 16), key, SIGMA);

        // Then use the subkey with the remaining bytes of the nonce
        streamSalsa20(out, outLength, Arrays.copyOfRange(nonce, 16, 24), subkey);
    }

    /**
     * XSalsa20 stream XOR function
     */
    private static void streamXorXSalsa20(byte[] c, byte[] m, int mlen, byte[] nonce, byte[] key) {
        // First, derive a subkey using HSalsa20
        byte[] subkey = new byte[32];
        hsalsa20(subkey, Arrays.copyOf(nonce, 16), key, SIGMA);

        // Then use the subkey with the remaining bytes of the nonce
        streamXorSalsa20(c, m, mlen, Arrays.copyOfRange(nonce, 16, 24), subkey);
    }

    /**
     * Core Salsa20 stream cipher function.
     */
    private static void streamSalsa20(byte[] out, int outLength, byte[] nonce, byte[] key) {
        byte[] zeros = new byte[outLength];
        streamXorSalsa20(out, zeros, outLength, nonce, key);
    }

    /**
     * Salsa20 stream XOR function
     */
    private static void streamXorSalsa20(byte[] c, byte[] m, int mlen, byte[] nonce, byte[] key) {
        // Use BouncyCastle's Salsa20 implementation
        org.bouncycastle.crypto.engines.Salsa20Engine salsa20 = new org.bouncycastle.crypto.engines.Salsa20Engine();
        org.bouncycastle.crypto.params.ParametersWithIV params = new org.bouncycastle.crypto.params.ParametersWithIV(
                new org.bouncycastle.crypto.params.KeyParameter(key), nonce);
        salsa20.init(true, params);

        salsa20.processBytes(m, 0, mlen, c, 0);
    }

    /**
     * Poly1305 one-time authentication.
     */
    private static void crypto_onetimeauth(byte[] out, byte[] m, int mpos, int mlen, byte[] key) {
        org.bouncycastle.crypto.macs.Poly1305 poly1305 = new org.bouncycastle.crypto.macs.Poly1305();
        poly1305.init(new org.bouncycastle.crypto.params.KeyParameter(key));

        poly1305.update(m, mpos, mlen);

        poly1305.doFinal(out, 0);
    }

    /**
     * Verify a Poly1305 one-time authentication tag.
     */
    private static int crypto_onetimeauth_verify(byte[] h, int hpos, byte[] m, int mpos, int mlen, byte[] key) {
        byte[] correct = new byte[16];
        crypto_onetimeauth(correct, m, mpos, mlen, key);
        return crypto_verify_16(h, hpos, correct, 0);
    }

    /**
     * Verify 16 bytes in constant time.
     */
    private static int crypto_verify_16(byte[] x, int xpos, byte[] y, int ypos) {
        return constantTimeEquals(Arrays.copyOfRange(x, xpos, xpos + 16), Arrays.copyOfRange(y, ypos, ypos + 16))
                ? 0
                : -1;
    }

    /**
     * Helper for loading 32-bit integers (little-endian).
     */
    private static int load32(byte[] src, int offset) {
        int u = src[offset] & 0xff;
        u |= (src[offset + 1] & 0xff) << 8;
        u |= (src[offset + 2] & 0xff) << 16;
        u |= (src[offset + 3] & 0xff) << 24;
        return u;
    }

    /**
     * Helper for storing 32-bit integers (little-endian).
     */
    private static void store32(byte[] dst, int offset, int u) {
        dst[offset] = (byte) (u & 0xff);
        dst[offset + 1] = (byte) ((u >>> 8) & 0xff);
        dst[offset + 2] = (byte) ((u >>> 16) & 0xff);
        dst[offset + 3] = (byte) ((u >>> 24) & 0xff);
    }

    /**
     * Rotate a 32-bit integer left by the specified number of bits.
     */
    private static int rotl32(int x, int b) {
        return ((x << b) | (x >>> (32 - b)));
    }

    /**
     * Compare two byte arrays in constant time to prevent timing attacks.
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * Encode bytes as Base64.
     */
    public static String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decode Base64 to bytes.
     */
    public static byte[] decodeBase64(String data) {
        return Base64.getDecoder().decode(data);
    }
}
