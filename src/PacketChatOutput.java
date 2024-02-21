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

    public void sendMessage(String message,String...dests) throws PacketChatException{
        sendPacket(PacketChatFactory.createMessagePacket(DEFAULT_SENDER, message, dests));
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

    public void sendUsername(String username) throws PacketChatException{
        sendPacket(PacketChatFactory.createLoginPacket(username));
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

    public void sendListUser(String...users) throws PacketChatException{
        sendPacket(PacketChatFactory.createListUserPacket(users));
    }
}
