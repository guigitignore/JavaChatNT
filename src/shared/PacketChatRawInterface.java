package shared;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import packetchat.IPacketChatInterface;
import packetchat.PacketChat;
import packetchat.PacketChatException;

//stateless class
public class PacketChatRawInterface implements IPacketChatInterface{

    private InputStream input;
    private OutputStream output;

    public PacketChatRawInterface(InputStream input,OutputStream output){
        this.input=input;
        this.output=output;
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
