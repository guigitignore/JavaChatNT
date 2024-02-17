import java.util.StringTokenizer;

public class ServerChatPacketHandler {
    private void sendPacket(PacketChat packet,ServiceChat user){
        try{
            packet.send(user.getOutputStream());
        }catch(PacketChatException e){
            Logger.w("Packet cannot be sent to "+user.getUser().getName());
        }
    }

    public ServerChatPacketHandler(PacketChat packet){
        Logger.i("got packet "+packet);
        switch(packet.getCommand()){
            case PacketChat.SEND_MSG:
                String message=new String(packet.getField(1));
                String senderName=new String(packet.getField(0));
                ServiceChat sender=ServerChatManager.getInstance().getConnectedUser(senderName);

                switch (sender.getUser().getTag()){
                    case "ADMIN":
                        packet.replaceField(0, String.format("{%s}", senderName).getBytes());
                    default:
                        packet.replaceField(0, String.format("<%s>", senderName).getBytes());
                        break;
                }

                if (message.startsWith("/")){
                    StringTokenizer tokens=new StringTokenizer(message," ");
                    String command=tokens.nextToken().substring(1).toLowerCase();
                    String args=tokens.hasMoreTokens()?tokens.nextToken("").strip():"";
                    
                    switch (sender.getUser().getTag()){
                        case "ADMIN":
                            break;
                        default:
                            new BasicCommand(sender, command, args);
                            break;
                    }

                }else{
                    int fieldsNumber=packet.getFieldsNumber();

                    if (fieldsNumber==2){
                        for (ServiceChat client:ServerChatManager.getInstance().getConnectedUsers()){
                            sendPacket(packet, client);
                        }
                    }else{
                        for (int i=2;i<fieldsNumber;i++){
                            String username=new String(packet.getField(i));
                            ServiceChat user=ServerChatManager.getInstance().getConnectedUser(username);
                            if (user!=null){
                                sendPacket(packet, user);
                            }
                        }
                    }
                }
                break;
                
        }
    }
}
