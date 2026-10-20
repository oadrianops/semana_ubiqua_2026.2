package br.com.nandesk.notification.infra;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Web Push sem dependências externas (ADR-0010): cifra {@code aes128gcm} (RFC 8188/8291) e JWT VAPID ES256
 * (RFC 8292) com a criptografia do JDK.
 */
public final class WebPushCrypto {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RECORD_SIZE = 4096;
    private static final ECParameterSpec P256 = p256();

    private WebPushCrypto() {}

    /** Corpo cifrado: cabeçalho (salt, rs, keyid = chave pública efêmera) + texto cifrado de um único registro. */
    public static byte[] encrypt(byte[] payload, byte[] receiverPublic, byte[] authSecret)
            throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair ephemeral = gen.generateKeyPair();
        byte[] senderPublic = encodePoint((ECPublicKey) ephemeral.getPublic());

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(ephemeral.getPrivate());
        ka.doPhase(decodePoint(receiverPublic), true);
        byte[] ecdhSecret = ka.generateSecret();

        byte[] prkKey = hmac(authSecret, ecdhSecret);
        byte[] keyInfo = concat(
                "WebPush: info\0".getBytes(StandardCharsets.US_ASCII), receiverPublic, senderPublic, new byte[] {1});
        byte[] ikm = hmac(prkKey, keyInfo);

        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] prk = hmac(salt, ikm);
        byte[] cek =
                Arrays.copyOf(hmac(prk, "Content-Encoding: aes128gcm\0\1".getBytes(StandardCharsets.US_ASCII)), 16);
        byte[] nonce = Arrays.copyOf(hmac(prk, "Content-Encoding: nonce\0\1".getBytes(StandardCharsets.US_ASCII)), 12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] cipherText = cipher.doFinal(concat(payload, new byte[] {2}));

        return ByteBuffer.allocate(16 + 4 + 1 + senderPublic.length + cipherText.length)
                .put(salt)
                .putInt(RECORD_SIZE)
                .put((byte) senderPublic.length)
                .put(senderPublic)
                .put(cipherText)
                .array();
    }

    /** Par de chaves VAPID do servidor (Base64URL: ponto público não comprimido de 65 bytes e escalar de 32 bytes). */
    public record Vapid(ECPublicKey publicKey, PrivateKey privateKey, String publicKeyB64) {

        public static Vapid of(String publicB64, String privateB64) throws GeneralSecurityException {
            byte[] pub = Base64.getUrlDecoder().decode(publicB64.trim());
            byte[] priv = Base64.getUrlDecoder().decode(privateB64.trim());
            PrivateKey privateKey =
                    KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, priv), P256));
            return new Vapid(decodePoint(pub), privateKey, b64(pub));
        }

        /** JWT ES256 com {@code aud} = origem do serviço de push, {@code exp} e {@code sub}. */
        public String jwt(String audience, String subject, long expiresAtEpochSeconds) throws GeneralSecurityException {
            String header = b64("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
            String claims = b64(
                    ("{\"aud\":\"" + audience + "\",\"exp\":" + expiresAtEpochSeconds + ",\"sub\":\"" + subject + "\"}")
                            .getBytes(StandardCharsets.UTF_8));
            Signature signer = Signature.getInstance("SHA256withECDSAinP1363Format");
            signer.initSign(privateKey);
            signer.update((header + "." + claims).getBytes(StandardCharsets.US_ASCII));
            return header + "." + claims + "." + b64(signer.sign());
        }
    }

    public static byte[] encodePoint(ECPublicKey key) {
        byte[] x = fixed(key.getW().getAffineX());
        byte[] y = fixed(key.getW().getAffineY());
        return concat(new byte[] {4}, x, y);
    }

    public static ECPublicKey decodePoint(byte[] uncompressed) throws GeneralSecurityException {
        if (uncompressed.length != 65 || uncompressed[0] != 4) {
            throw new GeneralSecurityException("Chave pública P-256 inválida");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1, 33));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(uncompressed, 33, 65));
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), P256));
    }

    public static byte[] privateScalar(PrivateKey key) {
        return fixed(((ECPrivateKey) key).getS());
    }

    public static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    public static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            out.writeBytes(p);
        }
        return out.toByteArray();
    }

    public static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] fixed(BigInteger value) {
        byte[] raw = value.toByteArray();
        if (raw.length == 32) {
            return raw;
        }
        byte[] out = new byte[32];
        int copy = Math.min(raw.length, 32);
        System.arraycopy(raw, raw.length - copy, out, 32 - copy, copy);
        return out;
    }

    private static ECParameterSpec p256() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            return ((ECPublicKey) gen.generateKeyPair().getPublic()).getParams();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
