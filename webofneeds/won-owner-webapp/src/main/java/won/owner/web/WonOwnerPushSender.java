package won.owner.web;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import won.owner.model.User;

import javax.annotation.PostConstruct;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.ExecutionException;

@Component
@PropertySource(value = "file:${WON_CONFIG_DIR}/owner.properties", ignoreResourceNotFound = true)
public class WonOwnerPushSender {
    private final String VAPID_KEY_ALIAS = "vapidPushKey";
    @Value("${vapid.privateKeyLocation}")
    private String privateKeyLocation;
    @Value("${vapid.publicKeyLocation}")
    private String publicKeyLocation;
    private PushService pushService;

    @PostConstruct
    public void init() {
        KeyPair serverKey;
        try {
            serverKey = loadKeys();
        } catch (InvalidKeySpecException | IOException e) {
            try {
                serverKey = generateKey();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        pushService = new PushService(serverKey, "");
    }

    public PublicKey getPublicKey() {
        return pushService.getPublicKey();
    }

    public void sendNotification(User user, String payload) {
        user.getPushSubscriptions().forEach(subscription -> {
            try {
                sendNotification(new Notification(subscription.toSubscription(), payload));
            } catch (InvalidKeySpecException | NoSuchProviderException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void sendNotification(Notification notification) {
        try {
            pushService.send(notification);
        } catch (GeneralSecurityException | JoseException | ExecutionException | InterruptedException | IOException e) {
            // TODO we need at least proper handling for denied subscriptions
            throw new RuntimeException(e);
        }
    }

    private PublicKey loadPublicKey() throws IOException, InvalidKeySpecException {
        try (FileInputStream in = new FileInputStream(publicKeyLocation)) {
            File fin = new File(publicKeyLocation);
            byte[] encodedKey = new byte[(int) fin.length()];
            in.read(encodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("ECDH");
            X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedKey);
            return keyFactory.generatePublic(spec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private PrivateKey loadPrivateKey() throws IOException, InvalidKeySpecException {
        try (FileInputStream in = new FileInputStream(publicKeyLocation)) {
            File fin = new File(publicKeyLocation);
            byte[] encodedKey = new byte[(int) fin.length()];
            in.read(encodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("ECDH");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encodedKey);
            return keyFactory.generatePrivate(spec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private KeyPair loadKeys() throws IOException, InvalidKeySpecException {
        return new KeyPair(loadPublicKey(), loadPrivateKey());
    }

    private KeyPair generateKey() throws IOException {
        try {
            ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "BC");
            keyPairGenerator.initialize(parameterSpec);
            KeyPair serverKey = keyPairGenerator.generateKeyPair();
            PrivateKey priv = serverKey.getPrivate();
            PublicKey pub = serverKey.getPublic();
            X509EncodedKeySpec publicKeyEncode = new X509EncodedKeySpec(pub.getEncoded());
            try (FileOutputStream out = new FileOutputStream(publicKeyLocation)) {
                out.write(publicKeyEncode.getEncoded());
            }
            PKCS8EncodedKeySpec privateKeyEncode = new PKCS8EncodedKeySpec(priv.getEncoded());
            try (FileOutputStream out = new FileOutputStream(privateKeyLocation)) {
                out.write(privateKeyEncode.getEncoded());
            }
            return serverKey;
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }
}
