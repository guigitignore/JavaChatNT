public class PacketChatSanitizer {
    private IUserConnection client;

    public PacketChatSanitizer(IUserConnection client){
        this.client=client;
    }

    private void sanitizeMessagePacket(PacketChat packet) throws PacketChatException{
        if (packet.getFieldsNumber()<2){
            throw new PacketChatException("Insuficiant arguments number");
        }
    }

    private void serverLogoutSanitize(PacketChat packet) throws PacketChatException{
        int packetFieldNumber=packet.getFieldsNumber();

        switch(packet.getCommand()){
            case PacketChat.AUTH:
                if (packetFieldNumber==0){
                    throw new PacketChatException("missing mandatory field in auth packet");
                }
                break;
            case PacketChat.CHALLENGE:
                if (packetFieldNumber==0){
                    packet.addField(new byte[0]);
                }
                break;
            default:
                throw new PacketChatException("Unauthorized packet type");
        }
    }


    private void serverLoginSanitize(PacketChat packet) throws PacketChatException{
        switch(packet.getCommand()){
            case PacketChat.SEND_MSG:
                sanitizeMessagePacket(packet);
                packet.replaceField(0,client.getUser().getName().getBytes());
                break;
            case PacketChat.LIST_USERS:
                packet.clearFields();
                break;
            default:
                throw new PacketChatException("Unknown packet type");
        }
    }

    public void clientLogoutSanitize(PacketChat packet) throws PacketChatException{

    }

    public void clientLoginSanitize(PacketChat packet) throws PacketChatException{
        
    }

    public void server(PacketChat packet) throws PacketChatException{
        if (client.getUser()==null){
            serverLogoutSanitize(packet);
        }else{
            serverLoginSanitize(packet);
        }
    }

    public void client(PacketChat packet) throws PacketChatException{
        if (client.getUser()==null){
            clientLogoutSanitize(packet);
        }else{
            clientLoginSanitize(packet);
        }
    }
}
