import java.io.IOException;
import java.util.Arrays;

public class ServiceChatInput implements IPacketChatOutput{
    private ServiceChat client;
    private User user=null;
    private IChallenge challenge=null;
    private String username;

    public ServiceChatInput(ServiceChat client){
        this.client=client;
    }

    public User getUser(){
        return user;
    }


    private void logoutPacketHandler(PacketChat packet) throws IOException{
        User selectedUser;

        switch(packet.getCommand()){
            case PacketChat.AUTH:
                username=new String(packet.getField(0));
                selectedUser=ServerChatManager.getInstance().getDataBase().getUser(username);

                if (selectedUser==null){
                    challenge=new PasswordRegisterChallenge(username);
                }else{
                    challenge=selectedUser.getChallenge();
                }

                
                break;

            case PacketChat.CHALLENGE:
                byte[] response=packet.getField(0);

                if (challenge!=null && challenge.submit(response)){        
                    if (ServerChatManager.getInstance().isConnected(username)){
                        packetInterface.sendAuthFailure("logged in another location");
                        
                    }else{
                        selectedUser=ServerChatManager.getInstance().getDataBase().getUser(username);
                        if (Arrays.asList(getServer().getTags()).contains(selectedUser.getTag())){
                            this.user=selectedUser;
                            packetInterface.sendAuthSuccess();
                            ServerChatManager.getInstance().register(this);
                        }else{
                            packetInterface.sendAuthFailure("Unauthorized connection");
                        }
                    }
                }else{
                    packetInterface.sendAuthFailure("challenge failed");
                }
                //clear challenge so it cannot be used again
                challenge=null;
                username=null;

                break;
        }
    }

    private void loginPacketHandler(PacketChat packet) throws IOException{
        
    }

    public void putPacketChat(PacketChat packet) throws IOException {
        if (client.getUser()==null){
            logoutPacketHandler(packet);
        }else{
            loginPacketHandler(packet);
        }
    }
}
