import java.io.IOException;
import java.io.InputStream;

public class InputStreamPacketChat implements IPacketChatInput {
    private InputStream input;

    public InputStreamPacketChat(InputStream input){
        this.input=input;
    }

    public PacketChat getPacketChat() throws IOException{
        try{
            return new PacketChat(input);
        }catch(PacketChatException e){
            throw new IOException(e.getMessage());
        }
        
    }
    
}
