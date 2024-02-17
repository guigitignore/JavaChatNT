public class RSAUser extends User{

    public RSAUser(String name,byte[] key,String tag){
        super(name,key,tag);
    }

    public String getTypeName() {
        return "RSA";
    }

    public IChallenge getChallenge() {
        return new RSAChallenge(this);
    }
    
}
