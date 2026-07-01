package mx.infotec.imss.infrastructure.security;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * Cifra la credencial con AES-256-GCM (autenticado). Genera un IV aleatorio por
 * operacion y evita convertir el password a String: trabaja con char[]/byte[] y
 * limpia los buffers intermedios.
 */
@Component
public class AesGcmCredentialCipher implements CredentialCipher {

    private static final int IV_BYTES = 12;   // recomendado para GCM
    private static final int TAG_BITS = 128;  // tag de autenticacion
    private static final int KEY_BYTES = 32;  // AES-256

    private final SecretKey key;
    private final SecureRandom rng = new SecureRandom();

    public AesGcmCredentialCipher(CipherProperties props) {
        byte[] raw = Base64.getDecoder().decode(props.getCipherKey());
        if (raw.length != KEY_BYTES) {
            throw new IllegalStateException(
                    "app.security.cipher-key debe ser AES-256 (32 bytes en Base64); se recibieron " + raw.length);
        }
        this.key = new SecretKeySpec(raw, "AES");
        Arrays.fill(raw, (byte) 0);
    }

    @Override
    public SessionCredential seal(String user, char[] password) {
        byte[] pwdBytes = toUtf8(password);
        try {
            byte[] iv = new byte[IV_BYTES];
            rng.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = c.doFinal(pwdBytes);
            return new SessionCredential(user, iv, cipherText);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Fallo al cifrar la credencial", e);
        } finally {
            Arrays.fill(pwdBytes, (byte) 0);
        }
    }

    @Override
    public char[] open(SessionCredential sc) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, sc.getIv()));
            byte[] pwdBytes = c.doFinal(sc.getCipherText());
            try {
                return toChars(pwdBytes);
            } finally {
                Arrays.fill(pwdBytes, (byte) 0);
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Fallo al descifrar la credencial", e);
        }
    }

    private static byte[] toUtf8(char[] chars) {
        CharBuffer cb = CharBuffer.wrap(chars);
        ByteBuffer bb = StandardCharsets.UTF_8.encode(cb);
        byte[] out = new byte[bb.remaining()];
        bb.get(out);
        if (bb.hasArray()) {
            Arrays.fill(bb.array(), (byte) 0);
        }
        return out;
    }

    private static char[] toChars(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        CharBuffer cb = StandardCharsets.UTF_8.decode(bb);
        char[] out = new char[cb.remaining()];
        cb.get(out);
        if (cb.hasArray()) {
            Arrays.fill(cb.array(), '\0');
        }
        return out;
    }
}
