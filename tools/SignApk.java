import com.android.apksig.ApkSigner;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

public class SignApk {
    public static void main(String[] args) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        char[] pw = args[4].toCharArray();
        try (FileInputStream fis = new FileInputStream(args[2])) {
            ks.load(fis, pw);
        }
        PrivateKey key = (PrivateKey) ks.getKey(args[3], pw);
        X509Certificate cert = (X509Certificate) ks.getCertificate(args[3]);
        ApkSigner.SignerConfig sc = new ApkSigner.SignerConfig.Builder("BILISAVE", key,
                Collections.singletonList(cert)).build();
        new ApkSigner.Builder(Collections.singletonList(sc))
                .setInputApk(new File(args[0]))
                .setOutputApk(new File(args[1]))
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setMinSdkVersion(21)
                .build()
                .sign();
        System.out.println("signed -> " + args[1]);
    }
}

