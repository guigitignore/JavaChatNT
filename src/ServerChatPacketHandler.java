import java.io.IOException;
import java.util.StringTokenizer;

public class ServerChatPacketHandler {

    private void sendPacket(ServiceChat user,PacketChat packet){
        if (user!=null){
            try{
                user.getPacketInterface().sendPacket(packet);
            }catch(IOException e){
                Logger.w("Failed to send packet to "+user.getUser().getName());
            }
        }
    }

    private void messagePacketHandler(PacketChat packet){
        String message=new String(packet.getField(1));
        String senderName=new String(packet.getField(0));
        ServiceChat sender=ServerChatManager.getInstance().getConnectedUser(senderName);

        switch (sender.getUser().getTag()){
            case User.ADMIN_TAG:
                packet.replaceField(0, String.format("{%s}", senderName).getBytes());
                break;
            case User.USER_TAG:
                packet.replaceField(0, String.format("<%s>", senderName).getBytes());
                break;
            default:
                packet.replaceField(0, "%unknown%".getBytes());
                break;
        }

        if (message.startsWith("/")){
            StringTokenizer tokens=new StringTokenizer(message," ");
            String command=tokens.nextToken().substring(1).toLowerCase();
            String args=tokens.hasMoreTokens()?tokens.nextToken("").strip():"";
            
            try{
                switch (sender.getUser().getTag()){
                    case User.ADMIN_TAG:
                        new AdminCommand(sender, command, args);
                        break;
                    case User.USER_TAG:
                        new UserCommand(sender, command, args);
                        break;
                    default:
                        sender.getPacketInterface().sendMessage("You are not allowed to execute server commands");
                        break;
                }
            }catch(IOException e){
                Logger.w("IOException during command %s executed by %s", command,sender.getUser().getName());
            }
            

        }else{
            int fieldsNumber=packet.getFieldsNumber();

            if (fieldsNumber==2){
                for (ServiceChat client:ServerChatManager.getInstance().getUsers()){
                    sendPacket(client, packet);
                }
            }else{
                for (int i=2;i<fieldsNumber;i++){
                    String username=new String(packet.getField(i));
                    ServiceChat user=ServerChatManager.getInstance().getConnectedUser(username);
                    sendPacket(user, packet);
                }
            }
        }
    }

    private void listUsersPacketHandler(PacketChat packet){
        String username=new String(packet.getField(0));
        ServiceChat user=ServerChatManager.getInstance().getConnectedUser(username);

        if (user!=null){
            try{
                user.getPacketInterface().sendPacket(ServerChatManager.getInstance().getListUsersPacket());
            }catch(IOException e){
                Logger.w("failed to send list user to %s",username);
            }
        }
        
    }

    public ServerChatPacketHandler(PacketChat packet){
        Logger.i("got packet "+packet);
        switch(packet.getCommand()){
            case PacketChat.SEND_MSG:
                messagePacketHandler(packet);
                break;
            case PacketChat.LIST_USERS:
                listUsersPacketHandler(packet);
                break;
                
        }
    }
}
