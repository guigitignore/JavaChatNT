public class PacketChatFactory{
    public final static byte AUTH =               (byte)0x00;
    public final static byte CHALLENGE =          (byte)0x01;
    public final static byte EXIT =               (byte)0x02;
    public final static byte SEND_MSG =           (byte)0x03;
    public final static byte LIST_USERS =         (byte)0x04;
    public final static byte FILE_INIT =          (byte)0x05;
    public final static byte FILE_DATA =          (byte)0x06;
    public final static byte FILE_OVER =          (byte)0x07;
    public final static byte HELLO =              (byte)0xFF;

    public final static byte STATUS_SUCCESS=      (byte)0x00;
    public final static byte STATUS_ERROR=        (byte)0x01;

    public static PacketChat createMessagePacket(String sender,String message,String... dests){
        PacketChat packet=new PacketChat();

        packet.setCommand(SEND_MSG);
        packet.addField(sender.getBytes());
        packet.addField(message.getBytes());

        for (String dest:dests){
            packet.addField(dest.getBytes());
        }
        return packet;
    }

    public static PacketChat createLoginPacket(String username){
        PacketChat packet=new PacketChat();

        packet.setCommand(AUTH);
        packet.addField(username.getBytes());

        return packet;
    }

    public static PacketChat createListUserPacket(){
        PacketChat packet=new PacketChat();

        packet.setCommand(LIST_USERS);
        return packet;
    }

    public static PacketChat createChallengePacket(byte[] challenge){
        PacketChat packet=new PacketChat();

        packet.setCommand(CHALLENGE);
        packet.addField(challenge);
        return packet;
    }

    public static PacketChat createAuthPacket(boolean status,String... messages){
        PacketChat packet=new PacketChat();

        packet.setCommand(AUTH);
        if (status==true) packet.setStatus(STATUS_SUCCESS);
        else packet.setStatus(STATUS_ERROR);
        
        for (String message:messages){
            packet.addField(message.getBytes());
        }
        return packet;
    }
}