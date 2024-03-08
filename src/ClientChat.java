import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class ClientChat extends SocketWorker{

    private PacketChatTelnetInterface messageInterface;
    private PacketChatRawInterface upstreamInterface;

    private ClientChatInput clientInput=null;
    private ClientChatOutput clientOutput=null;
    private ClientChatFile clientFile=null;

    public ClientChat(String host,int port) throws Exception{
        super(new Socket(host, port));
    }

    public String getDescription() {
        return String.format("ClientChat -> ServerChat %s", getSocket().getRemoteSocketAddress().toString());
    }

    public IPacketChatOutput getInput(){
        return clientInput;
    }

    public IPacketChatOutput getOutput(){
        return clientOutput;
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

        upstreamInterface=new PacketChatRawInterface(getSocket());
        messageInterface=new PacketChatTelnetInterface();

        clientInput=new ClientChatInput(this,messageInterface);
        clientOutput=new ClientChatOutput(this,new PacketChatInterface(messageInterface, upstreamInterface));
        clientFile=new ClientChatFile(this);
    }

    private void mainloop() throws IOException{
        PacketChat packet;

        while (true){
            try{
                packet=upstreamInterface.getPacketChat();
                
                Logger.i("got packet: %s",packet.toString());
            }catch(PacketChatException e){
                Logger.w("exit output loop: %s",e.getMessage());
                break;
            }
            try{
                clientInput.putPacketChat(packet);
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
    
}
