import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class ClientChat extends SocketWorker implements IPacketChatInterface{

    private IPacketChatInterface packetInterface;

    private ClientChatMessage clientMessage=null;
    private ClientChatFile clientFile=null;

    public ClientChat(String host,int port) throws Exception{
        super(new Socket(host, port));
    }

    public String getDescription() {
        return String.format("ClientChat -> ServerChat %s", getSocket().getRemoteSocketAddress().toString());
    }

    public ClientChatFile getFile(){
        return clientFile;
    }

    private void skipWelcomeMessage() throws IOException{
        InputStream inputStream=getSocket().getInputStream();
        inputStream.skip(inputStream.available());
    }

    private void sendHelloPacket() throws IOException{
        try{
            PacketChatFactory.createHelloPacket().send(getSocket().getOutputStream());
        }catch(PacketChatException e){
            throw new IOException(e.getMessage());
        }
    }

    private void initStreams() throws IOException{
        skipWelcomeMessage();
        sendHelloPacket();

        packetInterface=new PacketChatRawInterface(getSocket());
        clientMessage=new ClientChatMessage(this);
        clientFile=new ClientChatFile(this);
    }

    private void mainloop() throws IOException{
        PacketChat packet;
        byte command;

        while (true){
            try{
                packet=packetInterface.getPacketChat();
                Logger.i("got packet: %s",packet.toString());
            }catch(PacketChatException e){
                Logger.w("exit output loop: %s",e.getMessage());
                break;
            }
            try{
                command=packet.getCommand();
                if (command==PacketChat.FILE_INIT || command==PacketChat.FILE_DATA || command==PacketChat.FILE_OVER){
                    clientFile.putPacketChat(packet);
                }else{
                    clientMessage.putPacketChat(packet);
                }
            }catch(PacketChatException e){
                Logger.w("Packet dropped");
            }
            
        }
    }

    public void run(){
        try{
            initStreams();
            mainloop();
        }catch(IOException e){
            Logger.e("IO error: %s",e.getMessage());
        }

        WorkerManager.getInstance().cancelAll();
        WorkerManager.getInstance().remove(this);
        
    }

    public PacketChat getPacketChat() throws PacketChatException {
        return packetInterface.getPacketChat();
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        packetInterface.putPacketChat(packet);
    }
    
}
