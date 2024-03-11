import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.stream.IntStream;

public class ServiceChatInput implements IPacketChatOutput{
    private ServiceChat client;
    private User user=null;
    private String loginUsername=null;
    private IChallenge challenge=null;
    private PacketChatSanitizer sanitizer;

    public ServiceChatInput(ServiceChat client){
        this.client=client;
        sanitizer=new PacketChatSanitizer(client);
    }

    public User getUser(){
        return user;
    }

    private void handleAuthPacket(PacketChat packet) throws PacketChatException{
        loginUsername=new String(packet.getField(0));
        User selectedUser=ServerChatManager.getInstance().getDataBase().getUser(loginUsername);

        if (selectedUser==null){
            if (packet.getFieldsNumber()==2){
                challenge=new RSARegisterChallenge(loginUsername,packet.getField(1),client.getServer().getTags()[0]);
            }else{
                challenge=new PasswordRegisterChallenge(loginUsername,client.getServer().getTags()[0]);
            }
        }else{
            challenge=selectedUser.getChallenge();
        }
        client.getOutput().sendChallenge(challenge.get());
    }

    private void handleChallengePacket(PacketChat packet) throws PacketChatException{
        User selectedUser;
        byte[] response=packet.getField(0);

        if (challenge!=null && challenge.submit(response)){        
            if (ServerChatManager.getInstance().isConnected(loginUsername)){
                client.getOutput().sendAuthFailure("logged in another location");
            }else{
                selectedUser=ServerChatManager.getInstance().getDataBase().getUser(loginUsername);
                if (Arrays.asList(client.getServer().getTags()).contains(selectedUser.getTag())){
                    //set connected user
                    this.user=selectedUser;
                    client.getOutput().sendAuthSuccess();
                    ServerChatManager.getInstance().register(client);
                    //send list of connected users
                    client.getOutput().sendListUser(ServerChatManager.getInstance().getUsers());
                }else{
                    client.getOutput().sendAuthFailure("Unauthorized connection");
                }
            }
        }else{
            client.getOutput().sendAuthFailure("challenge failed");
        }
        //clear challenge so it cannot be used again
        challenge=null;
    }

    private void handleMessagePacket(PacketChat packet) throws PacketChatException{
        String senderName=new String(packet.getField(0));
        String message=new String(packet.getField(1));

        switch (client.getUser().getTag()){
            case User.ADMIN_TAG:
                senderName=String.format("{%s}", senderName);
                break;
            case User.USER_TAG:
                senderName=String.format("<%s>", senderName);
                break;
            default:
                senderName="%unknown%";
                break;
        }
        packet.replaceField(0, senderName.getBytes());

        if (message.startsWith("/")){
            StringTokenizer tokens=new StringTokenizer(message," ");
            String command=tokens.nextToken().substring(1).toLowerCase();
            String args=tokens.hasMoreTokens()?tokens.nextToken("").strip():"";
            
            switch (client.getUser().getTag()){
                case User.ADMIN_TAG:
                    new AdminCommand(client, command, args);
                    break;
                case User.USER_TAG:
                    new UserCommand(client, command, args);
                    break;
                default:
                    client.getOutput().sendMessage("You are not allowed to execute server commands");
                    break;
            }

        }else{
            int fieldsNumber=packet.getFieldsNumber();

            if (fieldsNumber==2){
                ServerChatManager.getInstance().getClients().getOutput().sendPacket(packet);
            }else{
                ServerChatManager.getInstance().getClientsByName(IntStream.range(2, fieldsNumber).mapToObj(index -> {
                    return new String(packet.getField(index));
                }).toList()).getOutput().sendPacket(packet);
            }
        }
    }

    private void handleListUserPacket(PacketChat packet) throws PacketChatException{
        client.getOutput().sendListUser(ServerChatManager.getInstance().getUsers());
    }


    public void putPacketChat(PacketChat packet) throws PacketChatException {
        Logger.i("got packet: %s",packet);
        sanitizer.server(packet);

        switch(packet.getCommand()){
            case PacketChat.AUTH:
                handleAuthPacket(packet);
                break;
            case PacketChat.CHALLENGE:
                handleChallengePacket(packet);
                break;
            case PacketChat.SEND_MSG:
                handleMessagePacket(packet);
                break;
            case PacketChat.LIST_USERS:
                handleListUserPacket(packet);
                break;
            default:
                Logger.w("Cannot handle this packet: %s",packet);
                break;
        }
    }
}
