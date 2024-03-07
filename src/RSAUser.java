import java.security.PublicKey;
import javax.crypto.Cipher;

public class RSAUser extends User{

    private Cipher cipher;

    public RSAUser(String name,byte[] key,String tag) throws Exception{
        super(name,key,tag);
        PublicKey publicKey=RSAEncoder.getInstance().publicDecode(key);
        cipher= Cipher.getInstance( "RSA/NONE/NoPadding", "BC" );
        cipher.init( Cipher.ENCRYPT_MODE,publicKey );
    }

    public RSAUser(String name,byte[] key) throws Exception{
        this(name, key,User.USER_TAG);
    }

    public String getTypeName() {
        return "RSA";
    }

    public Cipher getCipher(){
        return cipher;
    }

    public IChallenge getChallenge() {
        return new RSAChallenge(this);
    }
    
}
