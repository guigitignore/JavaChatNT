import java.util.Random;

import javax.crypto.Cipher;

import java.util.Arrays;

public class RSAChallenge implements IChallenge{
    

    public final static int CHALLENGE_SIZE=128;

    private byte[] challengeResult=new byte[CHALLENGE_SIZE];
    private byte[] challenge;


    public RSAChallenge(RSAUser user){
        
        Random r = new Random();
        r.nextBytes(challengeResult);
        challengeResult[0]&=0x7F;
        Logger.i("challenge result: %s",Arrays.toString(challengeResult));
        
        try{
            Cipher cipher= Cipher.getInstance( "RSA/NONE/NoPadding", "BC" );
            cipher.init( Cipher.ENCRYPT_MODE,user.getPublicKey());
            challenge=cipher.doFinal(challengeResult);
        }catch(Exception e){
            Logger.w("failed to generate challenge: %s",e.getMessage());
            challenge=null;
        }
        
    }

    public byte[] get() {
        Logger.i("challenge get: %s",Arrays.toString(challenge));
        return challenge;
    }

    public boolean submit(byte[] response) {
        Logger.i("challenge submit: %s",Arrays.toString(response));
        boolean result;
        if (challenge==null){
            result=false;
        }else{
            result=Arrays.equals(response,challengeResult);
        }
        return result;
    }
    
}
