package user;
import java.util.Arrays;

public class PasswordChallenge implements IChallenge{
    private PasswordUser user;

    public PasswordChallenge(PasswordUser user){
        this.user=user;
    }

    public byte[] get() {
        return null;
    }

    public boolean submit(byte[] response) {
        return Arrays.equals(user.getKey(), response);
    }
    
}
