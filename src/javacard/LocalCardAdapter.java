package javacard;

import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import util.RSAKeyPair;

public class LocalCardAdapter implements IJavacardInterface {
    private static byte[] keyBytes= { 0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0 };

    private String username=null;
    private RSAKeyPair userKeyPair=null;
    private Cipher rsaCipher;
    private Cipher desCipher;
    private SecretKeySpec desKey;

    public LocalCardAdapter() throws Exception{
        rsaCipher=Cipher.getInstance( "RSA/NONE/NoPadding", "BC" );
        desCipher = Cipher.getInstance("DES/ECB/PKCS7Padding", "BC");
        desKey=new SecretKeySpec(keyBytes, "DES");
    }

    public void select(String username) throws Exception{
        userKeyPair=RSAKeyPair.importKeyPair("keys/"+username);
        this.username=username;
    }

    public String getSelectedUser() {
        return username;
    }

    public RSAPublicKey getPublicKey(){
        return userKeyPair.getPublic();
    }

    public synchronized byte[] solveChallenge(byte[] challenge) throws Exception {
        rsaCipher.init(Cipher.DECRYPT_MODE, userKeyPair.getPrivate());
        return rsaCipher.doFinal(challenge);
    }

    public synchronized byte[] encryptDES(byte[] data) throws Exception {
        desCipher.init(Cipher.ENCRYPT_MODE, desKey);
        return desCipher.doFinal(data);
    }


    public synchronized byte[] decryptDES(byte[] data) throws Exception {
        desCipher.init(Cipher.DECRYPT_MODE, desKey);
        return desCipher.doFinal(data);
    }

    public void clearUser() {
        username=null;
        userKeyPair=null;
    }

    public void close() throws Exception {}
    
}
