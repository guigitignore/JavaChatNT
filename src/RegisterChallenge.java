public class RegisterChallenge implements IChallenge {
    private String username;


    public RegisterChallenge(String username){
        this.username=username;
    }

    public byte[] get() {
        return null;
    }

    public boolean submit(byte[] response) {
        UserDatabase db=ServerChatManager.getInstance().getDataBase();
        User tempUser=new PasswordUser(username, response);
        return db.addUser(tempUser);
    }
    
}
