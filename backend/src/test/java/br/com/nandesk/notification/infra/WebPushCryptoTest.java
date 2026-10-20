package br.com.nandesk.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-14")
class WebPushCryptoTest {

    @Test
    @DisplayName("RF-14: a mensagem cifrada (aes128gcm, RFC 8291) é decifrada pelo navegador de destino")
    void encryptsForReceiver() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair receiver = gen.generateKeyPair();
        byte[] receiverPublic = WebPushCrypto.encodePoint((ECPublicKey) receiver.getPublic());
        byte[] auth = new byte[16];
        new java.security.SecureRandom().nextBytes(auth);

        byte[] body = WebPushCrypto.encrypt(
                "{\"title\":\"Nova tarefa\"}".getBytes(StandardCharsets.UTF_8), receiverPublic, auth);

        // Decifra como o navegador faria (RFC 8291, seção 3).
        ByteBuffer buf = ByteBuffer.wrap(body);
        byte[] salt = new byte[16];
        buf.get(salt);
        int rs = buf.getInt();
        int idLen = buf.get() & 0xFF;
        byte[] senderPublic = new byte[idLen];
        buf.get(senderPublic);
        byte[] cipherText = new byte[buf.remaining()];
        buf.get(cipherText);
        assertThat(rs).isEqualTo(4096);

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(receiver.getPrivate());
        ka.doPhase(WebPushCrypto.decodePoint(senderPublic), true);
        byte[] ecdh = ka.generateSecret();
        byte[] prkKey = WebPushCrypto.hmac(auth, ecdh);
        byte[] keyInfo = WebPushCrypto.concat(
                "WebPush: info\0".getBytes(StandardCharsets.US_ASCII), receiverPublic, senderPublic, new byte[] {1});
        byte[] ikm = WebPushCrypto.hmac(prkKey, keyInfo);
        byte[] prk = WebPushCrypto.hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(
                WebPushCrypto.hmac(prk, "Content-Encoding: aes128gcm\0\1".getBytes(StandardCharsets.US_ASCII)), 16);
        byte[] nonce = Arrays.copyOf(
                WebPushCrypto.hmac(prk, "Content-Encoding: nonce\0\1".getBytes(StandardCharsets.US_ASCII)), 12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] plain = cipher.doFinal(cipherText);
        assertThat(plain[plain.length - 1]).isEqualTo((byte) 2);
        assertThat(new String(plain, 0, plain.length - 1, StandardCharsets.UTF_8))
                .isEqualTo("{\"title\":\"Nova tarefa\"}");
    }

    @Test
    @DisplayName("RF-14: o JWT do VAPID (ES256) é verificável com a chave pública do servidor")
    void vapidJwtIsValid() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair vapid = gen.generateKeyPair();
        String publicB64 = WebPushCrypto.b64(WebPushCrypto.encodePoint((ECPublicKey) vapid.getPublic()));
        String privateB64 = WebPushCrypto.b64(WebPushCrypto.privateScalar(vapid.getPrivate()));
        WebPushCrypto.Vapid keys = WebPushCrypto.Vapid.of(publicB64, privateB64);

        String jwt = keys.jwt("https://fcm.googleapis.com", "mailto:contato@nandesk.com.br", 1_800_000_000L);
        String[] parts = jwt.split("\\.");
        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(vapid.getPublic());
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(Base64.getUrlDecoder().decode(parts[2]))).isTrue();
        assertThat(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8))
                .contains("\"aud\":\"https://fcm.googleapis.com\"");
    }
}
