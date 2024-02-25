import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

//stateless class
public class PacketChatRawInterface implements IPacketChatInterface{

    private InputStream input;
    private OutputStream output;

    public PacketChatRawInterface(Socket socket) throws IOException{
        input=socket.getInputStream();
        output=socket.getOutputStream();
    }

    public PacketChat getPacketChat() throws PacketChatException{
        return new PacketChat(input);
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
