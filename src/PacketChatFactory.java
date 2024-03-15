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

    public static PacketChat createLoginPacket(String username,byte[]...params){
        PacketChat packet=new PacketChat();

        packet.setCommand(PacketChat.AUTH);
        packet.addField(username.getBytes());
        for (byte[] param:params){
            packet.addField(param);
        }
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

    public static PacketChat createHelloPacket(){
        PacketChat packet=new PacketChat();
        packet.setCommand(PacketChat.HELLO);
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

    public static PacketChat createFileInitPacket(boolean encrypted,byte nounce,String sender,byte[] filename,String dest){
        PacketChat packet=new PacketChat();

        packet.setCommand(PacketChat.FILE_INIT);
        if (encrypted) packet.setFlag(PacketChat.ENCRYPTION_FLAG);
        packet.addField(sender.getBytes());
        packet.addField(filename);
        packet.addField(new byte[]{(byte)0x0,(byte)0x2,(byte)0x3,(byte)0x4});
        packet.addField(dest.getBytes());
        return packet;
    }

    public static PacketChat createFileDataPacket(boolean encrypted,byte nounce,String sender,byte[] data,String dest){
        PacketChat packet=new PacketChat();

        packet.setCommand(PacketChat.FILE_DATA);
        if (encrypted) packet.setFlag(PacketChat.ENCRYPTION_FLAG);
        packet.addField(sender.getBytes());
        packet.addField(data);
        packet.addField(dest.getBytes());
        return packet;
    }

    public static PacketChat createFileOverPacket(byte nounce,String sender,String dest){
        PacketChat packet=new PacketChat();

        packet.setCommand(PacketChat.FILE_OVER);
        packet.addField(sender.getBytes());
        packet.addField(dest.getBytes());
        return packet;
    }

    private static PacketChat createFileStatusPacket(byte command,byte nounce,boolean status){
        PacketChat packet=new PacketChat();
        packet.setCommand(command);
        if (status) packet.setStatus(PacketChat.STATUS_SUCCESS);
        else packet.setStatus(PacketChat.STATUS_ERROR);
        return packet;
    }

    public static PacketChat createFileInitStatusPacket(byte nounce,boolean status){
        return createFileStatusPacket(PacketChat.FILE_INIT, nounce, status);
    }

    public static PacketChat createFileOverStatusPacket(byte nounce,boolean status){
        return createFileStatusPacket(PacketChat.FILE_OVER, nounce, status);
    }

    public static PacketChat createFileAckStatusPacket(byte nounce,boolean status){
        return createFileStatusPacket(PacketChat.FILE_ACK ,nounce, status);
    }


}