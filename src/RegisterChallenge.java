public class RegisterChallenge implements IChallenge {
    private String username;
    private User user=null;

    public RegisterChallenge(String username){
        this.username=username;
    }

    public byte[] get() {
        return null;
    }

    public boolean submit(byte[] response) {
        boolean status;

        Logger.i("submit");
        UserDatabase db=ServerChatManager.getInstance().getDataBase();
        User tempUser=new PasswordUser(username, response);
        status=db.addUser(tempUser);
        if (status) user=tempUser;

        return status;
    }

    public User getUser() {
        return user;
    }
    
}
