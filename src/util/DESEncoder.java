package util;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class DESEncoder {
    private static DESEncoder instance=null;
    private static byte[] keyBytes= { 0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0 };

    private SecretKeySpec key;
    private Cipher cipher;

    public static DESEncoder getInstance() throws Exception{
        if (instance==null) instance=new DESEncoder();
        return instance;
    }

    private DESEncoder() throws Exception{
        key=new SecretKeySpec(keyBytes, "DES");
        cipher = Cipher.getInstance("DES/ECB/PKCS7Padding", "BC");
    }

    public synchronized byte[] encode(byte[] data) throws Exception{
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public synchronized byte[] decode(byte[] data) throws Exception{
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }
}
