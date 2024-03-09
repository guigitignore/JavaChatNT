import java.io.IOException;
import java.net.Socket;


public class ServiceChat extends SocketWorker implements IServiceChat,IPacketChatInterface{
    private ServerChat server;
    private ServiceChatInput inputServiceChat=null;
    private ServiceChatOutput outputServiceChat=null;
    private IPacketChatInterface packetChatInterface=null;
    private ClientType clientType;

    private final static int LINE_FEED=10;

    public ServiceChat(Socket socket,ServerChat server) {
        super(socket,server);
    }

    public PacketChatOutput getInput(){
        return new PacketChatOutput(inputServiceChat);
    }

    public PacketChatOutput getOutput(){
        return new PacketChatOutput(outputServiceChat);
    }

    public User getUser(){
        return inputServiceChat==null?null:inputServiceChat.getUser();
    }

    public ServerChat getServer(){
        return server;
    }

    public ClientType getClientType(){
        return clientType;
    }

    private int readInputStreamByte() throws IOException{
        int result=getSocket().getInputStream().read();
        if (result==-1) throw new IOException("end of stream");
        return result;
    }

    private ClientType identifyClientType() throws IOException{
        ClientType clientType;

        getSocket().getOutputStream().write("Welcome to JavaChatNT. Press [enter] to continue...".getBytes());

        if (readInputStreamByte()==0xFF){
            clientType=ClientType.PACKETCHAT_CLIENT;
            //read the remaining bytes of hello packet to make sure alignment is correct.
            for (int i=0;i<7;i++) readInputStreamByte();
        }else{
            clientType=ClientType.TELNET_CLIENT;
            while (readInputStreamByte()!=LINE_FEED);
        }

        return clientType;
    }

    private void setupClientInterface() throws IOException{
        ClientType clientType=identifyClientType();
            
        switch (clientType) {
            case PACKETCHAT_CLIENT:
                packetChatInterface=new PacketChatRawInterface(getSocket());
                break;
            case TELNET_CLIENT:
                packetChatInterface=new PacketChatTelnetInterface(getSocket());
                break;
            default:
                throw new IOException("Unhandled client type");
        }
    }

    private void initStreams() throws IOException{
        inputServiceChat= new ServiceChatInput(this);
        outputServiceChat=new ServiceChatOutput(this);
    }

    private void mainloop(){
        PacketChat packet;

        while (true){
            try{
                packet=getPacketChat();
            }catch(PacketChatException e){
                break;
            }
            try{
                inputServiceChat.putPacketChat(packet);
            }catch(PacketChatException e){
                if (getUser()==null){
                    Logger.w("Packet dropped on input for %s: %s",getDescription(),e.getMessage());
                }else{
                    Logger.w("Packet dropped on input for user \"%s\" : %s",getUser().getName(),e.getMessage());
                }
            }
        }
    }

    public void run(){
        this.server=(ServerChat)getArgs()[0];
        
        try{
            setupClientInterface();
            initStreams();
            mainloop();
        }catch(IOException e){}

        ServerChatManager.getInstance().remove(this);
        server.remove(this);
        WorkerManager.getInstance().remove(this);
        if (outputServiceChat!=null) outputServiceChat.cancel();
        cancel();
    }

    public String getDescription() {
        return String.format("ServiceChat in %s", getSocket().getRemoteSocketAddress().toString());
    }

    public PacketChat getPacketChat() throws PacketChatException {
        return packetChatInterface.getPacketChat();
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        packetChatInterface.putPacketChat(packet);
    }

}
