import java.util.Random;

public class RSARegisterChallenge implements IChallenge{
    private RSAUser user=null;
    private IChallenge challenge=null;


    public RSARegisterChallenge(String username,byte[] publicKey,String tag){
        try{
            user=new RSAUser(username, publicKey,tag);
            challenge=user.getChallenge();
        }catch(Exception e){
            Logger.w("Invalid register public key");
        }
    }

    public RSARegisterChallenge(String username,byte[] publicKey){
        this(username,publicKey,User.USER_TAG);
    }

    public byte[] get() {
        byte[] challengeBytes;

        if (challenge==null){
            //generate a fake challenge to not disclose existing user
            challengeBytes=new byte[RSAChallenge.CHALLENGE_SIZE];
            new Random().nextBytes(challengeBytes);
            challengeBytes[0]&=0x7F;
        }else{
            challengeBytes=challenge.get();
        }
        return challengeBytes;
    }

    public boolean submit(byte[] response) {
        boolean status=false;

        if (challenge!=null && challenge.submit(response)){
            UserDatabase db=ServerChatManager.getInstance().getDataBase();
            status=db.addUser(user);
        }

        return status;
    }
    
}
