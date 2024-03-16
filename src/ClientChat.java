import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientChat extends SocketWorker implements IPacketChatInterface,IUserConnection{
    public final static int BUCKET_CAPACITY=100;
    public final static String CLIENT_NAME="[ClientChat]";

    private IPacketChatInterface packetInterface;
    private IPacketChatInterface messageInterface;

    private PacketChatBucket bucket=null;

    private ClientChatInput clientInput;
    private ClientChatOutput clientOutput;

    private ClientChatRequest requestManager;

    private NounceManager incomingFiles;
    private NounceManager outgoingFiles;

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
        Logger.i("send packet: %s",packet.toString());
    }

    public User getUser() {
        return clientInput.isConnected()?clientOutput.getUser():null;
    }

    public PacketChatBucket getBucket(){
        return bucket;
    }

    public ClientChatInput getInput(){
        return clientInput;
    }

    public ClientChatOutput getOutput(){
        return clientOutput;
    }

    public ClientChatRequest getRequestManager(){
        return requestManager;
    }

    public NounceManager getIncomingFiles(){
        return incomingFiles;
    }

    public NounceManager getOutgoingFiles(){
        return outgoingFiles;
    }

    private void skipWelcomeMessage(InputStream in) throws IOException{
        int available;
        do{
            available=in.available();
            try{
                Thread.sleep(10);
            }catch(InterruptedException e){
                throw new IOException(e.getMessage());
            }
        }while(available==0);
        in.skip(available);
    }

    private void sendHelloPacket(OutputStream out) throws IOException{
        try{
            PacketChatFactory.createHelloPacket().send(out);
        }catch(PacketChatException e){
            throw new IOException(e.getMessage());
        }
    }

    private void initStreams() throws IOException{
        InputStream stdin;
        InputStream in=getSocket().getInputStream();
        OutputStream out=getSocket().getOutputStream();

        skipWelcomeMessage(in);
        sendHelloPacket(out);

        packetInterface=new PacketChatRawInterface(in,out);

        stdin=System.getProperty("java.version").startsWith("1.")?System.in:new InterruptibleInputStream();
        messageInterface=new PacketChatTelnetInterface(stdin,System.out);
        bucket=new PacketChatBucket(BUCKET_CAPACITY);
        incomingFiles=new NounceManager();
        outgoingFiles=new NounceManager();

        clientInput=new ClientChatInput(this);
        clientOutput=new ClientChatOutput(this);

        requestManager=new ClientChatRequest(this);
    }

    private void mainloop() throws IOException{
        PacketChat packet;

        while (true){
            try{
                packet=packetInterface.getPacketChat();
            }catch(PacketChatException e){
                break;
            }
            try{
                bucket.putPacketChat(packet);
            }catch(PacketChatException e){
                Logger.w("Cannot put packet in bucket: %s",e.getMessage());
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
