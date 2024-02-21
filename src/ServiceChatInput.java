import java.util.Arrays;

public class ServiceChatInput implements IPacketChatOutput{
    private ServiceChat client;
    private User user=null;
    private IChallenge challenge=null;
    private String username;
    private PacketChatSanitizer sanitizer;

    public ServiceChatInput(ServiceChat client){
        this.client=client;
        sanitizer=new PacketChatSanitizer(client);
    }

    public User getUser(){
        return user;
    }


    private void logoutPacketHandler(PacketChat packet) throws PacketChatException{
        User selectedUser;

        switch(packet.getCommand()){
            case PacketChat.AUTH:
                username=new String(packet.getField(0));
                selectedUser=ServerChatManager.getInstance().getDataBase().getUser(username);

                if (selectedUser==null){
                    if (packet.getFieldsNumber()==2){
                        challenge=new RSARegisterChallenge(username,packet.getField(1));
                    }else{
                        challenge=new PasswordRegisterChallenge(username);
                    }
                }else{
                    challenge=selectedUser.getChallenge();
                }

                
                break;

            case PacketChat.CHALLENGE:
                byte[] response=packet.getField(0);

                if (challenge!=null && challenge.submit(response)){        
                    if (ServerChatManager.getInstance().isConnected(username)){
                        client.getOutput().sendAuthFailure("logged in another location");
                    }else{
                        selectedUser=ServerChatManager.getInstance().getDataBase().getUser(username);
                        if (Arrays.asList(client.getServer().getTags()).contains(selectedUser.getTag())){
                            this.user=selectedUser;
                            client.getOutput().sendAuthSuccess();
                            ServerChatManager.getInstance().register(client);
                        }else{
                            client.getOutput().sendAuthFailure("Unauthorized connection");
                        }
                    }
                }else{
                    client.getOutput().sendAuthFailure("challenge failed");
                }
                //clear challenge so it cannot be used again
                challenge=null;
                username=null;

                break;
        }
    }

    private void loginPacketHandler(PacketChat packet) throws PacketChatException{
        
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        if (client.getUser()==null){
            sanitizer.logoutSanitize(packet);
            logoutPacketHandler(packet);
        }else{
            loginPacketHandler(packet);
        }
    }
}
