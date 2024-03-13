import java.util.Collection;

public class PacketChatOutput {
    public final static String DEFAULT_SENDER="[SERVER]";
    
    private IPacketChatOutput output;

    public PacketChatOutput(IPacketChatOutput output){
        this.output=output;
    }

    public IPacketChatOutput getInterface(){
        return output;
    }

    public void sendPacket(PacketChat packet) throws PacketChatException{
        output.putPacketChat(packet);
    }

    public void sendMessage(String sender,String message,String...dests) throws PacketChatException{
        sendPacket(PacketChatFactory.createMessagePacket(sender, message, dests));
    }

    public void sendMessage(String message,String...dests) throws PacketChatException{
        sendMessage(DEFAULT_SENDER,message,dests);
    }

    public void sendFormattedMessage(String format,Object...args) throws PacketChatException{
        sendMessage(String.format(format, args));
    }

    public void sendChallenge(byte[] challenge) throws PacketChatException{
        sendPacket(PacketChatFactory.createChallengePacket(challenge));
    }

    public void sendPassword(String password) throws PacketChatException{
        sendChallenge(password.getBytes());
    }

    public void sendUsername(String username,byte[]...params) throws PacketChatException{
        sendPacket(PacketChatFactory.createLoginPacket(username,params));
    }

    public void sendAuthSuccess() throws PacketChatException{
        sendPacket(PacketChatFactory.createAuthPacket(true));
    }

    public void sendAuthFailure(String...messages) throws PacketChatException{
        sendPacket(PacketChatFactory.createAuthPacket(false, messages));
    }

    public void sendListUserRequest() throws PacketChatException{
        sendPacket(PacketChatFactory.createListUserPacket());
    }

    public void sendListUser(Collection<String> users) throws PacketChatException{
        sendPacket(PacketChatFactory.createListUserPacket(users));
    }

    public void sendFileInitSucess(byte nounce) throws PacketChatException{
        sendPacket(PacketChatFactory.createFileInitStatus(nounce,true));
    }

    public void sendFileInitFailure(byte nounce) throws PacketChatException{
        sendPacket(PacketChatFactory.createFileInitStatus(nounce,false));
    }
}
