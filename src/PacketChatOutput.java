import java.util.Collection;

public class PacketChatOutput {
    private IPacketChatOutput output;
    private String sender;

    public PacketChatOutput(IPacketChatOutput output,String sender){
        this.output=output;
        this.sender=sender;
    }

    public PacketChatOutput(IPacketChatOutput output){
        this(output,"");
    }

    public IPacketChatOutput getInterface(){
        return output;
    }

    public void setSender(String sender){
        this.sender=sender;
    }

    public void sendPacket(PacketChat packet) throws PacketChatException{
        output.putPacketChat(packet);
    }

    public void sendMessage(String message,String...dests) throws PacketChatException{
        sendPacket(PacketChatFactory.createMessagePacket(sender, message, dests));
    }

    public void sendFormattedMessage(String format,Object...args) throws PacketChatException{
        sendMessage(sender,String.format(format, args));
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

    public void sendFileInitRequest(byte nounce,String filename,String dest) throws PacketChatException{
        sendPacket(PacketChatFactory.createFileInitPacket(nounce, sender,filename, dest));
    }
}
