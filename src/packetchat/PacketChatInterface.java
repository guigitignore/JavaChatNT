package packetchat;
//wrapper class

public class PacketChatInterface implements IPacketChatInterface{
    IPacketChatInput input;
    IPacketChatOutput output;

    public PacketChatInterface(IPacketChatInput input,IPacketChatOutput output){
        this.input=input;
        this.output=output;
    }

    public PacketChat getPacketChat() throws PacketChatException {
        return input.getPacketChat();
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        output.putPacketChat(packet);
    }
    
}
