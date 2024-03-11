import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class ClientChat extends SocketWorker implements IPacketChatInterface,IUserConnection{

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

        packetInterface=new PacketChatRawInterface(getSocket().getInputStream(),getSocket().getOutputStream());
        clientMessage=new ClientChatMessage(this);
        clientFile=new ClientChatFile(this);
    }

    private void mainloop() throws IOException{
        PacketChat packet;
        byte command;
        PacketChatSanitizer sanitizer=new PacketChatSanitizer(this);

        while (true){
            try{
                packet=packetInterface.getPacketChat();
                Logger.i("got packet: %s",packet.toString());
            }catch(PacketChatException e){
                Logger.w("exit output loop: %s",e.getMessage());
                break;
            }
            try{
                sanitizer.client(packet);
                command=packet.getCommand();
                if (command==PacketChat.FILE_INIT || command==PacketChat.FILE_DATA || command==PacketChat.FILE_OVER){
                    clientFile.putPacketChat(packet);
                }else{
                    clientMessage.putPacketChat(packet);
                }
            }catch(PacketChatException e){
                Logger.w("Packet dropped: %s",e.getMessage());
            }
            
        }
    }

    public void run(){
        Logger.removeSTDOUT();

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

    public User getUser() {
        return clientMessage.getUser();
    }
    
}
