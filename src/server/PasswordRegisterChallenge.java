package server;
import user.IChallenge;
import user.PasswordUser;
import user.User;
import user.UserDatabase;

public class PasswordRegisterChallenge implements IChallenge {
    private String username;
    private String tag;

    public PasswordRegisterChallenge(String username,String tag){
        this.username=username;
        this.tag=tag;
    }

    public PasswordRegisterChallenge(String username){
        this(username,User.USER_TAG);
    }

    public byte[] get() {
        return null;
    }

    public boolean submit(byte[] response) {
        UserDatabase db=ServerChatManager.getInstance().getDataBase();
        User tempUser=new PasswordUser(username, response,tag);
        return db.addUser(tempUser);
    }
    
}
