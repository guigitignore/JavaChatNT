public class PasswordUser extends User {


    public PasswordUser(String name,byte[] key,String tag){
        super(name,key,tag);
    }

    public PasswordUser(String name,byte[] key){
        super(name,key);
    }

    public PasswordUser(String name,String password,String tag){
        this(name,password.getBytes(),tag);
    }

    public PasswordUser(String name,String password){
        this(name,password.getBytes());
    }
    
    public String getTypeName() {
        return "PASSWORD";
    }

    public IChallenge getChallenge() {
        return new PasswordChallenge(this);
    }
    
}
