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
        sendPacket(PacketChatFactory.createFileInitStatusPacket(nounce,true));
    }

    public void sendFileInitFailure(byte nounce) throws PacketChatException{
        sendPacket(PacketChatFactory.createFileInitStatusPacket(nounce,false));
    }

    public void sendFileInitRequest(byte nounce,String filename,String dest) throws PacketChatException{
        sendPacket(PacketChatFactory.createFileInitPacket(false,nounce, sender,filename.getBytes(), dest));
    }

    public void sendFileEncryptedInitRequest(byte nounce,byte[] filename,String dest) throws PacketChatException{
        sendPacket(PacketChatFactory.createFileInitPacket(true,nounce, sender,filename, dest));
    }

    public void sendFileDataRequest(byte nounce,byte[] data,String dest) throws PacketChatException{
        sendPacket(PacketChatFactory.createFileDataPacket(false,nounce, sender,data, dest));
    }

    public void sendFileEncryptedDataRequest(byte nounce,byte[] data,String dest) throws PacketChatException{
        sendPacket(PacketChatFactory.createFileDataPacket(true,nounce, sender,data, dest));
    }

    public void sendFileDataSuccess(byte nounce) throws PacketChatException{
        sendPacket(PacketChatFactory.createFileAckStatusPacket(nounce, true));
    }

    public void sendFileDataFailure(byte nounce) throws PacketChatException{
        sendPacket(PacketChatFactory.createFileAckStatusPacket(nounce, false));
    }

    public void sendFileOverRequest(byte nounce,String dest) throws PacketChatException{
        sendPacket(PacketChatFactory.createFileOverPacket(nounce,sender,dest));
    }

    public void sendFileOverSuccess(byte nounce) throws PacketChatException{
        sendPacket(PacketChatFactory.createFileOverStatusPacket(nounce,true));
    }

    public void sendFileOverFailure(byte nounce) throws PacketChatException{
        sendPacket(PacketChatFactory.createFileOverStatusPacket(nounce,false));
    }
}
