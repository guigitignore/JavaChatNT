public class RSAChallenge implements IChallenge{
    private RSAUser user;

    public RSAChallenge(RSAUser user){
        this.user=user;
    }

    public byte[] get() {
        return null;
    }

    public boolean submit(byte[] response) {
        return true;
    }

    public User getUser() {
        return user;
    }
    
}
