import java.security.interfaces.RSAPublicKey;

public class RSAUser extends User{

    private RSAPublicKey publicKey;

    public RSAUser(String name,byte[] key,String tag) throws Exception{
        super(name,key,tag);
        publicKey=(RSAPublicKey)RSAEncoder.getInstance().publicDecode(key);
    }

    public RSAUser(String name,byte[] key) throws Exception{
        this(name, key,User.USER_TAG);
    }

    public String getTypeName() {
        return "RSA";
    }

    public RSAPublicKey getPublicKey(){
        return publicKey;
    }

    public IChallenge getChallenge() {
        return new RSAChallenge(this);
    }
    
}
