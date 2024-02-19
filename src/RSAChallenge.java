import java.util.Random;
import java.util.Arrays;

public class RSAChallenge implements IChallenge{
    

    public final static int CHALLENGE_SIZE=128;

    private byte[] challengeResult=new byte[CHALLENGE_SIZE];
    private byte[] challenge;


    public RSAChallenge(RSAUser user){
        
        Random r = new Random();
        r.nextBytes(challengeResult);
        try{
            challenge=user.getCipher().doFinal(challengeResult);
        }catch(Exception e){
            challenge=null;
        }
        
    }

    public byte[] get() {
        return challenge;
    }

    public boolean submit(byte[] response) {
        boolean result;
        if (challenge==null){
            result=false;
        }else{
            result=Arrays.equals(challenge,challengeResult);
        }
        return result;
    }
    
}
