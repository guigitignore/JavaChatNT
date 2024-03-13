import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class ClientChat extends SocketWorker implements IPacketChatInterface,IUserConnection{
    public final static int BUCKET_CAPACITY=100;
    public final static String CLIENT_NAME="[ClientChat]";

    private IPacketChatInterface packetInterface;
    private IPacketChatInterface messageInterface;

    private PacketChatBucket bucket=null;

    private ClientChatInput clientInput;
    private ClientChatOutput clientOutput;

    private PacketChatOutput input;
    private PacketChatOutput output;

    private ClientChatRequest requestManager;

    public ClientChat(String host,int port) throws Exception{
        super(new Socket(host, port));
    }

    public String getDescription() {
        return String.format("ClientChat -> ServerChat %s", getSocket().getRemoteSocketAddress().toString());
    }

    public IPacketChatInterface getMessageInterface(){
        return messageInterface;
    }

    public PacketChat getPacketChat() throws PacketChatException {
        return packetInterface.getPacketChat();
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        packetInterface.putPacketChat(packet);
    }

    public User getUser() {
        return clientInput.isConnected()?clientOutput.getUser():null;
    }

    public PacketChatBucket getBucket(){
        return bucket;
    }

    public PacketChatOutput getInput(){
        return input;
    }

    public PacketChatOutput getOutput(){
        return output;
    }

    public ClientChatRequest getRequestManager(){
        return requestManager;
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
        messageInterface=new PacketChatTelnetInterface(new InterruptibleInputStream(),System.out);
        bucket=new PacketChatBucket(BUCKET_CAPACITY);

        clientInput=new ClientChatInput(this);
        clientOutput=new ClientChatOutput(this);

        input=new PacketChatOutput(clientInput);
        output=new PacketChatOutput(clientOutput);

        requestManager=new ClientChatRequest(this);
    }

    private void mainloop() throws IOException{
        PacketChat packet;
        PacketChatSanitizer sanitizer=new PacketChatSanitizer(this);

        while (true){
            try{
                packet=packetInterface.getPacketChat();
                Logger.i("got packet: %s",packet.toString());
            }catch(PacketChatException e){
                Logger.w("exit input loop: %s",e.getMessage());
                break;
            }
            try{
                sanitizer.client(packet);
                bucket.putPacketChat(packet);
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

    
    
}
