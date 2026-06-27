package au.com.example.webchat.server.service;

import au.com.example.webchat.server.model.RsaKey;
import au.com.example.webchat.server.repository.RsaKeyRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class RsaKeyService {

    private final RsaKeyRepository rsaKeyRepository;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private String publicKeyPem;

    @PostConstruct
    public void init() {
        try {
            RsaKey rsaKey = rsaKeyRepository.findFirstByOrderByCreatedAtDesc().orElse(null);
            if (rsaKey == null) {
                log.info("No RSA key found in database. Generating a new 2048-bit RSA key pair...");
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();
                
                publicKey = kp.getPublic();
                privateKey = kp.getPrivate();
                
                String pubBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
                String privBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
                
                rsaKey = RsaKey.builder()
                        .publicKey(pubBase64)
                        .privateKey(privBase64)
                        .createdAt(System.currentTimeMillis())
                        .build();
                rsaKeyRepository.save(rsaKey);
                log.info("New RSA key pair generated and saved to database.");
            } else {
                log.info("Loading existing RSA key pair from database.");
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                
                byte[] publicBytes = Base64.getDecoder().decode(rsaKey.getPublicKey());
                X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicBytes);
                publicKey = keyFactory.generatePublic(publicSpec);
                
                byte[] privateBytes = Base64.getDecoder().decode(rsaKey.getPrivateKey());
                PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateBytes);
                privateKey = keyFactory.generatePrivate(privateSpec);
            }
            
            // Format public key in PEM format for the client
            String pubEncoded = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(publicKey.getEncoded());
            publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" + pubEncoded + "\n-----END PUBLIC KEY-----";
            
        } catch (Exception e) {
            log.error("Failed to initialize RSA key pair", e);
            throw new RuntimeException("Failed to initialize RSA key pair", e);
        }
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public String decrypt(String encryptedDataBase64) {
        if (encryptedDataBase64 == null || encryptedDataBase64.isBlank()) {
            return encryptedDataBase64;
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedDataBase64.trim()));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt data: {}", encryptedDataBase64, e);
            throw new IllegalArgumentException("Invalid encrypted payload or decryption error", e);
        }
    }
}
