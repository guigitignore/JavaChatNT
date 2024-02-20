import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamPacketChat implements IPacketChatOutput{
    private OutputStream output;

    public OutputStreamPacketChat(OutputStream output){
        this.output=output;
    }

    public void putPacketChat(PacketChat packet) throws IOException {
        synchronized(output){
            output.write(packet.getBytes());
        }
    }
    
}
