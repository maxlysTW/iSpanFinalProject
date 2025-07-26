package journey.utils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * retrieve Private and Public key from a keypair in the keystore
 * 
 * @author 乃文
 * @version 1.0
 * @since 07/03/2025
 */
@Component
public class LoadKey {
    @Value("${key.store.path}")
    private String keyStorePath;

    public KeyStore getKeyStore(String keyStoreName, String storePass) {
        if (keyStoreName != null && storePass != null) {
            Path storePath = Paths.get(keyStorePath, keyStoreName);
            if (!Files.exists(storePath)) {
                System.out.println("KeyStore Not Exist!: " + storePath);
                return null;
            }
            try (InputStream is = Files.newInputStream(storePath)) {
                KeyStore keyStore = KeyStore.getInstance("JKS");
                keyStore.load(is, storePass.toCharArray());
                return keyStore;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("testABC");
        return null;
    }

    public PrivateKey getPrivateKey(String keyStoreName, String storePass, String alias) {
        if (keyStoreName != null && storePass != null) {
            KeyStore keyStore = getKeyStore(keyStoreName, storePass);
            if (keyStore != null) {
                try {
                    return (PrivateKey) keyStore.getKey(alias, storePass.toCharArray());
                } catch (UnrecoverableKeyException e) {
                    e.printStackTrace();
                } catch (KeyStoreException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public PublicKey getPublicKey(String keyStoreName, String storePass, String alias) {
        if (keyStoreName != null && storePass != null) {
            KeyStore keyStore = getKeyStore(keyStoreName, storePass);
            if (keyStore != null) {
                try {
                    return keyStore.getCertificate(alias).getPublicKey();
                } catch (KeyStoreException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
