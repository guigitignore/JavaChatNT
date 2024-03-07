import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class KeyGenerator {
    public final static int KEY_SIZE=1024;

    public KeyGenerator(String username) throws Exception{
        KeyPair keyPair=generateRSAKeyPair();
        PublicKey publicKey=keyPair.getPublic();
        PrivateKey privateKey=keyPair.getPrivate();

        exportKey(String.format("%s.pub",username),publicKey);
        exportKey(String.format("%s.priv",username), privateKey);
    }

    private KeyPair generateRSAKeyPair() throws Exception {
        // Choose the RSA algorithm and key size
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(KEY_SIZE); 

        return keyPairGenerator.generateKeyPair();
    }

    public void exportKey(String filename,Key key) throws Exception{
        PrintStream writer=new PrintStream(new FileOutputStream(filename));
        byte[] keyBytes=RSAEncoder.getInstance().encode(key);
        String keyString=Base64.getEncoder().encodeToString(keyBytes);
        writer.println(keyString);
        writer.close();
    }
}
