import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class PacketChatInterface {
    private InputStream input;
    private OutputStream output;
    private Socket socket;

    public final static String DEFAULT_SENDER="[SERVER]";

    public PacketChatInterface(Socket socket) throws IOException{
        this.socket=socket;
        this.input=socket.getInputStream();
        this.output=socket.getOutputStream();
    }

    public PacketChat getPacket() throws IOException{
        PacketChat packet;

        try{
            packet=new PacketChat(input);
        }catch(PacketChatException e){
            throw new IOException(e.getMessage());
        }
        return packet;
    }

    public void sendPacket(PacketChat packet) throws IOException{
        try{
            packet.send(output);
        }catch(PacketChatException e){
            throw new IOException(e.getMessage());
        }
    }

    public void sendMessage(String message,String...dests) throws IOException{
        sendPacket(PacketChatFactory.createMessagePacket(DEFAULT_SENDER, message, dests));
    }

    public void sendFormattedMessage(String format,Object...args) throws IOException{
        sendMessage(String.format(format, args));
    }

    public void sendChallenge(byte[] challenge) throws IOException{
        sendPacket(PacketChatFactory.createChallengePacket(challenge));
    }

    public void sendPassword(String password) throws IOException{
        sendChallenge(password.getBytes());
    }

    public void sendUsername(String username) throws IOException{
        sendPacket(PacketChatFactory.createLoginPacket(username));
    }

    public void sendAuthSuccess() throws IOException{
        sendPacket(PacketChatFactory.createAuthPacket(true));
    }

    public void sendAuthFailure(String...messages) throws IOException{
        sendPacket(PacketChatFactory.createAuthPacket(false, messages));
    }

    public void sendListUserRequest() throws IOException{
        sendPacket(PacketChatFactory.createListUserPacket());
    }

    public void sendListUser(String...users) throws IOException{
        sendPacket(PacketChatFactory.createListUserPacket(users));
    }

    public void close() throws IOException{
        socket.close();
    }

}
