import java.util.Arrays;
import java.util.Collection;

public class PacketChatFactory{

    public static PacketChat createMessagePacket(String sender,String message,String... dests){
        PacketChat packet=new PacketChat();

        packet.setCommand(PacketChat.SEND_MSG);
        packet.addField(sender.getBytes());
        packet.addField(message.getBytes());

        for (String dest:dests){
            packet.addField(dest.getBytes());
        }
        return packet;
    }

    public static PacketChat createLoginPacket(String username){
        PacketChat packet=new PacketChat();

        packet.setCommand(PacketChat.AUTH);
        packet.addField(username.getBytes());

        return packet;
    }

    public static PacketChat createListUserPacket(String...users){
        return createListUserPacket(Arrays.asList(users));
    }

    public static PacketChat createListUserPacket(Collection<String> users){
        PacketChat packet=new PacketChat();

        packet.setCommand(PacketChat.LIST_USERS);
        for (String user:users){    
            packet.addField(user.getBytes());
        }
        return packet;
    }

    public static PacketChat createChallengePacket(byte[] challenge){
        PacketChat packet=new PacketChat();

        packet.setCommand(PacketChat.CHALLENGE);
        if (challenge!=null) packet.addField(challenge);
        return packet;
    }

    public static PacketChat createAuthPacket(boolean status,String... messages){
        PacketChat packet=new PacketChat();

        packet.setCommand(PacketChat.AUTH);
        if (status==true) packet.setStatus(PacketChat.STATUS_SUCCESS);
        else packet.setStatus(PacketChat.STATUS_ERROR);
        
        for (String message:messages){
            packet.addField(message.getBytes());
        }
        return packet;
    }
}