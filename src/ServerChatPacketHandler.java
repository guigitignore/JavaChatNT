public class ServerChatPacketHandler {
    private void sendPacket(PacketChat packet,ServiceChat user){
        try{
            packet.send(user.getOutputStream());
        }catch(PacketChatException e){
            Logger.w("Packet cannot be sent to "+user.getUserName());
        }
    }

    public ServerChatPacketHandler(PacketChat packet){
        Logger.i("got packet "+packet);
        switch(packet.getCommand()){
            case PacketChat.SEND_MSG:
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
    }
}
