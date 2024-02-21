import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamPacketChat implements IPacketChatOutput{
    private OutputStream output;

    public OutputStreamPacketChat(OutputStream output){
        this.output=output;
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException{
        try{
            synchronized(output){
                output.write(packet.getBytes());
            }
        }catch(IOException e){
            throw new PacketChatException(e.getMessage());
        }
        
    }
    
}
