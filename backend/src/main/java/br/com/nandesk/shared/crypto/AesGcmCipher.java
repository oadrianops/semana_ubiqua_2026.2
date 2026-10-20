package br.com.nandesk.shared.crypto;

import br.com.nandesk.shared.config.AppProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM para a chave PIX (seções 12.6 e 13.1). Formato gravado: versão da chave (1 byte) + nonce
 * (12 bytes) + texto cifrado com tag. A versão permite rotacionar a chave sem perder os dados antigos.
 */
@Component
public class AesGcmCipher {

    private static final Logger log = LoggerFactory.getLogger(AesGcmCipher.class);
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final byte version;
    private final SecureRandom random = new SecureRandom();

    public AesGcmCipher(AppProperties properties) {
        String configured = properties.crypto().pixKey();
        byte[] raw;
        if (configured == null || configured.isBlank()) {
            log.warn("APP_CRYPTO_PIX_KEY ausente: usando chave efêmera (somente desenvolvimento/testes).");
            raw = new byte[32];
            random.nextBytes(raw);
        } else {
            raw = Base64.getDecoder().decode(configured.trim());
        }
        if (raw.length != 32) {
            throw new IllegalStateException("APP_CRYPTO_PIX_KEY deve ter 32 bytes em Base64");
        }
        this.key = new SecretKeySpec(raw, "AES");
        this.version = (byte) properties.crypto().pixKeyVersion();
    }

    public byte[] encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(new byte[] {version});
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(1 + NONCE_BYTES + encrypted.length)
                    .put(version)
                    .put(nonce)
                    .put(encrypted)
                    .array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar", e);
        }
    }

    public String decrypt(byte[] payload) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte storedVersion = buffer.get();
            if (storedVersion != version) {
                throw new IllegalStateException("Versão de chave desconhecida: " + storedVersion);
            }
            byte[] nonce = new byte[NONCE_BYTES];
            buffer.get(nonce);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(new byte[] {storedVersion});
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao decifrar", e);
        }
    }
}
