package util;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public class RSAKeyPair {
    public final static int KEY_SIZE=1024;
    private KeyPair keyPair;

    public static RSAKeyPair importKeyPair(String username) throws Exception{
        PublicKey pub=RSAEncoder.getInstance().publicDecode(importKey(String.format("%s.pub", username)));
        PrivateKey priv=RSAEncoder.getInstance().privateDecode(importKey(String.format("%s.priv", username)));
        return new RSAKeyPair(new KeyPair(pub, priv));
    }

    private RSAKeyPair(KeyPair keyPair){
        this.keyPair=keyPair;
    }

    public RSAKeyPair() throws Exception{
        this(generateRSAKeyPair());
    }

    public RSAPublicKey getPublic(){
        return (RSAPublicKey)keyPair.getPublic();
    }

    public RSAPrivateKey getPrivate(){
        return (RSAPrivateKey)keyPair.getPrivate();
    }

    public void exportKeyPair(String username) throws Exception{
        exportKey(String.format("%s.pub",username),keyPair.getPublic());
        exportKey(String.format("%s.priv",username), keyPair.getPrivate());
    }

    private static KeyPair generateRSAKeyPair() throws Exception {
        // Choose the RSA algorithm and key size
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(KEY_SIZE); 

        return keyPairGenerator.generateKeyPair();
    }

    private static byte[] importKey(String filename) throws Exception{
        BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
        String data=reader.readLine();
        reader.close();
        return Base64.getDecoder().decode(data);
    }

    private static String getKeyString(Key key) throws Exception{
        byte[] keyBytes=RSAEncoder.getInstance().encode(key);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    private static void exportKey(String filename,Key key) throws Exception{
        PrintStream writer=new PrintStream(new FileOutputStream(filename));
        writer.println(getKeyString(key));
        writer.close();
    }
}
