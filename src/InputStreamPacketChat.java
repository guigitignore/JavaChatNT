import java.io.InputStream;

public class InputStreamPacketChat implements IPacketChatInput {
    private InputStream input;

    public InputStreamPacketChat(InputStream input){
        this.input=input;
    }

    public PacketChat getPacketChat() throws PacketChatException{
            return new PacketChat(input);
    }
    
}
