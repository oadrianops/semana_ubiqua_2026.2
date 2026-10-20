package br.com.nandesk.shared.security;

import br.com.nandesk.shared.config.AppProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Par de chaves RSA do JWT (RS256 — 12.1). Ordem de leitura: PEM nas variáveis de ambiente, arquivos PEM
 * indicados por caminho e, só na falta dos dois, um par efêmero (as sessões caem a cada reinício).
 */
@Component
public class JwtKeys {

    private static final Logger log = LoggerFactory.getLogger(JwtKeys.class);

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final String keyId;

    public JwtKeys(AppProperties properties) {
        AppProperties.Jwt jwt = properties.jwt();
        this.keyId = jwt.keyId();
        String privatePem = firstNonBlank(jwt.privateKeyPem(), read(jwt.privateKeyPath()));
        String publicPem = firstNonBlank(jwt.publicKeyPem(), read(jwt.publicKeyPath()));
        if (privatePem != null && publicPem != null) {
            this.privateKey = (RSAPrivateKey) parsePrivate(privatePem);
            this.publicKey = (RSAPublicKey) parsePublic(publicPem);
        } else {
            log.warn("Chaves JWT não configuradas: gerando par efêmero (sessões não sobrevivem a reinícios).");
            KeyPair pair = generate();
            this.privateKey = (RSAPrivateKey) pair.getPrivate();
            this.publicKey = (RSAPublicKey) pair.getPublic();
        }
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    public RSAPrivateKey privateKey() {
        return privateKey;
    }

    public String keyId() {
        return keyId;
    }

    private static String read(String path) {
        if (path == null || path.isBlank() || !Files.isReadable(Path.of(path))) {
            return null;
        }
        try {
            return Files.readString(Path.of(path), StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler a chave JWT em " + path, e);
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b != null && !b.isBlank() ? b : null;
    }

    private static byte[] decodePem(String pem) {
        String body = pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replace("\\n", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    private static java.security.PrivateKey parsePrivate(String pem) {
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decodePem(pem)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Chave privada JWT inválida (use PKCS#8)", e);
        }
    }

    private static java.security.PublicKey parsePublic(String pem) {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodePem(pem)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Chave pública JWT inválida", e);
        }
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
