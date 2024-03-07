import java.io.IOException;
import java.net.Socket;

public class ClientChat extends SocketWorker{

    private PacketChatFileInterface fileInterface;
    private PacketChatTelnetInterface messageInterface;
    private PacketChatRawInterface upstreamInterface;

    private ClientChatMessageInput clientInputMessage=null;
    private ClientChatOutput clientOutput;

    public ClientChat(String host,int port) throws Exception{
        super(new Socket(host, port));
    }

    public String getDescription() {
        return String.format("ClientChat -> ServerChat %s", getSocket().getRemoteSocketAddress().toString());
    }

    public IPacketChatInterface getUpstreamInterface(){
        return upstreamInterface;
    }

    public IPacketChatInterface getMessageInterface(){
        return messageInterface;
    }

    public PacketChatFileInterface getFileInterface(){
        return fileInterface;
    }

    private void initStreams() throws IOException{
        upstreamInterface=new PacketChatRawInterface(getSocket());
        messageInterface=new PacketChatTelnetInterface();
        fileInterface=new PacketChatFileInterface();

        clientOutput=new ClientChatOutput(this);
        clientInputMessage=new ClientChatMessageInput(this);
    }

    private void mainloop() throws IOException{
        PacketChat packet;

        while (true){
            try{
                packet=upstreamInterface.getPacketChat();
            }catch(PacketChatException e){
                Logger.w("exit output loop: %s",e.getMessage());
                break;
            }
            try{
                clientOutput.putPacketChat(packet);
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

        if (clientInputMessage!=null) clientInputMessage.cancel();
        WorkerManager.getInstance().remove(this);
        cancel();
    }
    
}
