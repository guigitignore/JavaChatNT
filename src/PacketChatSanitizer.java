import java.util.Arrays;

public class PacketChatSanitizer {
    private IUserConnection client;

    public PacketChatSanitizer(IUserConnection client){
        this.client=client;
    }

    private void sanitizePacketVAArgs(PacketChat packet,int minNumber) throws PacketChatException{
        if (packet.getFieldsNumber()<minNumber){
            throw new PacketChatException("expecting at least %d arguments",minNumber);
        }
    }

    private void sanitizePacketArgsNumber(PacketChat packet,int...argsNumber) throws PacketChatException{
        int packetFieldNumber=packet.getFieldsNumber();

        for (int argNumber:argsNumber){
            if (argNumber==packetFieldNumber) return;
        }
        throw new PacketChatException("Wrong arguments number: support only %s argument(s)",Arrays.toString(argsNumber));
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
                sanitizePacketVAArgs(packet, 2);
                packet.replaceField(0,client.getUser().getName().getBytes());
                break;
            case PacketChat.LIST_USERS:
                packet.clearFields();
                break;
            case PacketChat.FILE_INIT:
                sanitizePacketArgsNumber(packet, 0,3);
                if (packet.getFieldsNumber()>0) packet.replaceField(0,client.getUser().getName().getBytes());
                break;
            case PacketChat.FILE_DATA:
                sanitizePacketArgsNumber(packet, 3);
                if (packet.getFieldsNumber()>0) packet.replaceField(0,client.getUser().getName().getBytes());
                break;
            case PacketChat.FILE_ACK:
                packet.clearFields();
                break;
            case PacketChat.FILE_OVER:
                sanitizePacketArgsNumber(packet, 0,2);
                if (packet.getFieldsNumber()>0) packet.replaceField(0,client.getUser().getName().getBytes());
                break;
            default:
                throw new PacketChatException("Unknown packet type");
        }
    }

    public void clientLogoutSanitize(PacketChat packet) throws PacketChatException{
        switch(packet.getCommand()){
            case PacketChat.AUTH:
                packet.clearFields();
                break;
            case PacketChat.CHALLENGE:
                sanitizePacketArgsNumber(packet, 0,1);
                break;
            default:
                throw new PacketChatException("Unauthorized packet type");
        }
    }

    public void clientLoginSanitize(PacketChat packet) throws PacketChatException{
        switch(packet.getCommand()){
            case PacketChat.SEND_MSG:
                sanitizePacketVAArgs(packet, 2);
                break;
            case PacketChat.LIST_USERS:
                sanitizePacketVAArgs(packet, 1);
                break;
            case PacketChat.FILE_INIT:
                sanitizePacketArgsNumber(packet, 3);
                break;
            case PacketChat.FILE_DATA:
                sanitizePacketArgsNumber(packet, 3);
                break;
            case PacketChat.FILE_ACK:
                packet.clearFields();
                break;
            case PacketChat.FILE_OVER:
                sanitizePacketArgsNumber(packet, 2);
                break;
            default:
                throw new PacketChatException("Unauthorized packet type");
        }
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
